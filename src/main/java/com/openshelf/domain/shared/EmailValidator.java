package com.openshelf.domain.shared;

/**
 * Validates email address format.
 *
 * <p>Intended as a stepping stone toward a dedicated {@code Email} value object.
 */
public final class EmailValidator {

  private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

  /** Private constructor to prevent instantiation. */
  private EmailValidator() {}

  /**
   * Returns true if {@code email} matches a basic RFC-5322-compatible format.
   *
   * @param email the string to validate (null returns false)
   * @return true if the email format is valid
   */
  public static boolean isValidEmail(String email) {
    return email != null && email.matches(EMAIL_REGEX);
  }
}
