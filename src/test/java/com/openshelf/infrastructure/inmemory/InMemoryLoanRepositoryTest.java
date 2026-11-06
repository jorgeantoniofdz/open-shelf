package com.openshelf.infrastructure.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanRepository;
import com.openshelf.domain.loan.LoanStatus;
import com.openshelf.domain.user.Member;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InMemoryLoanRepositoryTest {

  private final LoanRepository repository = new InMemoryLoanRepository();

  private Loan loan(String id, String memberId) {
    return new Loan(
        id,
        RepositoryFixtures.book(),
        RepositoryFixtures.member(memberId),
        LocalDate.of(2020, 1, 1));
  }

  @Test
  void emptyRepository_returnsEmptyResults() {
    Member member = RepositoryFixtures.member("m-1");
    assertTrue(repository.findAll().isEmpty());
    assertTrue(repository.findById("missing").isEmpty());
    assertTrue(repository.findByMember(member).isEmpty());
    assertTrue(repository.findActiveByMember(member).isEmpty());
  }

  @Test
  void save_replacesLoanWithSameId() {
    Loan original = loan("loan-1", "m-1");
    Loan replacement = loan("loan-1", "m-2");
    repository.save(original);
    assertSame(original, repository.findById("loan-1").orElseThrow());
    repository.save(replacement);

    assertSame(replacement, repository.findById("loan-1").orElseThrow());
    assertEquals(List.of(replacement), repository.findAll());
    assertTrue(repository.findByMember(original.getMember()).isEmpty());
  }

  @Test
  void memberQueries_matchByIdAndFilterStoredStatus() {
    Loan active = loan("active", "m-1");
    Loan overdue = loan("overdue", "m-1");
    Loan returned = loan("returned", "m-1");
    Loan other = loan("other", "m-2");
    overdue.refreshOverdueStatus(overdue.getDueDate().plusDays(1));
    returned.markReturned();
    repository.save(active);
    repository.save(overdue);
    repository.save(returned);
    repository.save(other);
    Member sameId = new Member("m-1", "Changed name", "changed@example.com");

    assertEquals(Set.of(active, overdue, returned), Set.copyOf(repository.findByMember(sameId)));
    assertEquals(List.of(active), repository.findActiveByMember(sameId));
    assertEquals(List.of(other), repository.findActiveByMember(other.getMember()));
    assertEquals(LoanStatus.ACTIVE, active.getStatus());
    assertTrue(repository.findByMember(RepositoryFixtures.member("unknown")).isEmpty());
  }

  @Test
  void save_updatedStatus_changesActiveQuery() {
    Loan loan = loan("loan-1", "m-1");
    repository.save(loan);
    loan.markReturned();
    repository.save(loan);

    assertTrue(repository.findActiveByMember(loan.getMember()).isEmpty());
    assertEquals(List.of(loan), repository.findByMember(loan.getMember()));
    assertEquals(LoanStatus.RETURNED, repository.findById("loan-1").orElseThrow().getStatus());
  }

  @Test
  void queryLists_areUnmodifiableSnapshotsOfMembership() {
    Loan first = loan("loan-1", "m-1");
    repository.save(first);
    List<Loan> all = repository.findAll();
    List<Loan> byMember = repository.findByMember(first.getMember());
    List<Loan> active = repository.findActiveByMember(first.getMember());
    assertThrows(UnsupportedOperationException.class, all::clear);
    assertThrows(UnsupportedOperationException.class, byMember::clear);
    assertThrows(UnsupportedOperationException.class, active::clear);
    repository.save(loan("loan-2", "m-1"));

    assertEquals(List.of(first), all);
    assertEquals(List.of(first), byMember);
    assertEquals(List.of(first), active);
    assertEquals(2, repository.findAll().size());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void findById_rejectsInvalidKeys(String key) {
    assertThrows(IllegalArgumentException.class, () -> repository.findById(key));
  }

  @Test
  void objectOperations_rejectNull() {
    assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    assertThrows(IllegalArgumentException.class, () -> repository.findByMember(null));
    assertThrows(IllegalArgumentException.class, () -> repository.findActiveByMember(null));
  }
}
