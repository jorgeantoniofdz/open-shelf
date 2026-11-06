package com.openshelf.domain.fine;

import com.openshelf.domain.shared.Preconditions;
import java.math.BigDecimal;

/** Premium policy using the practice project's daily rate of 0.50 USD. */
public final class PremiumFineStrategy implements FineCalculationStrategy {

  /** Daily rate in USD chosen for this learning project. */
  public static final BigDecimal DAILY_RATE = new BigDecimal("0.50");

  @Override
  public BigDecimal calculate(long daysOverdue) {
    Preconditions.checkArgument(daysOverdue >= 0, "Overdue days cannot be negative");
    return DAILY_RATE.multiply(BigDecimal.valueOf(daysOverdue));
  }
}
