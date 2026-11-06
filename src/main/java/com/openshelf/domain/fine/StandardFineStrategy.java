package com.openshelf.domain.fine;

import com.openshelf.domain.shared.Preconditions;
import java.math.BigDecimal;

/** Standard fine of one USD per overdue day. */
public final class StandardFineStrategy implements FineCalculationStrategy {

  /** Standard daily rate in USD. */
  public static final BigDecimal DAILY_RATE = new BigDecimal("1.00");

  @Override
  public BigDecimal calculate(long daysOverdue) {
    Preconditions.checkArgument(daysOverdue >= 0, "Overdue days cannot be negative");
    return DAILY_RATE.multiply(BigDecimal.valueOf(daysOverdue));
  }
}
