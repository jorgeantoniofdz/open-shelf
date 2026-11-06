package com.openshelf.domain.book;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.TooManyMethods")
class BookBuilderTest {

  private static final String VALID_ISBN = "9780132350884";

  private Author validAuthor() {
    return new Author("Oscar", "Wilde", LocalDate.of(1854, 10, 16), "Irish", "");
  }

  private Book.Builder validBuilder() {
    return new Book.Builder()
        .title("Clean Code")
        .isbn(VALID_ISBN)
        .publicationDate(LocalDate.of(2008, 8, 1))
        .pageCount(431)
        .language(Language.ENGLISH)
        .author(validAuthor());
  }

  // -------------------- happy path --------------------

  @Test
  void build_withAllValidFields_doesNotThrow() {
    assertDoesNotThrow(() -> validBuilder().build());
  }

  // -------------------- title --------------------

  @Test
  void build_titleIsNull_throwsWithMessage() {
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> validBuilder().title(null).build());
    assertTrue(ex.getMessage().contains("Title cannot be null or blank"));
  }

  @Test
  void build_titleIsBlank_throwsWithMessage() {
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> validBuilder().title("").build());
    assertTrue(ex.getMessage().contains("Title cannot be null or blank"));
  }

  @Test
  void build_titleExceedsMaxLength_throwsWithMessage() {
    String longTitle = "A".repeat(Book.Builder.MAX_TITLE_LENGTH + 1);
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> validBuilder().title(longTitle).build());
    assertTrue(ex.getMessage().contains("Title cannot exceed"));
  }

  // -------------------- isbn --------------------

  @Test
  void build_isbnIsNull_throwsWithMessage() {
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> validBuilder().isbn(null).build());
    assertTrue(ex.getMessage().contains("ISBN cannot be null or blank"));
  }

  @Test
  void build_isbnIsBlank_throwsWithMessage() {
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> validBuilder().isbn("").build());
    assertTrue(ex.getMessage().contains("ISBN cannot be null or blank"));
  }

  @Test
  void build_isbnHasInvalidFormat_throwsWithMessage() {
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> validBuilder().isbn("not-an-isbn").build());
    assertTrue(ex.getMessage().contains("ISBN must be a valid ISBN-13 format"));
  }

  // -------------------- publicationDate --------------------

  @Test
  void build_publicationDateIsNull_throwsIllegalStateException() {
    assertThrows(IllegalStateException.class, () -> validBuilder().publicationDate(null).build());
  }

  @Test
  void build_publicationDateInFuture_throwsIllegalStateException() {
    assertThrows(
        IllegalStateException.class,
        () -> validBuilder().publicationDate(LocalDate.now().plusDays(1)).build());
  }

  // -------------------- pageCount --------------------

  @Test
  void build_pageCountIsZero_throwsIllegalStateException() {
    assertThrows(IllegalStateException.class, () -> validBuilder().pageCount(0).build());
  }

  @Test
  void build_pageCountIsNegative_throwsIllegalStateException() {
    assertThrows(IllegalStateException.class, () -> validBuilder().pageCount(-1).build());
  }

  // -------------------- language --------------------

  @Test
  void build_languageIsNull_throwsIllegalStateException() {
    assertThrows(IllegalStateException.class, () -> validBuilder().language(null).build());
  }

  // -------------------- author --------------------

  @Test
  void build_authorIsNull_throwsIllegalStateException() {
    assertThrows(IllegalStateException.class, () -> validBuilder().author(null).build());
  }

  // -------------------- multiple errors --------------------

  @Test
  void build_multipleMissingFields_exceptionMessageContainsAllErrors() {
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> new Book.Builder().build());
    assertTrue(ex.getMessage().contains(";"));
  }
}
