package com.openshelf.domain.fine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FineCalculationStrategyTest {

  static Stream<Arguments> policies() {
    return Stream.of(
        Arguments.of(new StandardFineStrategy(), new BigDecimal("1.00")),
        Arguments.of(new PremiumFineStrategy(), new BigDecimal("0.50")),
        Arguments.of(new StudentFineStrategy(), new BigDecimal("0.25")));
  }

  @ParameterizedTest
  @MethodSource("policies")
  void calculate_chargesExactDailyRate(FineCalculationStrategy strategy, BigDecimal rate) {
    assertEquals(new BigDecimal("0.00"), strategy.calculate(0));
    assertEquals(rate, strategy.calculate(1));
    assertEquals(rate.multiply(BigDecimal.valueOf(5)), strategy.calculate(5));
    assertEquals(rate.multiply(BigDecimal.valueOf(366)), strategy.calculate(366));
    assertEquals(
        rate.multiply(BigDecimal.valueOf(Long.MAX_VALUE)), strategy.calculate(Long.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("policies")
  void calculate_rejectsNegativeDays(FineCalculationStrategy strategy, BigDecimal rate) {
    assertThrows(IllegalArgumentException.class, () -> strategy.calculate(-1));
    assertThrows(IllegalArgumentException.class, () -> strategy.calculate(Long.MIN_VALUE));
  }
}
