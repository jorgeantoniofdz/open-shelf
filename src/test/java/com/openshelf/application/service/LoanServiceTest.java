package com.openshelf.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.BookRepository;
import com.openshelf.domain.book.Language;
import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanRepository;
import com.openshelf.domain.loan.LoanStatus;
import com.openshelf.domain.user.Member;
import com.openshelf.infrastructure.inmemory.InMemoryBookRepository;
import com.openshelf.infrastructure.inmemory.InMemoryLoanRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LoanServiceTest {

  private final LocalDate date = LocalDate.of(2020, 1, 1);
  private final Member member = new Member("m-1", "John Doe", "john@example.com");
  private final Book book =
      new Book.Builder()
          .title("Clean Code")
          .isbn("9780132350884")
          .publicationDate(LocalDate.of(2008, 8, 1))
          .pageCount(431)
          .language(Language.ENGLISH)
          .author(new Author("Oscar", "Wilde", LocalDate.of(1854, 10, 16), "Irish", ""))
          .build();
  private BookRepository books;
  private LoanRepository loans;
  private LoanService service;

  @BeforeEach
  void setUp() {
    books = new InMemoryBookRepository();
    loans = new InMemoryLoanRepository();
    service = new LoanService(loans, books);
    books.save(book);
  }

  private Loan saveLoan(String id, Member borrower, LoanStatus status) {
    Loan loan = new Loan(id, book, borrower, date);
    if (status == LoanStatus.OVERDUE) {
      loan.refreshOverdueStatus(loan.getDueDate().plusDays(1));
    } else if (status == LoanStatus.RETURNED) {
      loan.markReturned();
    }
    loans.save(loan);
    return loan;
  }

  private void fillToLimit(LoanStatus status) {
    for (int i = 0; i < Member.MAX_ACTIVE_LOANS; i++) {
      saveLoan("existing-" + i, member, status);
    }
  }

  @Test
  void checkout_savesActiveLoanWithUniqueIdAndFourteenDayDuration() {
    Loan first = service.checkout(member, book.getIsbn(), date);
    Loan second = service.checkout(member, book.getIsbn(), date);

    assertFalse(first.getId().isBlank());
    assertNotEquals(first.getId(), second.getId());
    assertSame(first, loans.findById(first.getId()).orElseThrow());
    assertSame(book, first.getBook());
    assertSame(member, first.getMember());
    assertEquals(date, first.getLoanDate());
    assertEquals(date.plusDays(Loan.DURATION_DAYS), first.getDueDate());
    assertEquals(LoanStatus.ACTIVE, first.getStatus());
    assertEquals(2, loans.findAll().size());
  }

  @ParameterizedTest
  @EnumSource(
      value = LoanStatus.class,
      names = {"ACTIVE", "OVERDUE"})
  void checkout_atLimit_rejectsWithoutSaving(LoanStatus status) {
    fillToLimit(status);
    Member sameId = new Member(member.getId(), "Updated name", "updated@example.com");

    assertThrows(IllegalStateException.class, () -> service.checkout(sameId, book.getIsbn(), date));
    assertEquals(Member.MAX_ACTIVE_LOANS, loans.findAll().size());
  }

  @Test
  void checkout_countsActiveAndOverdueLoansTogether() {
    fillToLimit(LoanStatus.ACTIVE);
    Loan overdue = loans.findById("existing-0").orElseThrow();
    overdue.refreshOverdueStatus(overdue.getDueDate().plusDays(1));
    loans.save(overdue);

    assertThrows(IllegalStateException.class, () -> service.checkout(member, book.getIsbn(), date));
    assertEquals(Member.MAX_ACTIVE_LOANS, loans.findAll().size());
  }

  @Test
  void checkout_belowLimit_succeedsAndIgnoresOtherMembersAndReturns() {
    for (int i = 0; i < Member.MAX_ACTIVE_LOANS - 1; i++) {
      saveLoan("active-" + i, member, LoanStatus.ACTIVE);
    }
    saveLoan("returned", member, LoanStatus.RETURNED);
    saveLoan("other", new Member("m-2", "Jane Doe", "jane@example.com"), LoanStatus.ACTIVE);

    Loan loan = service.checkout(member, book.getIsbn(), date);

    assertEquals(LoanStatus.ACTIVE, loan.getStatus());
    assertEquals(Member.MAX_ACTIVE_LOANS, loans.findActiveByMember(member).size());
  }

  @Test
  void checkout_unknownBook_doesNotSave() {
    assertThrows(IllegalArgumentException.class, () -> service.checkout(member, "missing", date));
    assertTrue(loans.findAll().isEmpty());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void operations_rejectInvalidKeys(String key) {
    assertThrows(IllegalArgumentException.class, () -> service.checkout(member, key, date));
    assertThrows(IllegalArgumentException.class, () -> service.returnBook(key));
    assertTrue(loans.findAll().isEmpty());
  }

  @Test
  void checkout_invalidMemberOrDate_doesNotSave() {
    assertThrows(
        IllegalArgumentException.class, () -> service.checkout(null, book.getIsbn(), date));
    assertThrows(
        IllegalArgumentException.class, () -> service.checkout(member, book.getIsbn(), null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.checkout(member, book.getIsbn(), LocalDate.now().plusDays(1)));
    assertTrue(loans.findAll().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(
      value = LoanStatus.class,
      names = {"ACTIVE", "OVERDUE"})
  void returnBook_returnsLoanAndFreesCapacity(LoanStatus status) {
    fillToLimit(status);
    service.returnBook("existing-0");

    assertEquals(LoanStatus.RETURNED, loans.findById("existing-0").orElseThrow().getStatus());
    Loan replacement = service.checkout(member, book.getIsbn(), date);
    assertEquals(LoanStatus.ACTIVE, replacement.getStatus());
    assertThrows(IllegalStateException.class, () -> service.returnBook("existing-0"));
  }

  @Test
  void returnBook_unknownLoan_preservesExistingLoan() {
    Loan existing = saveLoan("existing", member, LoanStatus.ACTIVE);

    assertThrows(IllegalArgumentException.class, () -> service.returnBook("missing"));
    assertEquals(LoanStatus.ACTIVE, existing.getStatus());
    assertEquals(1, loans.findAll().size());
  }

  @Test
  void constructor_rejectsNullRepositories() {
    assertThrows(IllegalArgumentException.class, () -> new LoanService(null, books));
    assertThrows(IllegalArgumentException.class, () -> new LoanService(loans, null));
  }
}
