package com.openshelf.infrastructure.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.BookRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InMemoryBookRepositoryTest {

  private final BookRepository repository = new InMemoryBookRepository();

  @Test
  void emptyRepository_returnsEmptyResults() {
    assertTrue(repository.findAll().isEmpty());
    assertTrue(repository.findByIsbn("missing").isEmpty());
    assertTrue(repository.findByAuthor(RepositoryFixtures.author()).isEmpty());
  }

  @Test
  void save_replacesBookWithSameIsbn() {
    Book original = RepositoryFixtures.book();
    Book updated =
        RepositoryFixtures.book(original.getIsbn(), "Updated title", original.getAuthor());
    repository.save(original);
    assertSame(original, repository.findByIsbn(original.getIsbn()).orElseThrow());
    repository.save(updated);

    assertSame(updated, repository.findByIsbn(original.getIsbn()).orElseThrow());
    assertEquals(List.of(updated), repository.findAll());
  }

  @Test
  void findByAuthor_matchesEquivalentAuthorsAndExcludesOthers() {
    Book first = RepositoryFixtures.book();
    Author other = new Author("Other", "Author", LocalDate.of(1900, 1, 1), "Irish", "");
    Book second = RepositoryFixtures.book("9780201633610", "Other book", other);
    repository.save(first);
    repository.save(second);

    assertEquals(List.of(first), repository.findByAuthor(RepositoryFixtures.author()));
    assertEquals(List.of(second), repository.findByAuthor(other));
    assertEquals(2, repository.findAll().size());
  }

  @Test
  void delete_isIdempotentAndPreservesOtherBooks() {
    Book first = RepositoryFixtures.book();
    Book second = RepositoryFixtures.book("9780201633610", "Other book", first.getAuthor());
    repository.save(first);
    repository.save(second);
    repository.delete(first.getIsbn());
    repository.delete(first.getIsbn());
    repository.delete("missing");

    assertTrue(repository.findByIsbn(first.getIsbn()).isEmpty());
    assertEquals(List.of(second), repository.findAll());
  }

  @Test
  void queryLists_areUnmodifiableSnapshots() {
    Book book = RepositoryFixtures.book();
    repository.save(book);
    List<Book> all = repository.findAll();
    List<Book> byAuthor = repository.findByAuthor(book.getAuthor());
    assertThrows(UnsupportedOperationException.class, all::clear);
    assertThrows(UnsupportedOperationException.class, byAuthor::clear);
    repository.delete(book.getIsbn());

    assertEquals(List.of(book), all);
    assertEquals(List.of(book), byAuthor);
    assertTrue(repository.findAll().isEmpty());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void keyOperations_rejectInvalidKeys(String key) {
    assertThrows(IllegalArgumentException.class, () -> repository.findByIsbn(key));
    assertThrows(IllegalArgumentException.class, () -> repository.delete(key));
  }

  @Test
  void objectOperations_rejectNull() {
    assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    assertThrows(IllegalArgumentException.class, () -> repository.findByAuthor(null));
  }
}
