package com.openshelf.domain.fine;

import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.shared.Preconditions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Immutable snapshot of a loan's fine in USD at an explicit calculation date. */
public final class Fine {

  /** Standard daily fine in USD. */
  public static final BigDecimal DAILY_RATE = StandardFineStrategy.DAILY_RATE;

  private final String id;
  private final String loanId;
  private final LocalDate calculationDate;
  private final long daysOverdue;
  private final BigDecimal amount;

  /**
   * Calculates a fine, charging only days strictly after the due date.
   *
   * <p>The caller supplies the return date for a returned loan. This snapshot neither changes the
   * loan status nor accrues further charges when the loan changes.
   *
   * @param id unique identifier
   * @param loan associated loan
   * @param calculationDate date through which to calculate, no earlier than checkout
   */
  public Fine(String id, Loan loan, LocalDate calculationDate) {
    this(id, loan, calculationDate, new StandardFineStrategy());
  }

  /**
   * Calculates and snapshots a fine using the supplied pricing policy.
   *
   * @param id unique identifier
   * @param loan associated loan
   * @param calculationDate calculation date, or return date for a returned loan
   * @param strategy policy used once to calculate the amount in USD
   * @throws IllegalArgumentException if arguments or the strategy's result are invalid
   */
  public Fine(String id, Loan loan, LocalDate calculationDate, FineCalculationStrategy strategy) {
    this.id = Preconditions.requireNotBlank(id, "Fine ID cannot be null or blank");
    Preconditions.checkNotNull(loan, "Loan cannot be null");
    Preconditions.checkNotNull(calculationDate, "Calculation date cannot be null");
    Preconditions.checkNotNull(strategy, "Fine calculation strategy cannot be null");
    Preconditions.checkArgument(
        !calculationDate.isBefore(loan.getLoanDate()),
        "Calculation date cannot be before the loan date");
    this.loanId = loan.getId();
    this.calculationDate = calculationDate;
    this.daysOverdue = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), calculationDate));
    this.amount = validateAmount(strategy.calculate(daysOverdue));
  }

  private BigDecimal validateAmount(BigDecimal calculatedAmount) {
    Preconditions.checkNotNull(calculatedAmount, "Fine amount cannot be null");
    Preconditions.checkArgument(calculatedAmount.signum() >= 0, "Fine amount cannot be negative");
    Preconditions.checkArgument(
        calculatedAmount.stripTrailingZeros().scale() <= 2,
        "Fine amount must be expressed in whole cents");
    Preconditions.checkArgument(
        daysOverdue > 0 || calculatedAmount.signum() == 0,
        "Fine amount must be zero when there is no delay");
    return calculatedAmount.setScale(2);
  }

  public String getId() {
    return id;
  }

  public String getLoanId() {
    return loanId;
  }

  public LocalDate getCalculationDate() {
    return calculationDate;
  }

  public long getDaysOverdue() {
    return daysOverdue;
  }

  public BigDecimal getAmount() {
    return amount;
  }
}
