package com.openshelf.domain.reservation;

import com.openshelf.domain.book.Book;
import com.openshelf.domain.shared.Preconditions;
import com.openshelf.domain.user.Member;
import java.time.LocalDate;

/** A member's request for a book. Availability must be checked before creating it. */
public final class Reservation {

  private final String id;
  private final Book book;
  private final Member member;
  private final LocalDate reservationDate;
  private ReservationStatus status;

  /**
   * Creates a pending reservation.
   *
   * @param id unique identifier
   * @param book requested book
   * @param member requesting member
   * @param reservationDate date of the request, no later than today
   */
  public Reservation(String id, Book book, Member member, LocalDate reservationDate) {
    this.id = Preconditions.requireNotBlank(id, "Reservation ID cannot be null or blank");
    Preconditions.checkNotNull(book, "Book cannot be null");
    Preconditions.checkNotNull(member, "Member cannot be null");
    Preconditions.checkNotNull(reservationDate, "Reservation date cannot be null");
    Preconditions.checkArgument(
        !reservationDate.isAfter(LocalDate.now()), "Reservation date cannot be in the future");
    this.book = book;
    this.member = member;
    this.reservationDate = reservationDate;
    this.status = ReservationStatus.PENDING;
  }

  public String getId() {
    return id;
  }

  public Book getBook() {
    return book;
  }

  public Member getMember() {
    return member;
  }

  public LocalDate getReservationDate() {
    return reservationDate;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  /**
   * Fulfills this reservation.
   *
   * @throws IllegalStateException if it is no longer pending
   */
  public void markFulfilled() {
    transitionTo(ReservationStatus.FULFILLED);
  }

  /**
   * Cancels this reservation.
   *
   * @throws IllegalStateException if it is no longer pending
   */
  public void cancel() {
    transitionTo(ReservationStatus.CANCELLED);
  }

  private void transitionTo(ReservationStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new IllegalStateException(
          "Cannot transition reservation from " + status + " to " + target);
    }
    status = target;
  }
}
