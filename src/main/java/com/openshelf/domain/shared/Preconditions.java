package com.openshelf.domain.shared;

/** Utility methods for validating method arguments in domain constructors. */
public final class Preconditions {

  /** Private constructor to prevent instantiation. */
  private Preconditions() {}

  /**
   * Throws {@link IllegalArgumentException} if {@code obj} is null.
   *
   * @param obj the object to check
   * @param message the exception message
   */
  public static void checkNotNull(Object obj, String message) {
    if (obj == null) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Throws {@link IllegalArgumentException} if {@code condition} is false.
   *
   * @param condition the condition that must be true
   * @param message the exception message
   */
  public static void checkArgument(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Returns {@code str} unchanged after asserting it is not null or blank.
   *
   * @param str the string to check
   * @param message the exception message
   * @return the original string, for inline assignment
   */
  public static String requireNotBlank(String str, String message) {
    checkNotNull(str, message);

    if (str.isBlank()) {
      throw new IllegalArgumentException(message);
    }

    return str;
  }

  /**
   * Returns {@code str} unchanged after asserting it contains no whitespace characters.
   *
   * @param str the string to check
   * @param message the exception message
   * @return the original string, for inline assignment
   */
  public static String requireNotWhitespace(String str, String message) {
    checkNotNull(str, message);

    if (str.matches(".*\\s+.*")) {
      throw new IllegalArgumentException(message);
    }

    return str;
  }
}
