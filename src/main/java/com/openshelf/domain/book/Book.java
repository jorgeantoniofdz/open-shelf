package com.openshelf.domain.book;

import com.openshelf.domain.shared.isbn.IsbnValidator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a book in the library catalog.
 *
 * <p>Use {@link Book.Builder} to construct instances — the number of fields makes a plain
 * constructor impractical and hard to read at the call site.
 *
 * <pre>{@code
 * Book book = new Book.Builder()
 *     .title("Clean Code")
 *     .isbn("978-0132350884")
 *     .author(author)
 *     .language(Language.ENGLISH)
 *     .publicationDate(LocalDate.of(2008, 8, 1))
 *     .pageCount(431)
 *     .category(Category.PROGRAMMING)
 *     .build();
 * }</pre>
 */
public final class Book {

  private final String title;
  private final String isbn;
  private final LocalDate publicationDate;
  private final int pageCount;
  private final Language language;
  private final List<Category> categories;
  private final Author author;

  private Book(Builder builder) {
    this.title = builder.title;
    this.isbn = builder.isbn;
    this.publicationDate = builder.publicationDate;
    this.pageCount = builder.pageCount;
    this.language = builder.language;
    this.categories = Collections.unmodifiableList(new ArrayList<>(builder.categories));
    this.author = builder.author;
  }

  public String getTitle() {
    return title;
  }

  public String getIsbn() {
    return isbn;
  }

  public LocalDate getPublicationDate() {
    return publicationDate;
  }

  public int getPageCount() {
    return pageCount;
  }

  public Language getLanguage() {
    return language;
  }

  public List<Category> getCategories() {
    return categories;
  }

  public Author getAuthor() {
    return author;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Book)) {
      return false;
    }
    Book other = (Book) obj;
    return Objects.equals(isbn, other.isbn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isbn);
  }

  @Override
  public String toString() {
    return "Book{isbn='" + isbn + "', title='" + title + "'}";
  }

  /**
   * Fluent builder for {@link Book}.
   *
   * <p>Suppressions below are intentional for the builder pattern:
   *
   * <ul>
   *   <li>HiddenField — setter methods shadow fields by design ({@code .title(v)} not {@code
   *       .titleValue(v)})
   *   <li>JavadocMethod — single-line setter Javadoc is sufficient; full @param/@return would be
   *       noise on 7 identical one-liners
   * </ul>
   */
  @SuppressWarnings({
    "checkstyle:HiddenField",
    "checkstyle:JavadocMethod",
    "PMD.AvoidFieldNameMatchingMethodName",
    "PMD.TooManyMethods"
  })
  public static final class Builder {

    /** Maximum allowed length for a book title. */
    public static final int MAX_TITLE_LENGTH = 255;

    private String title;
    private String isbn;
    private LocalDate publicationDate;
    private int pageCount;
    private Language language;
    private final List<Category> categories = new ArrayList<>();
    private Author author;

    /** Sets the book title. */
    public Builder title(String title) {
      this.title = title;
      return this;
    }

    /** Sets the ISBN. */
    public Builder isbn(String isbn) {
      this.isbn = isbn;
      return this;
    }

    /** Sets the publication date. */
    public Builder publicationDate(LocalDate publicationDate) {
      this.publicationDate = publicationDate;
      return this;
    }

    /** Sets the page count. */
    public Builder pageCount(int pageCount) {
      this.pageCount = pageCount;
      return this;
    }

    /** Sets the language. */
    public Builder language(Language language) {
      this.language = language;
      return this;
    }

    /** Adds a category to the book. */
    public Builder category(Category category) {
      this.categories.add(category);
      return this;
    }

    /** Sets the author. */
    public Builder author(Author author) {
      this.author = author;
      return this;
    }

    /**
     * Builds the {@link Book} instance.
     *
     * @return a new, immutable Book
     */
    public Book build() {
      List<String> errors = validate();

      if (!errors.isEmpty()) {
        throw new IllegalStateException("Invalid Book: " + String.join("; ", errors));
      }

      return new Book(this);
    }

    private List<String> validate() {
      List<String> errors = new ArrayList<>();
      validateTitle(errors);
      validatePublicationDate(errors);
      validateIsbn(errors);

      if (pageCount <= 0) {
        errors.add("Page count must be a positive integer");
      }

      if (language == null) {
        errors.add("Language cannot be null");
      }

      if (author == null) {
        errors.add("Author cannot be null");
      }

      if (categories.contains(null)) {
        errors.add("Categories cannot contain null values");
      }

      return errors;
    }

    private void validateTitle(List<String> errors) {
      if (title == null || title.isBlank()) {
        errors.add("Title cannot be null or blank");
      } else if (title.length() > MAX_TITLE_LENGTH) {
        errors.add("Title cannot exceed " + MAX_TITLE_LENGTH + " characters");
      }
    }

    private void validatePublicationDate(List<String> errors) {
      if (publicationDate == null) {
        errors.add("Publication date cannot be null");
      } else if (publicationDate.isAfter(LocalDate.now())) {
        errors.add("Publication date cannot be in the future");
      }
    }

    private void validateIsbn(List<String> errors) {
      if (isbn == null || isbn.isBlank()) {
        errors.add("ISBN cannot be null or blank");
      } else if (!IsbnValidator.isValidIsbn13(isbn)) {
        errors.add("ISBN must be a valid ISBN-13 format");
      }
    }
  }
}
