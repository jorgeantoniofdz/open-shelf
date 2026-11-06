package com.openshelf.infrastructure.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openshelf.domain.user.Administrator;
import com.openshelf.domain.user.Librarian;
import com.openshelf.domain.user.Member;
import com.openshelf.domain.user.User;
import com.openshelf.domain.user.UserRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InMemoryUserRepositoryTest {

  private final UserRepository repository = new InMemoryUserRepository();

  @Test
  void emptyRepository_returnsEmptyResults() {
    assertTrue(repository.findAll().isEmpty());
    assertTrue(repository.findById("missing").isEmpty());
  }

  @Test
  void save_supportsEveryUserRole() {
    User member = RepositoryFixtures.member("m-1");
    User librarian = new Librarian("l-1", "Jane Doe", "jane@example.com");
    User admin = new Administrator("a-1", "Admin", "admin@example.com");
    repository.save(member);
    repository.save(librarian);
    repository.save(admin);

    assertEquals(Set.of(member, librarian, admin), Set.copyOf(repository.findAll()));
    assertSame(member, repository.findById("m-1").orElseThrow());
    assertSame(librarian, repository.findById("l-1").orElseThrow());
    assertSame(admin, repository.findById("a-1").orElseThrow());
  }

  @Test
  void save_replacesUserWithSameId() {
    repository.save(RepositoryFixtures.member("m-1"));
    User updated = new Member("m-1", "Updated name", "updated@example.com");
    repository.save(updated);

    assertSame(updated, repository.findById("m-1").orElseThrow());
    assertEquals(List.of(updated), repository.findAll());
  }

  @Test
  void findAll_returnsUnmodifiableSnapshot() {
    User first = RepositoryFixtures.member("m-1");
    repository.save(first);
    List<User> snapshot = repository.findAll();
    assertThrows(UnsupportedOperationException.class, snapshot::clear);
    repository.save(RepositoryFixtures.member("m-2"));

    assertEquals(List.of(first), snapshot);
    assertEquals(2, repository.findAll().size());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void findById_rejectsInvalidKeys(String key) {
    assertThrows(IllegalArgumentException.class, () -> repository.findById(key));
  }

  @Test
  void save_rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> repository.save(null));
  }
}
