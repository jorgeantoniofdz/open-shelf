package com.openshelf.domain.shared.isbn;

/** Validates ISBN-13 identifiers, including checksum verification. */
public final class IsbnValidator {

  private static final int ISBN_13_LENGTH = 13;
  private static final int ISBN_13_PREFIX_LENGTH = 12;
  private static final int ISBN_13_ODD_WEIGHT = 3;
  private static final int CHECKSUM_MODULO = 10;

  /** Private constructor to prevent instantiation. */
  private IsbnValidator() {}

  /**
   * Returns true if {@code isbn} is a valid ISBN-13, including checksum verification.
   *
   * <p>The input must be exactly 13 digits with no hyphens or spaces.
   *
   * @param isbn the string to validate (null returns false)
   * @return true if the ISBN-13 format and checksum are valid
   */
  public static boolean isValidIsbn13(String isbn) {
    if (isbn == null || !isbn.matches("\\d{13}")) {
      return false;
    }

    int sum = 0;
    for (int i = 0; i < ISBN_13_PREFIX_LENGTH; i++) {
      int digit = Character.getNumericValue(isbn.charAt(i));
      sum += (i % 2 == 0) ? digit : digit * ISBN_13_ODD_WEIGHT;
    }

    int checksum = (CHECKSUM_MODULO - (sum % CHECKSUM_MODULO)) % CHECKSUM_MODULO;
    return checksum == Character.getNumericValue(isbn.charAt(ISBN_13_LENGTH - 1));
  }
}
