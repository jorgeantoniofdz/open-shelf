package com.openshelf.book;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AuthorTest {

  private Author validAuthor() {
    return new Author(
        "Oscar",
        "Wilde",
        LocalDate.of(1854, 10, 16),
        "Irish",
        "Oscar Wilde was an Irish poet and playwright. After writing in different forms"
            + " throughout the 1880s, he became one of London's most popular playwrights in the"
            + " early 1890s.");
  }

  // -------------------- happy path --------------------

  @Test
  void constructor_withValidParameters_doesNotThrow() {
    assertDoesNotThrow(this::validAuthor);
  }

  // -------------------- firstName --------------------

  @Test
  void constructor_firstNameIsNull_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                null, a.getLastName(), a.getBirthDate(), a.getNationality(), a.getBiography()));
  }

  @Test
  void constructor_firstNameIsBlank_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                "   ", a.getLastName(), a.getBirthDate(), a.getNationality(), a.getBiography()));
  }

  @Test
  void constructor_firstNameIsExactly50Characters_doesNotThrow() {
    Author a = validAuthor();
    assertDoesNotThrow(
        () ->
            new Author(
                "A".repeat(50),
                a.getLastName(),
                a.getBirthDate(),
                a.getNationality(),
                a.getBiography()));
  }

  @Test
  void constructor_firstNameIsMoreThan50Characters_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                "A".repeat(51),
                a.getLastName(),
                a.getBirthDate(),
                a.getNationality(),
                a.getBiography()));
  }

  // -------------------- lastName --------------------

  @Test
  void constructor_lastNameIsNull_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(), null, a.getBirthDate(), a.getNationality(), a.getBiography()));
  }

  @Test
  void constructor_lastNameIsBlank_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(), "   ", a.getBirthDate(), a.getNationality(), a.getBiography()));
  }

  @Test
  void constructor_lastNameIsExactly50Characters_doesNotThrow() {
    Author a = validAuthor();
    assertDoesNotThrow(
        () ->
            new Author(
                a.getFirstName(),
                "A".repeat(50),
                a.getBirthDate(),
                a.getNationality(),
                a.getBiography()));
  }

  @Test
  void constructor_lastNameIsMoreThan50Characters_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(),
                "A".repeat(51),
                a.getBirthDate(),
                a.getNationality(),
                a.getBiography()));
  }

  // -------------------- birthDate --------------------

  @Test
  void constructor_birthDateIsNull_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(), a.getLastName(), null, a.getNationality(), a.getBiography()));
  }

  @Test
  void constructor_birthDateInTheFuture_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(),
                a.getLastName(),
                LocalDate.now().plusDays(1),
                a.getNationality(),
                a.getBiography()));
  }

  @Test
  void constructor_birthDateInPast_doesNotThrow() {
    Author a = validAuthor();
    assertDoesNotThrow(
        () ->
            new Author(
                a.getFirstName(),
                a.getLastName(),
                LocalDate.now().minusDays(1),
                a.getNationality(),
                a.getBiography()));
  }

  @Test
  void constructor_birthDateToday_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(),
                a.getLastName(),
                LocalDate.now(),
                a.getNationality(),
                a.getBiography()));
  }

  // -------------------- nationality --------------------

  @Test
  void constructor_nationalityIsNull_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(), a.getLastName(), a.getBirthDate(), null, a.getBiography()));
  }

  @Test
  void constructor_nationalityIsBlank_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(), a.getLastName(), a.getBirthDate(), "   ", a.getBiography()));
  }

  // -------------------- biography --------------------

  @Test
  void constructor_biographyIsNull_throwsIllegalArgumentException() {
    Author a = validAuthor();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Author(
                a.getFirstName(), a.getLastName(), a.getBirthDate(), a.getNationality(), null));
  }

  @Test
  void constructor_biographyIsBlank_doesNotThrow() {
    Author a = validAuthor();
    assertDoesNotThrow(
        () ->
            new Author(
                a.getFirstName(), a.getLastName(), a.getBirthDate(), a.getNationality(), "   "));
  }
}
