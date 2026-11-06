package com.openshelf.domain.reservation;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for reservations keyed by ID.
 *
 * <p>Null arguments and blank keys are rejected with IllegalArgumentException. Save changed
 * reservations explicitly; query results are not deep copies of mutable entities.
 */
public interface ReservationRepository {

  /**
   * Saves a reservation, replacing any existing entry with the same ID.
   *
   * @param reservation reservation to store
   */
  void save(Reservation reservation);

  /**
   * Finds a reservation by its exact ID.
   *
   * @param id reservation ID
   * @return the stored reservation, or an empty optional
   */
  Optional<Reservation> findById(String id);

  /**
   * Finds pending reservations for an exact ISBN string.
   *
   * @param isbn requested book ISBN
   * @return an unmodifiable snapshot of matching entries, with no guaranteed order
   */
  List<Reservation> findPendingByIsbn(String isbn);
}
