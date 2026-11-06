package com.openshelf.infrastructure.postgres;

import com.openshelf.domain.book.Author;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.BookRepository;
import com.openshelf.domain.book.Category;
import com.openshelf.domain.book.Language;
import com.openshelf.domain.shared.Preconditions;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * JDBC book storage using one connection per operation and an atomic upsert per save.
 *
 * <p>The supplied data source must provide auto-commit connections pointing at an initialized
 * schema. This repository closes acquired connections, but does not own or close the data source.
 */
public final class PostgresBookRepository implements BookRepository {

  private static final String SELECT = "SELECT * FROM books";
  private static final String UPSERT =
      """
      INSERT INTO books (isbn, title, publication_date, page_count, language,
          author_first_name, author_last_name, author_birth_date, author_nationality,
          author_biography, categories)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (isbn) DO UPDATE SET
          title = EXCLUDED.title, publication_date = EXCLUDED.publication_date,
          page_count = EXCLUDED.page_count, language = EXCLUDED.language,
          author_first_name = EXCLUDED.author_first_name,
          author_last_name = EXCLUDED.author_last_name,
          author_birth_date = EXCLUDED.author_birth_date,
          author_nationality = EXCLUDED.author_nationality,
          author_biography = EXCLUDED.author_biography, categories = EXCLUDED.categories
      """;

  private final DataSource dataSource;

  /**
   * Creates a repository without opening a connection or modifying the schema.
   *
   * @param dataSource source of auto-commit JDBC connections
   */
  public PostgresBookRepository(DataSource dataSource) {
    Preconditions.checkNotNull(dataSource, "Data source cannot be null");
    this.dataSource = dataSource;
  }

  @Override
  public void save(Book book) {
    Preconditions.checkNotNull(book, "Book cannot be null");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPSERT)) {
      Array categories =
          connection.createArrayOf(
              "text", book.getCategories().stream().map(Enum::name).toArray(String[]::new));
      try {
        bindBook(statement, book, categories);
        statement.executeUpdate();
      } finally {
        categories.free();
      }
    } catch (SQLException exception) {
      throw new PersistenceException("Could not save book", exception);
    }
  }

  @Override
  public Optional<Book> findByIsbn(String isbn) {
    Preconditions.requireNotBlank(isbn, "ISBN cannot be null or blank");
    return query(SELECT + " WHERE isbn = ?", statement -> statement.setString(1, isbn)).stream()
        .findFirst();
  }

  @Override
  public List<Book> findByAuthor(Author author) {
    Preconditions.checkNotNull(author, "Author cannot be null");
    return query(
        SELECT + " WHERE author_first_name = ? AND author_last_name = ? AND author_birth_date = ?",
        statement -> {
          int index = 1;
          statement.setString(index++, author.getFirstName());
          statement.setString(index++, author.getLastName());
          statement.setObject(index, author.getBirthDate());
        });
  }

  @Override
  public List<Book> findAll() {
    return query(SELECT, statement -> {});
  }

  @Override
  public void delete(String isbn) {
    Preconditions.requireNotBlank(isbn, "ISBN cannot be null or blank");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM books WHERE isbn = ?")) {
      statement.setString(1, isbn);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new PersistenceException("Could not delete book", exception);
    }
  }

  private List<Book> query(String sql, StatementBinder binder) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      binder.bind(statement);
      try (ResultSet rows = statement.executeQuery()) {
        List<Book> books = new ArrayList<>();
        while (rows.next()) {
          books.add(readBook(rows));
        }
        return List.copyOf(books);
      }
    } catch (SQLException exception) {
      throw new PersistenceException("Could not query books", exception);
    }
  }

  private static void bindBook(PreparedStatement statement, Book book, Array categories)
      throws SQLException {
    int index = 1;
    statement.setString(index++, book.getIsbn());
    statement.setString(index++, book.getTitle());
    statement.setObject(index++, book.getPublicationDate());
    statement.setInt(index++, book.getPageCount());
    statement.setString(index++, book.getLanguage().name());
    Author author = book.getAuthor();
    statement.setString(index++, author.getFirstName());
    statement.setString(index++, author.getLastName());
    statement.setObject(index++, author.getBirthDate());
    statement.setString(index++, author.getNationality());
    statement.setString(index++, author.getBiography());
    statement.setArray(index, categories);
  }

  private static Book readBook(ResultSet row) throws SQLException {
    Author author =
        new Author(
            row.getString("author_first_name"),
            row.getString("author_last_name"),
            row.getObject("author_birth_date", LocalDate.class),
            row.getString("author_nationality"),
            row.getString("author_biography"));
    Book.Builder builder =
        new Book.Builder()
            .isbn(row.getString("isbn"))
            .title(row.getString("title"))
            .publicationDate(row.getObject("publication_date", LocalDate.class))
            .pageCount(row.getInt("page_count"))
            .language(Language.valueOf(row.getString("language")))
            .author(author);
    Array categories = row.getArray("categories");
    try {
      for (String category : (String[]) categories.getArray()) {
        builder.category(Category.valueOf(category));
      }
    } finally {
      categories.free();
    }
    return builder.build();
  }

  @FunctionalInterface
  private interface StatementBinder {
    void bind(PreparedStatement statement) throws SQLException;
  }
}
