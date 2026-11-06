package com.openshelf.application.loan;

import com.openshelf.domain.loan.Loan;

/** Observer of successfully saved loan returns. */
@FunctionalInterface
public interface LoanEventListener {

  /**
   * Handles a returned loan without changing its state.
   *
   * @param loan the loan saved with status RETURNED
   */
  void onBookReturned(Loan loan);
}
