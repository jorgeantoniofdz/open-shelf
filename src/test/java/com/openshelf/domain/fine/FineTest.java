package com.openshelf.domain.fine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.Language;
import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanStatus;
import com.openshelf.domain.user.Member;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FineTest {

  private Loan validLoan() {
    Book book =
        new Book.Builder()
            .title("Clean Code")
            .isbn("9780132350884")
            .publicationDate(LocalDate.of(2008, 8, 1))
            .pageCount(431)
            .language(Language.ENGLISH)
            .author(new Author("Oscar", "Wilde", LocalDate.of(1854, 10, 16), "Irish", ""))
            .build();
    return new Loan(
        "loan-1",
        book,
        new Member("m-1", "John Doe", "john@example.com"),
        LocalDate.of(2020, 2, 14));
  }

  @ParameterizedTest
  @CsvSource({
    "-14, 0, 0.00",
    "-1, 0, 0.00",
    "0, 0, 0.00",
    "1, 1, 1.00",
    "5, 5, 5.00",
    "366, 366, 366.00"
  })
  void constructor_chargesOnlyDaysAfterDueDate(
      long offset, long expectedDays, String expectedAmount) {
    Loan loan = validLoan();
    LocalDate calculationDate = loan.getDueDate().plusDays(offset);
    Fine fine = new Fine("f-1", loan, calculationDate);

    assertEquals("f-1", fine.getId());
    assertEquals(loan.getId(), fine.getLoanId());
    assertEquals(calculationDate, fine.getCalculationDate());
    assertEquals(expectedDays, fine.getDaysOverdue());
    assertEquals(new BigDecimal(expectedAmount), fine.getAmount());
    assertEquals(LoanStatus.ACTIVE, loan.getStatus());
  }

  @Test
  void constructor_countsLeapDayAcrossMonthBoundary() {
    Fine fine = new Fine("f-1", validLoan(), LocalDate.of(2020, 3, 1));

    assertEquals(2, fine.getDaysOverdue());
    assertEquals(new BigDecimal("2.00"), fine.getAmount());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void constructor_rejectsInvalidId(String id) {
    Loan loan = validLoan();
    assertThrows(IllegalArgumentException.class, () -> new Fine(id, loan, loan.getDueDate()));
  }

  @Test
  void constructor_rejectsMissingLoanOrDate() {
    Loan loan = validLoan();
    assertThrows(IllegalArgumentException.class, () -> new Fine("f-1", null, loan.getDueDate()));
    assertThrows(IllegalArgumentException.class, () -> new Fine("f-1", loan, null));
  }

  @Test
  void constructor_rejectsDateBeforeCheckout() {
    Loan loan = validLoan();
    assertThrows(
        IllegalArgumentException.class,
        () -> new Fine("f-1", loan, loan.getLoanDate().minusDays(1)));
  }

  @Test
  void amount_isSnapshotIndependentOfLoanStatus() {
    Loan loan = validLoan();
    LocalDate calculationDate = loan.getDueDate().plusDays(5);
    Fine fine = new Fine("f-1", loan, calculationDate);
    loan.markReturned();

    assertEquals(new BigDecimal("5.00"), fine.getAmount());
    assertEquals(5, fine.getDaysOverdue());
    assertEquals(fine.getAmount(), new Fine("f-2", loan, calculationDate).getAmount());
    assertEquals(LoanStatus.RETURNED, loan.getStatus());
  }
}
