package com.openshelf.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.Language;
import com.openshelf.domain.fine.Fine;
import com.openshelf.domain.fine.FineCalculationStrategy;
import com.openshelf.domain.fine.PremiumFineStrategy;
import com.openshelf.domain.fine.StandardFineStrategy;
import com.openshelf.domain.fine.StudentFineStrategy;
import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanStatus;
import com.openshelf.domain.user.Member;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class FineServiceTest {

  private Loan loan() {
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
        LocalDate.of(2020, 1, 1));
  }

  static Stream<Arguments> policies() {
    return Stream.of(
        Arguments.of(new StandardFineStrategy(), "5.00"),
        Arguments.of(new PremiumFineStrategy(), "2.50"),
        Arguments.of(new StudentFineStrategy(), "1.25"));
  }

  @ParameterizedTest
  @MethodSource("policies")
  void calculate_usesInjectedPolicy(FineCalculationStrategy strategy, String expected) {
    Loan loan = loan();
    FineService service = new FineService(strategy);
    LocalDate date = loan.getDueDate().plusDays(5);
    Fine fine = service.calculate(loan, date);

    assertEquals(new BigDecimal(expected), fine.getAmount());
    assertEquals(5, fine.getDaysOverdue());
    assertEquals(loan.getId(), fine.getLoanId());
    assertEquals(date, fine.getCalculationDate());
    assertFalse(fine.getId().isBlank());
    assertNotEquals(fine.getId(), service.calculate(loan, date).getId());
    assertEquals(LoanStatus.ACTIVE, loan.getStatus());
    assertEquals(new BigDecimal("0.00"), service.calculate(loan, loan.getDueDate()).getAmount());
    assertEquals(new BigDecimal("0.00"), service.calculate(loan, loan.getLoanDate()).getAmount());
  }

  @Test
  void calculate_supportsNewPolicyAndSnapshotsItsResult() {
    AtomicInteger calls = new AtomicInteger();
    FineService service =
        new FineService(
            days -> {
              calls.incrementAndGet();
              return BigDecimal.valueOf(days).multiply(new BigDecimal("0.10"));
            });
    Loan loan = loan();
    Fine fine = service.calculate(loan, loan.getDueDate().plusDays(5));
    loan.markReturned();

    assertEquals(new BigDecimal("0.50"), fine.getAmount());
    assertEquals(new BigDecimal("0.50"), fine.getAmount());
    assertEquals(1, calls.get());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"-1.00", "0.001"})
  void calculate_rejectsInvalidPolicyResult(String amount) {
    FineService service = new FineService(days -> amount == null ? null : new BigDecimal(amount));
    Loan loan = loan();
    assertThrows(
        IllegalArgumentException.class,
        () -> service.calculate(loan, loan.getDueDate().plusDays(1)));
  }

  @Test
  void calculate_rejectsChargeWithoutDelay() {
    FineService service = new FineService(days -> BigDecimal.ONE);
    Loan loan = loan();
    assertThrows(IllegalArgumentException.class, () -> service.calculate(loan, loan.getDueDate()));
  }

  @Test
  void calculate_normalizesExactCentsWithoutRounding() {
    FineService service = new FineService(days -> new BigDecimal("1.2500"));
    Loan loan = loan();
    assertEquals(
        new BigDecimal("1.25"), service.calculate(loan, loan.getDueDate().plusDays(1)).getAmount());
  }

  @Test
  void calculate_rejectsMissingInputsAndDateBeforeCheckout() {
    Loan loan = loan();
    FineService service = new FineService(new StandardFineStrategy());
    assertThrows(IllegalArgumentException.class, () -> new FineService(null));
    assertThrows(IllegalArgumentException.class, () -> service.calculate(null, loan.getDueDate()));
    assertThrows(IllegalArgumentException.class, () -> service.calculate(loan, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.calculate(loan, loan.getLoanDate().minusDays(1)));
    assertThrows(
        IllegalArgumentException.class, () -> new Fine("f-1", loan, loan.getDueDate(), null));
  }
}
