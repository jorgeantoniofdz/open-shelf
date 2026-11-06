package com.openshelf.domain.fine;

import com.openshelf.domain.shared.Preconditions;
import java.math.BigDecimal;

/** Student policy using the practice project's daily rate of 0.25 USD. */
public final class StudentFineStrategy implements FineCalculationStrategy {

  /** Daily rate in USD chosen for this learning project. */
  public static final BigDecimal DAILY_RATE = new BigDecimal("0.25");

  @Override
  public BigDecimal calculate(long daysOverdue) {
    Preconditions.checkArgument(daysOverdue >= 0, "Overdue days cannot be negative");
    return DAILY_RATE.multiply(BigDecimal.valueOf(daysOverdue));
  }
}
