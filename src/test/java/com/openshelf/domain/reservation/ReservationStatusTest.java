package com.openshelf.domain.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ReservationStatusTest {

  @ParameterizedTest
  @CsvSource({
    "PENDING, PENDING, false",
    "PENDING, FULFILLED, true",
    "PENDING, CANCELLED, true",
    "FULFILLED, PENDING, false",
    "FULFILLED, FULFILLED, false",
    "FULFILLED, CANCELLED, false",
    "CANCELLED, PENDING, false",
    "CANCELLED, FULFILLED, false",
    "CANCELLED, CANCELLED, false"
  })
  void canTransitionTo_checksEveryPair(
      ReservationStatus source, ReservationStatus target, boolean allowed) {
    assertEquals(allowed, source.canTransitionTo(target));
  }

  @ParameterizedTest
  @EnumSource(ReservationStatus.class)
  void canTransitionTo_nullTarget_returnsFalse(ReservationStatus source) {
    assertFalse(source.canTransitionTo(null));
  }
}
