package com.openshelf.infrastructure.inmemory;

import com.openshelf.domain.reservation.Reservation;
import com.openshelf.domain.reservation.ReservationRepository;
import com.openshelf.domain.reservation.ReservationStatus;
import com.openshelf.domain.shared.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores reservation references in memory for single-threaded use. */
public final class InMemoryReservationRepository implements ReservationRepository {

  private final Map<String, Reservation> store = new HashMap<>();

  @Override
  public void save(Reservation reservation) {
    Preconditions.checkNotNull(reservation, "Reservation cannot be null");
    store.put(reservation.getId(), reservation);
  }

  @Override
  public Optional<Reservation> findById(String id) {
    Preconditions.requireNotBlank(id, "Reservation ID cannot be null or blank");
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<Reservation> findPendingByIsbn(String isbn) {
    Preconditions.requireNotBlank(isbn, "ISBN cannot be null or blank");
    return store.values().stream()
        .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
        .filter(reservation -> reservation.getBook().getIsbn().equals(isbn))
        .toList();
  }
}
