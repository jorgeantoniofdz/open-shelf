package com.openshelf.infrastructure.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openshelf.application.service.LoanService;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanEventListener;
import com.openshelf.domain.loan.LoanStatus;
import com.openshelf.domain.notification.Notification;
import com.openshelf.domain.notification.NotificationChannel;
import com.openshelf.domain.notification.NotificationFactory;
import com.openshelf.domain.notification.NotificationService;
import com.openshelf.domain.reservation.Reservation;
import com.openshelf.domain.reservation.ReservationNotifier;
import com.openshelf.domain.reservation.ReservationStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LoanReturnNotificationsTest {

  private final InMemoryBookRepository books = new InMemoryBookRepository();
  private final InMemoryLoanRepository loans = new InMemoryLoanRepository();
  private final InMemoryReservationRepository reservations = new InMemoryReservationRepository();
  private final List<String> recipients = new ArrayList<>();
  private final List<String> messages = new ArrayList<>();
  private final Book book = RepositoryFixtures.book();
  private final LocalDate date = LocalDate.of(2020, 1, 1);

  @ParameterizedTest
  @EnumSource(NotificationChannel.class)
  void returnBook_deliversThroughFactoryChannels(NotificationChannel channel) {
    List<Notification> recorded = new ArrayList<>();
    NotificationService notifications =
        switch (channel) {
          case EMAIL -> NotificationFactory.email(recorded::add);
          case SMS -> NotificationFactory.sms(recorded::add);
          case PUSH -> NotificationFactory.push(recorded::add);
        };
    Loan loan = loan();
    reserve("r-1", "m-1", book);
    ReservationNotifier observer = new ReservationNotifier(reservations, notifications);

    new LoanService(loans, books, List.of(observer)).returnBook(loan.getId());

    assertEquals(1, recorded.size());
    assertEquals(channel, recorded.getFirst().channel());
    assertEquals("m-1", recorded.getFirst().recipientId());
    assertTrue(recorded.getFirst().message().contains(book.getIsbn()));
    assertEquals(LoanStatus.RETURNED, loan.getStatus());
  }

  private Loan loan() {
    Loan loan = new Loan("loan-1", book, RepositoryFixtures.member("borrower"), date);
    loans.save(loan);
    return loan;
  }

  private Reservation reserve(String id, String memberId, Book requestedBook) {
    Reservation reservation =
        new Reservation(id, requestedBook, RepositoryFixtures.member(memberId), date);
    reservations.save(reservation);
    return reservation;
  }

  private ReservationNotifier notifier() {
    return new ReservationNotifier(
        reservations,
        (user, message) -> {
          recipients.add(user.getId());
          messages.add(message);
        });
  }

  @ParameterizedTest
  @EnumSource(
      value = LoanStatus.class,
      names = {"ACTIVE", "OVERDUE"})
  void returnBook_notifiesOnlyPendingMembersOncePerId(LoanStatus status) {
    Loan loan = loan();
    if (status == LoanStatus.OVERDUE) {
      loan.refreshOverdueStatus(loan.getDueDate().plusDays(1));
    }
    Reservation pending = reserve("r-1", "m-1", RepositoryFixtures.book());
    reserve("duplicate", "m-1", book);
    reserve("r-2", "m-2", book);
    reserve("cancelled", "m-3", book).cancel();
    reserve("fulfilled", "m-4", book).markFulfilled();
    Book other = RepositoryFixtures.book("9780201633610", "Other book", book.getAuthor());
    reserve("other", "m-5", other);
    LoanService service = new LoanService(loans, books, List.of(notifier()));

    service.returnBook(loan.getId());

    assertEquals(Set.of("m-1", "m-2"), Set.copyOf(recipients));
    assertEquals(2, recipients.size());
    assertTrue(
        messages.stream()
            .allMatch(
                message -> message.contains(book.getTitle()) && message.contains(book.getIsbn())));
    assertEquals(ReservationStatus.PENDING, pending.getStatus());
    assertThrows(IllegalStateException.class, () -> service.returnBook(loan.getId()));
    assertEquals(2, recipients.size());
  }

  @Test
  void returnBook_withoutPendingReservations_sendsNothing() {
    Loan loan = loan();
    new LoanService(loans, books, List.of(notifier())).returnBook(loan.getId());
    assertTrue(recipients.isEmpty());
    assertEquals(LoanStatus.RETURNED, loan.getStatus());
  }

  @Test
  void returnBook_missingLoan_sendsNothing() {
    reserve("r-1", "m-1", book);
    LoanService service = new LoanService(loans, books, List.of(notifier()));
    assertThrows(IllegalArgumentException.class, () -> service.returnBook("missing"));
    assertTrue(recipients.isEmpty());
  }

  @Test
  void observers_runInOrderAfterReturnAndListIsCopied() {
    Loan loan = loan();
    List<String> events = new ArrayList<>();
    List<LoanEventListener> listeners = new ArrayList<>();
    listeners.add(
        returned -> {
          assertEquals(
              LoanStatus.RETURNED, loans.findById(returned.getId()).orElseThrow().getStatus());
          events.add("first");
        });
    listeners.add(returned -> events.add("second"));
    LoanService service = new LoanService(loans, books, listeners);
    listeners.clear();
    service.returnBook(loan.getId());
    assertEquals(List.of("first", "second"), events);
  }

  @Test
  void notificationFailure_propagatesWithoutUndoingReturnOrRetrying() {
    Loan loan = loan();
    reserve("r-1", "m-1", book);
    IllegalStateException failure = new IllegalStateException("Delivery unavailable");
    ReservationNotifier failing =
        new ReservationNotifier(
            reservations,
            (user, message) -> {
              throw failure;
            });
    LoanService service = new LoanService(loans, books, List.of(failing, notifier()));

    assertSame(
        failure, assertThrows(IllegalStateException.class, () -> service.returnBook(loan.getId())));
    assertEquals(LoanStatus.RETURNED, loans.findById(loan.getId()).orElseThrow().getStatus());
    assertTrue(recipients.isEmpty());
    assertThrows(IllegalStateException.class, () -> service.returnBook(loan.getId()));
  }

  @Test
  void constructors_rejectMissingDependenciesAndNullListeners() {
    assertThrows(
        IllegalArgumentException.class, () -> new ReservationNotifier(null, (user, message) -> {}));
    assertThrows(IllegalArgumentException.class, () -> new ReservationNotifier(reservations, null));
    assertThrows(IllegalArgumentException.class, () -> new LoanService(loans, books, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LoanService(loans, books, Arrays.asList(notifier(), null)));
  }

  @Test
  void notifier_rejectsNullAndUnreturnedLoans() {
    ReservationNotifier notifier = notifier();
    Loan loan = loan();
    assertThrows(IllegalArgumentException.class, () -> notifier.onBookReturned(null));
    assertThrows(IllegalArgumentException.class, () -> notifier.onBookReturned(loan));
    loan.refreshOverdueStatus(loan.getDueDate().plusDays(1));
    assertThrows(IllegalArgumentException.class, () -> notifier.onBookReturned(loan));
    assertTrue(recipients.isEmpty());
  }
}
