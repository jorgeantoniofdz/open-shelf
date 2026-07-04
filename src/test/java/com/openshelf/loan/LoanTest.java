package com.openshelf.loan;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openshelf.book.Author;
import com.openshelf.book.Book;
import com.openshelf.book.Language;
import com.openshelf.user.Member;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.TooManyMethods")
class LoanTest {

  private static final String LOAN_ID = "loan-1";

  private Author validAuthor() {
    return new Author("Oscar", "Wilde", LocalDate.of(1854, 10, 16), "Irish", "");
  }

  private Member validMember() {
    return new Member("m-1", "John Doe", "john@example.com");
  }

  private Book validBook() {
    return new Book.Builder()
        .title("Clean Code")
        .isbn("9780132350884")
        .publicationDate(LocalDate.of(2008, 8, 1))
        .pageCount(431)
        .language(Language.ENGLISH)
        .author(validAuthor())
        .build();
  }

  private Loan validLoan() {
    return new Loan(LOAN_ID, validBook(), validMember(), LocalDate.now());
  }

  // -------------------- constructor --------------------

  @Test
  void constructor_withValidParameters_statusIsActiveAndDueDateIs14DaysLater() {
    LocalDate loanDate = LocalDate.now();
    Loan loan = new Loan(LOAN_ID, validBook(), validMember(), loanDate);
    assertEquals(LoanStatus.ACTIVE, loan.getStatus());
    assertEquals(loanDate.plusDays(Loan.DURATION_DAYS), loan.getDueDate());
  }

  @Test
  void constructor_idIsNull_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Loan(null, validBook(), validMember(), LocalDate.now()));
  }

  @Test
  void constructor_idIsBlank_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Loan("   ", validBook(), validMember(), LocalDate.now()));
  }

  @Test
  void constructor_bookIsNull_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Loan(LOAN_ID, null, validMember(), LocalDate.now()));
  }

  @Test
  void constructor_memberIsNull_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Loan(LOAN_ID, validBook(), null, LocalDate.now()));
  }

  @Test
  void constructor_loanDateIsNull_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> new Loan(LOAN_ID, validBook(), validMember(), null));
  }

  @Test
  void constructor_loanDateInFuture_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Loan(LOAN_ID, validBook(), validMember(), LocalDate.now().plusDays(1)));
  }

  @Test
  void constructor_loanDateIsToday_doesNotThrow() {
    assertDoesNotThrow(() -> new Loan(LOAN_ID, validBook(), validMember(), LocalDate.now()));
  }

  // -------------------- markReturned --------------------

  @Test
  void markReturned_fromActive_statusIsReturned() {
    Loan loan = validLoan();
    loan.markReturned();
    assertEquals(LoanStatus.RETURNED, loan.getStatus());
  }

  @Test
  void markReturned_fromReturned_throwsIllegalStateException() {
    Loan loan = validLoan();
    loan.markReturned();
    assertThrows(IllegalStateException.class, loan::markReturned);
  }

  @Test
  void markReturned_fromOverdue_throwsIllegalStateException() {
    Loan loan = validLoan();
    loan.refreshOverdueStatus(loan.getDueDate().plusDays(1));
    assertThrows(IllegalStateException.class, loan::markReturned);
  }

  // -------------------- refreshOverdueStatus --------------------

  @Test
  void refreshOverdueStatus_activePastDueDate_statusIsOverdue() {
    Loan loan = validLoan();
    loan.refreshOverdueStatus(loan.getDueDate().plusDays(1));
    assertEquals(LoanStatus.OVERDUE, loan.getStatus());
  }

  @Test
  void refreshOverdueStatus_activeBeforeDueDate_statusRemainsActive() {
    Loan loan = validLoan();
    loan.refreshOverdueStatus(loan.getDueDate().minusDays(1));
    assertEquals(LoanStatus.ACTIVE, loan.getStatus());
  }

  @Test
  void refreshOverdueStatus_alreadyReturned_statusRemainsReturned() {
    Loan loan = validLoan();
    loan.markReturned();
    loan.refreshOverdueStatus(loan.getDueDate().plusDays(1));
    assertEquals(LoanStatus.RETURNED, loan.getStatus());
  }
}
