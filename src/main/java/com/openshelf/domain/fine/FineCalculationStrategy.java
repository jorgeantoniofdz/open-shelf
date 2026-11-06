package com.openshelf.domain.fine;

import java.math.BigDecimal;

/** Calculates a fine in USD independently of the loan and its dates. */
@FunctionalInterface
public interface FineCalculationStrategy {

  /**
   * Calculates the charge for a nonnegative number of overdue days.
   *
   * @param daysOverdue days strictly after the due date
   * @return a nonnull, nonnegative amount in whole cents, zero when there is no delay
   * @throws IllegalArgumentException if the number of days is negative
   */
  BigDecimal calculate(long daysOverdue);
}
