package com.openshelf.domain.shared;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailValidatorTest {

  @Test
  void isValidEmail_null_returnsFalse() {
    assertFalse(EmailValidator.isValidEmail(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"user@example.com", "user@email.example.com"})
  void isValidEmail_validEmail_returnsTrue(String email) {
    assertTrue(EmailValidator.isValidEmail(email));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "userexample.com",
        "user@",
        "@example.com",
        "user@.com",
        "user@com",
        "user@exam_ple.com",
        "@",
        "",
        " "
      })
  void isValidEmail_invalidEmail_returnsFalse(String email) {
    assertFalse(EmailValidator.isValidEmail(email));
  }
}
