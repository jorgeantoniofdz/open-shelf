package com.openshelf.domain.shared.isbn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IsbnValidatorTest {

  @ParameterizedTest
  @ValueSource(strings = {"9780132350884", "9780134685991", "9780596009205"})
  void isValidIsbn13_validIsbn_returnsTrue(String isbn) {
    assertTrue(IsbnValidator.isValidIsbn13(isbn));
  }

  @Test
  void isValidIsbn13_null_returnsFalse() {
    assertFalse(IsbnValidator.isValidIsbn13(null));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "", // empty
        "   ", // blank
        "978013235088", // 12 digits — too short
        "97801323508840", // 14 digits — too long
        "978-0132350884", // hyphens not accepted
        "9780132350883" // valid format, wrong checksum
      })
  void isValidIsbn13_invalidInput_returnsFalse(String isbn) {
    assertFalse(IsbnValidator.isValidIsbn13(isbn));
  }
}
