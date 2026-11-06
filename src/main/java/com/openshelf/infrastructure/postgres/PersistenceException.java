package com.openshelf.infrastructure.postgres;

/** Unchecked storage failure retaining the original JDBC cause for diagnosis. */
public final class PersistenceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a storage failure without exposing SQL details through the repository interface.
   *
   * @param message failed operation
   * @param cause original storage error
   */
  public PersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
