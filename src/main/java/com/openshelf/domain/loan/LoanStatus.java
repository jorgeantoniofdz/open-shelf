package com.openshelf.domain.loan;

/**
 * Represents the lifecycle state of a book loan.
 *
 * <p>This enum is the single source of truth for all valid loan states and their allowed
 * transitions. Business logic that depends on state lives here — not scattered across services.
 *
 * <p>Valid transitions:
 *
 * <ul>
 *   <li>ACTIVE → RETURNED (member returns the book on time)
 *   <li>ACTIVE → OVERDUE (due date has passed)
 *   <li>OVERDUE → RETURNED (member returns the book late)
 * </ul>
 */
public enum LoanStatus {
  ACTIVE,
  RETURNED,
  OVERDUE;

  /**
   * Returns true if this status can legally transition to the given target status.
   *
   * @param target the desired next status
   * @return true if the transition is allowed by business rules
   */
  public boolean canTransitionTo(LoanStatus target) {
    return switch (this) {
      case ACTIVE -> target == RETURNED || target == OVERDUE;
      case OVERDUE -> target == RETURNED;
      case RETURNED -> false;
    };
  }
}
