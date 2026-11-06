package com.openshelf.infrastructure.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openshelf.application.service.LoanService;
import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.BookRepository;
import com.openshelf.domain.book.Category;
import com.openshelf.domain.book.Language;
import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanStatus;
import com.openshelf.domain.user.Member;
import com.openshelf.infrastructure.inmemory.InMemoryLoanRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PostgresBookRepositoryIT {

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16-alpine")
          .withDatabaseName("openshelf_test")
          .withUsername("openshelf_test")
          .withPassword("openshelf_test");

  private PGSimpleDataSource dataSource;
  private BookRepository repository;
  private String schema;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = new PGSimpleDataSource();
    dataSource.setURL(POSTGRES.getJdbcUrl());
    dataSource.setUser(POSTGRES.getUsername());
    dataSource.setPassword(POSTGRES.getPassword());
    schema = "test_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
    }
    dataSource.setCurrentSchema(schema);
    assertEquals(1, DatabaseMigrator.migrate(dataSource));
    repository = new PostgresBookRepository(dataSource);
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (schema != null) {
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement()) {
        statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
      }
    }
  }

  private Author author() {
    return new Author(
        "María", "O'Connor", LocalDate.of(1970, 2, 3), "Irish", "Biography\nwith 'quotes' and ñ.");
  }

  private Book book(String isbn, String title, Author author, Category... categories) {
    Book.Builder builder =
        new Book.Builder()
            .isbn(isbn)
            .title(title)
            .publicationDate(LocalDate.of(2008, 8, 1))
            .pageCount(431)
            .language(Language.SPANISH)
            .author(author);
    for (Category category : categories) {
      builder.category(category);
    }
    return builder.build();
  }

  private Book book() {
    return book(
        "9780132350884",
        "Programación 'segura'",
        author(),
        Category.SCIENCE,
        Category.PROGRAMMING,
        Category.SCIENCE);
  }

  private void assertBookContents(Book expected, Book actual) {
    assertEquals(expected.getIsbn(), actual.getIsbn());
    assertEquals(expected.getTitle(), actual.getTitle());
    assertEquals(expected.getPublicationDate(), actual.getPublicationDate());
    assertEquals(expected.getPageCount(), actual.getPageCount());
    assertEquals(expected.getLanguage(), actual.getLanguage());
    assertEquals(expected.getCategories(), actual.getCategories());
    assertEquals(expected.getAuthor(), actual.getAuthor());
    assertEquals(expected.getAuthor().getNationality(), actual.getAuthor().getNationality());
    assertEquals(expected.getAuthor().getBiography(), actual.getAuthor().getBiography());
  }

  @Test
  void emptyRepository_returnsEmptyResults() {
    assertTrue(repository.findAll().isEmpty());
    assertTrue(repository.findByIsbn("missing").isEmpty());
    assertTrue(repository.findByAuthor(author()).isEmpty());
  }

  @Test
  void migrate_again_preservesDataAndHistory() throws SQLException {
    Book original = book();
    repository.save(original);
    assertEquals(0, DatabaseMigrator.migrate(dataSource));
    assertBookContents(original, repository.findByIsbn(original.getIsbn()).orElseThrow());
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rows =
            statement.executeQuery(
                "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL")) {
      assertTrue(rows.next());
      assertEquals("1", rows.getString("version"));
      assertTrue(rows.getBoolean("success"));
      assertTrue(!rows.next());
    }
  }

  @Test
  void migrate_rejectsChangedChecksumWithoutModifyingBooks() throws SQLException {
    Book original = book();
    repository.save(original);
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "UPDATE flyway_schema_history SET checksum = checksum + 1 WHERE version = '1'");
    }
    assertThrows(FlywayException.class, () -> DatabaseMigrator.migrate(dataSource));
    assertBookContents(original, repository.findByIsbn(original.getIsbn()).orElseThrow());
  }

  @Test
  void migrate_rejectsUnversionedNonemptySchema() throws SQLException {
    Book original = book();
    repository.save(original);
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE flyway_schema_history");
    }
    assertThrows(FlywayException.class, () -> DatabaseMigrator.migrate(dataSource));
    assertBookContents(original, repository.findByIsbn(original.getIsbn()).orElseThrow());
  }

  @Test
  void save_roundTripsAllFieldsAcrossRepositoryInstances() {
    Book original = book();
    repository.save(original);
    Book loaded =
        new PostgresBookRepository(dataSource).findByIsbn(original.getIsbn()).orElseThrow();
    assertNotSame(original, loaded);
    assertBookContents(original, loaded);
    assertBookContents(original, repository.findAll().getFirst());
  }

  @Test
  void save_replacesAllFieldsAndClearsCategories() {
    Book original = book();
    repository.save(original);
    Author updatedAuthor = new Author("Other", "Writer", LocalDate.of(1980, 1, 1), "Argentine", "");
    Book updated =
        new Book.Builder()
            .isbn(original.getIsbn())
            .title("Revised")
            .publicationDate(LocalDate.of(2010, 1, 1))
            .pageCount(100)
            .language(Language.ENGLISH)
            .author(updatedAuthor)
            .build();
    repository.save(updated);

    assertEquals(1, repository.findAll().size());
    assertBookContents(updated, repository.findByIsbn(original.getIsbn()).orElseThrow());
    assertTrue(repository.findByAuthor(original.getAuthor()).isEmpty());
    assertEquals(List.of(updated), repository.findByAuthor(updatedAuthor));
  }

  @Test
  void findByAuthor_matchesIdentityAndKeepsIndependentAuthorDetails() {
    Book first = book();
    Author sameIdentity =
        new Author(
            author().getFirstName(),
            author().getLastName(),
            author().getBirthDate(),
            "Other nationality",
            "Different biography");
    Book second = book("9780201633610", "Second", sameIdentity);
    repository.save(first);
    repository.save(second);

    assertEquals(Set.of(first, second), Set.copyOf(repository.findByAuthor(sameIdentity)));
    assertBookContents(first, repository.findByIsbn(first.getIsbn()).orElseThrow());
    assertBookContents(second, repository.findByIsbn(second.getIsbn()).orElseThrow());
    Author differentBirthday =
        new Author(
            author().getFirstName(),
            author().getLastName(),
            author().getBirthDate().plusDays(1),
            "Irish",
            "");
    assertTrue(repository.findByAuthor(differentBirthday).isEmpty());
  }

  @Test
  void delete_isIdempotentAndDoesNotAffectOtherBooks() {
    Book first = book();
    Book second = book("9780201633610", "Second", author());
    repository.save(first);
    repository.save(second);
    repository.delete(first.getIsbn());
    repository.delete(first.getIsbn());
    repository.delete("missing");

    assertTrue(repository.findByIsbn(first.getIsbn()).isEmpty());
    assertEquals(List.of(second), repository.findAll());
  }

  @Test
  void queries_treatSqlMetacharactersAsData() {
    repository.save(book());
    String injection = "' OR 1=1; DROP TABLE books; --";
    assertTrue(repository.findByIsbn(injection).isEmpty());
    repository.delete(injection);
    Author injectedAuthor = new Author(injection, "Writer", LocalDate.of(1970, 1, 1), "Irish", "");
    Book quoted = book("9780201633610", injection, injectedAuthor);
    repository.save(quoted);

    assertEquals(List.of(quoted), repository.findByAuthor(injectedAuthor));
    assertEquals(2, repository.findAll().size());
  }

  @Test
  void queryLists_areUnmodifiableSnapshots() {
    Book first = book();
    repository.save(first);
    List<Book> all = repository.findAll();
    List<Book> byAuthor = repository.findByAuthor(first.getAuthor());
    assertThrows(UnsupportedOperationException.class, all::clear);
    assertThrows(UnsupportedOperationException.class, byAuthor::clear);
    repository.delete(first.getIsbn());
    assertEquals(List.of(first), all);
    assertEquals(List.of(first), byAuthor);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void queries_rejectInvalidKeys(String key) {
    assertThrows(IllegalArgumentException.class, () -> repository.findByIsbn(key));
    assertThrows(IllegalArgumentException.class, () -> repository.delete(key));
  }

  @Test
  void objectOperations_rejectNull() {
    assertThrows(IllegalArgumentException.class, () -> new PostgresBookRepository(null));
    assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    assertThrows(IllegalArgumentException.class, () -> repository.findByAuthor(null));
  }

  @Test
  void sqlFailures_preserveCauseAndFailedUpsertKeepsOriginal() throws SQLException {
    repository.save(book());
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "ALTER TABLE books ADD CONSTRAINT reject_test_title CHECK (title <> 'Rejected')");
    }
    Book rejected = book(book().getIsbn(), "Rejected", author());
    PersistenceException failure =
        assertThrows(PersistenceException.class, () -> repository.save(rejected));
    assertInstanceOf(SQLException.class, failure.getCause());
    assertBookContents(book(), repository.findByIsbn(book().getIsbn()).orElseThrow());
  }

  @Test
  void loanService_worksWithPostgresCatalog() {
    repository.save(book());
    InMemoryLoanRepository loans = new InMemoryLoanRepository();
    LoanService service = new LoanService(loans, repository);
    Loan loan =
        service.checkout(
            new Member("m-1", "Member", "member@example.com"),
            book().getIsbn(),
            LocalDate.of(2020, 1, 1));
    assertBookContents(book(), loan.getBook());
    service.returnBook(loan.getId());
    assertEquals(LoanStatus.RETURNED, loans.findById(loan.getId()).orElseThrow().getStatus());
  }
}
