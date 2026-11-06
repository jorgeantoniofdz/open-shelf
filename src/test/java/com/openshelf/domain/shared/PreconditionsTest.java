package com.openshelf.domain.shared;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PreconditionsTest {

  @Test
  void checkNotNull_noNull_doesNotThrow() {
    assertDoesNotThrow(() -> Preconditions.checkNotNull("not null", "Should not be null"));
  }

  @Test
  void checkNotNull_null_throwsIllegalArgumentExceptionWithMessage() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.checkNotNull(null, "Should not be null"));

    assertEquals("Should not be null", exception.getMessage());
  }

  @Test
  void checkArgument_true_doesNotThrow() {
    assertDoesNotThrow(() -> Preconditions.checkArgument(true, "Condition should be true"));
  }

  @Test
  void checkArgument_false_throwsIllegalArgumentExceptionWithMessage() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.checkArgument(false, "Condition should be true"));

    assertEquals("Condition should be true", exception.getMessage());
  }

  @Test
  void requireNotBlank_stringWithoutWhitespace_returnsSameString() {
    String result = Preconditions.requireNotBlank("abc", "String cannot be null or blank");

    assertEquals("abc", result);
  }

  @Test
  void requireNotBlank_null_throwsIllegalArgumentExceptionWithMessage() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.requireNotBlank(null, "String cannot be null or blank"));

    assertEquals("String cannot be null or blank", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
  void requireNotBlank_blankString_throwsIllegalArgumentExceptionWithMessage(String blank) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.requireNotBlank(blank, "String cannot be null or blank"));

    assertEquals("String cannot be null or blank", exception.getMessage());
  }

  @Test
  void requireNotWhitespace_stringWithoutWhitespace_returnsSameString() {
    String result = Preconditions.requireNotWhitespace("abc", "String cannot contain whitespace");
    assertEquals("abc", result);
  }

  @Test
  void requireNotWhitespace_null_throwsIllegalArgumentExceptionWithMessage() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.requireNotWhitespace(null, "String cannot contain whitespace"));

    assertEquals("String cannot contain whitespace", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "a b", "abc ", "abc\tdef", "abc\ndef"})
  void requireNotWhitespace_stringWithWhitespace_throwsIllegalArgumentExceptionWithMessage(
      String str) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.requireNotWhitespace(str, "String cannot contain whitespace"));

    assertEquals("String cannot contain whitespace", exception.getMessage());
  }
}
