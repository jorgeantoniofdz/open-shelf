package com.openshelf.domain.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoanStatusTest {

  @ParameterizedTest(name = "{0} -> {1} should be {2}")
  @CsvSource({
    "ACTIVE, RETURNED, true",
    "ACTIVE, OVERDUE, true",
    "ACTIVE, ACTIVE, false",
    "RETURNED, ACTIVE, false",
    "RETURNED, OVERDUE, false",
    "RETURNED, RETURNED, false",
    "OVERDUE, ACTIVE, false",
    "OVERDUE, RETURNED, true",
    "OVERDUE, OVERDUE, false",
  })
  void canTransitionTo_allCombinations_returnsExpected(
      LoanStatus from, LoanStatus to, boolean expected) {
    assertEquals(expected, from.canTransitionTo(to));
  }
}
