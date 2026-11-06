package com.openshelf.infrastructure.inmemory;

import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.BookRepository;
import com.openshelf.domain.shared.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory book storage for single-threaded use.
 *
 * <p>Stores entity references and returns unmodifiable list snapshots.
 */
public final class InMemoryBookRepository implements BookRepository {

  private final Map<String, Book> store = new HashMap<>();

  @Override
  public void save(Book book) {
    Preconditions.checkNotNull(book, "Book cannot be null");
    store.put(book.getIsbn(), book);
  }

  @Override
  public Optional<Book> findByIsbn(String isbn) {
    Preconditions.requireNotBlank(isbn, "isbn cannot be null or blank");
    return Optional.ofNullable(store.get(isbn));
  }

  @Override
  public List<Book> findAll() {
    return List.copyOf(store.values());
  }

  @Override
  public List<Book> findByAuthor(Author author) {
    Preconditions.checkNotNull(author, "Author cannot be null");
    return store.values().stream().filter(book -> book.getAuthor().equals(author)).toList();
  }

  @Override
  public void delete(String isbn) {
    Preconditions.requireNotBlank(isbn, "ISBN cannot be null or blank");
    store.remove(isbn);
  }
}
