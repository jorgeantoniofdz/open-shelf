package com.openshelf.domain.book;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for books, keyed by their exact ISBN string.
 *
 * <p>Implementations reject null arguments and blank keys with {@link IllegalArgumentException}.
 * Lists are unmodifiable snapshots with no guaranteed ordering; empty results are never null.
 */
public interface BookRepository {

  /**
   * Saves a book, replacing any existing book with the same ISBN.
   *
   * @param book book to store
   */
  void save(Book book);

  /**
   * Looks up a book by ISBN without normalizing the key.
   *
   * @param isbn exact ISBN string
   * @return the matching book, or an empty optional if absent
   */
  Optional<Book> findByIsbn(String isbn);

  /**
   * Finds books whose author equals the supplied author.
   *
   * @param author author identified by name, surname and birth date
   * @return an unmodifiable list of matching books, possibly empty
   */
  List<Book> findByAuthor(Author author);

  /**
   * Lists all stored books.
   *
   * @return an unmodifiable list of books, possibly empty
   */
  List<Book> findAll();

  /**
   * Removes a book by ISBN; an absent book is a no-op.
   *
   * @param isbn exact ISBN string to remove
   */
  void delete(String isbn);
}
