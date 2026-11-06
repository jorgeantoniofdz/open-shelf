package com.openshelf.domain.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.Language;
import com.openshelf.domain.user.Member;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ReservationTest {

  private final LocalDate date = LocalDate.of(2020, 1, 1);
  private final Member member = new Member("m-1", "John Doe", "john@example.com");
  private final Book book =
      new Book.Builder()
          .title("Clean Code")
          .isbn("9780132350884")
          .publicationDate(LocalDate.of(2008, 8, 1))
          .pageCount(431)
          .language(Language.ENGLISH)
          .author(new Author("Oscar", "Wilde", LocalDate.of(1854, 10, 16), "Irish", ""))
          .build();

  @Test
  void constructor_createsPendingReservation() {
    Reservation reservation = new Reservation("r-1", book, member, date);

    assertEquals("r-1", reservation.getId());
    assertSame(book, reservation.getBook());
    assertSame(member, reservation.getMember());
    assertEquals(date, reservation.getReservationDate());
    assertEquals(ReservationStatus.PENDING, reservation.getStatus());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void constructor_rejectsInvalidId(String id) {
    assertThrows(IllegalArgumentException.class, () -> new Reservation(id, book, member, date));
  }

  @Test
  void constructor_rejectsMissingBookMemberOrDate() {
    assertThrows(IllegalArgumentException.class, () -> new Reservation("r-1", null, member, date));
    assertThrows(IllegalArgumentException.class, () -> new Reservation("r-1", book, null, date));
    assertThrows(IllegalArgumentException.class, () -> new Reservation("r-1", book, member, null));
  }

  @Test
  void constructor_acceptsTodayButRejectsFutureDate() {
    LocalDate today = LocalDate.now();
    Reservation reservation = new Reservation("r-1", book, member, today);

    assertEquals(today, reservation.getReservationDate());
    assertThrows(
        IllegalArgumentException.class,
        () -> new Reservation("r-1", book, member, today.plusDays(1)));
  }

  @Test
  void markFulfilled_fromPending_isTerminal() {
    Reservation reservation = new Reservation("r-1", book, member, date);
    reservation.markFulfilled();

    assertEquals(ReservationStatus.FULFILLED, reservation.getStatus());
    assertThrows(IllegalStateException.class, reservation::markFulfilled);
    assertThrows(IllegalStateException.class, reservation::cancel);
    assertEquals(ReservationStatus.FULFILLED, reservation.getStatus());
  }

  @Test
  void cancel_fromPending_isTerminal() {
    Reservation reservation = new Reservation("r-1", book, member, date);
    reservation.cancel();

    assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    assertThrows(IllegalStateException.class, reservation::cancel);
    assertThrows(IllegalStateException.class, reservation::markFulfilled);
    assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
  }
}
