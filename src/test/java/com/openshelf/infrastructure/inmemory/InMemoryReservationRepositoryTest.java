package com.openshelf.infrastructure.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openshelf.domain.reservation.Reservation;
import com.openshelf.domain.reservation.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InMemoryReservationRepositoryTest {

  private final ReservationRepository repository = new InMemoryReservationRepository();

  private Reservation reservation(String id) {
    return new Reservation(
        id, RepositoryFixtures.book(), RepositoryFixtures.member("m-1"), LocalDate.of(2020, 1, 1));
  }

  @Test
  void emptyRepository_returnsEmptyResults() {
    assertTrue(repository.findById("missing").isEmpty());
    assertTrue(repository.findPendingByIsbn("missing").isEmpty());
  }

  @Test
  void save_replacesByIdAndQueriesReflectStatusChanges() {
    Reservation first = reservation("r-1");
    repository.save(first);
    assertSame(first, repository.findById("r-1").orElseThrow());
    Reservation replacement = reservation("r-1");
    repository.save(replacement);
    assertSame(replacement, repository.findById("r-1").orElseThrow());
    assertEquals(List.of(replacement), repository.findPendingByIsbn(first.getBook().getIsbn()));
    replacement.cancel();
    repository.save(replacement);
    assertTrue(repository.findPendingByIsbn(first.getBook().getIsbn()).isEmpty());
  }

  @Test
  void pendingQuery_filtersByIsbnAndStatus() {
    Reservation pending = reservation("pending");
    Reservation cancelled = reservation("cancelled");
    Reservation fulfilled = reservation("fulfilled");
    cancelled.cancel();
    fulfilled.markFulfilled();
    repository.save(pending);
    repository.save(cancelled);
    repository.save(fulfilled);

    assertEquals(List.of(pending), repository.findPendingByIsbn(pending.getBook().getIsbn()));
    assertTrue(repository.findPendingByIsbn("9780201633610").isEmpty());
  }

  @Test
  void pendingQuery_returnsUnmodifiableSnapshot() {
    Reservation first = reservation("r-1");
    repository.save(first);
    List<Reservation> snapshot = repository.findPendingByIsbn(first.getBook().getIsbn());
    assertThrows(UnsupportedOperationException.class, snapshot::clear);
    repository.save(reservation("r-2"));

    assertEquals(List.of(first), snapshot);
    assertEquals(2, repository.findPendingByIsbn(first.getBook().getIsbn()).size());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void queries_rejectInvalidKeys(String key) {
    assertThrows(IllegalArgumentException.class, () -> repository.findById(key));
    assertThrows(IllegalArgumentException.class, () -> repository.findPendingByIsbn(key));
  }

  @Test
  void save_rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> repository.save(null));
  }
}
