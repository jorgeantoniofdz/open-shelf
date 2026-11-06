package com.openshelf.infrastructure.inmemory;

import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.Language;
import com.openshelf.domain.user.Member;
import java.time.LocalDate;

final class RepositoryFixtures {

  private RepositoryFixtures() {}

  static Author author() {
    return new Author("Oscar", "Wilde", LocalDate.of(1854, 10, 16), "Irish", "");
  }

  static Book book(String isbn, String title, Author author) {
    return new Book.Builder()
        .title(title)
        .isbn(isbn)
        .publicationDate(LocalDate.of(2008, 8, 1))
        .pageCount(431)
        .language(Language.ENGLISH)
        .author(author)
        .build();
  }

  static Book book() {
    return book("9780132350884", "Clean Code", author());
  }

  static Member member(String id) {
    return new Member(id, "John Doe", "john@example.com");
  }
}
