package com.openshelf.application.service;

import com.openshelf.domain.fine.Fine;
import com.openshelf.domain.fine.FineCalculationStrategy;
import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.shared.Preconditions;
import java.time.LocalDate;
import java.util.UUID;

/** Creates fine snapshots with an injected pricing policy, without persisting or charging them. */
public final class FineService {

  private final FineCalculationStrategy strategy;

  /**
   * Creates a service using the selected policy.
   *
   * @param strategy fine calculation policy
   */
  public FineService(FineCalculationStrategy strategy) {
    Preconditions.checkNotNull(strategy, "Fine calculation strategy cannot be null");
    this.strategy = strategy;
  }

  /**
   * Creates a new fine with a generated ID and the configured pricing policy.
   *
   * @param loan associated loan
   * @param calculationDate calculation date, or return date if the loan has been returned
   * @return an immutable fine snapshot in USD
   * @throws IllegalArgumentException if arguments or the strategy's result are invalid
   */
  public Fine calculate(Loan loan, LocalDate calculationDate) {
    return new Fine(UUID.randomUUID().toString(), loan, calculationDate, strategy);
  }
}
