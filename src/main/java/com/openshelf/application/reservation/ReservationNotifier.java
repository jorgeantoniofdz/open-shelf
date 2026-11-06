package com.openshelf.application.reservation;

import com.openshelf.application.loan.LoanEventListener;
import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanStatus;
import com.openshelf.domain.notification.NotificationService;
import com.openshelf.domain.reservation.Reservation;
import com.openshelf.domain.reservation.ReservationRepository;
import com.openshelf.domain.shared.Preconditions;
import java.util.HashSet;
import java.util.Set;

/** Notifies each waiting member once per return without fulfilling their reservations. */
public final class ReservationNotifier implements LoanEventListener {

  private final ReservationRepository reservations;
  private final NotificationService notifications;

  /**
   * Creates an observer with reservation storage and a delivery channel.
   *
   * @param reservations reservation repository
   * @param notifications message delivery channel
   */
  public ReservationNotifier(
      ReservationRepository reservations, NotificationService notifications) {
    Preconditions.checkNotNull(reservations, "Reservation repository cannot be null");
    Preconditions.checkNotNull(notifications, "Notification service cannot be null");
    this.reservations = reservations;
    this.notifications = notifications;
  }

  @Override
  public void onBookReturned(Loan loan) {
    Preconditions.checkNotNull(loan, "Loan cannot be null");
    Preconditions.checkArgument(
        loan.getStatus() == LoanStatus.RETURNED, "Loan must be returned before notifying");
    Set<String> notifiedMembers = new HashSet<>();
    String message =
        "Book returned: " + loan.getBook().getTitle() + " (ISBN " + loan.getBook().getIsbn() + ").";
    for (Reservation reservation : reservations.findPendingByIsbn(loan.getBook().getIsbn())) {
      if (notifiedMembers.add(reservation.getMember().getId())) {
        notifications.notify(reservation.getMember(), message);
      }
    }
  }
}
