package com.openshelf.domain.book;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
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
  void equals_sameIdentity_obeysEqualityContract() {
    Author first = validAuthor();
    Author second = validAuthor();
    Author third = validAuthor();

    assertEquals(first, first);
    assertEquals(first, second);
    assertEquals(second, first);
    assertEquals(second, third);
    assertEquals(first, third);
    assertEquals(first.hashCode(), second.hashCode());
    assertFalse(first.equals(null));
    assertNotEquals(first, "Oscar Wilde");
  }

  @Test
  void equals_differentDescriptiveDetails_preservesIdentity() {
    Author original = validAuthor();
    Author updated =
        new Author(
            original.getFirstName(),
            original.getLastName(),
            original.getBirthDate(),
            "British",
            "Updated biography");

    assertEquals(original, updated);
    assertEquals(original.hashCode(), updated.hashCode());
  }

  @Test
  void equals_differentIdentityFields_returnsFalse() {
    Author author = validAuthor();

    assertNotEquals(author, new Author("Other", "Wilde", author.getBirthDate(), "Irish", ""));
    assertNotEquals(author, new Author("Oscar", "Other", author.getBirthDate(), "Irish", ""));
    assertNotEquals(
        author, new Author("Oscar", "Wilde", author.getBirthDate().plusDays(1), "Irish", ""));
  }

  @Test
  void hashSet_equivalentAuthors_areDeduplicatedAndFound() {
    Set<Author> authors = new HashSet<>();
    authors.add(validAuthor());

    assertFalse(authors.add(validAuthor()));
    assertEquals(1, authors.size());
    assertTrue(authors.contains(validAuthor()));
    assertTrue(authors.remove(validAuthor()));
    assertTrue(authors.isEmpty());
  }

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
