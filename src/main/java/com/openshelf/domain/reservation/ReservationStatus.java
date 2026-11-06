package com.openshelf.domain.reservation;

/** Lifecycle of a reservation; fulfilled and cancelled reservations are terminal. */
public enum ReservationStatus {
  PENDING,
  FULFILLED,
  CANCELLED;

  /**
   * Checks whether this state allows the requested transition.
   *
   * @param target requested next state
   * @return true only for a pending reservation being fulfilled or cancelled
   */
  public boolean canTransitionTo(ReservationStatus target) {
    return this == PENDING && (target == FULFILLED || target == CANCELLED);
  }
}
