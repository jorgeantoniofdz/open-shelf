package com.openshelf.infrastructure.inmemory;

import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanRepository;
import com.openshelf.domain.shared.Preconditions;
import com.openshelf.domain.user.Member;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory loan storage for single-threaded use.
 *
 * <p>Stores entity references and returns unmodifiable list snapshots.
 */
public final class InMemoryLoanRepository implements LoanRepository {

  private final Map<String, Loan> store = new HashMap<>();

  @Override
  public void save(Loan loan) {
    Preconditions.checkNotNull(loan, "Loan cannot be null");
    store.put(loan.getId(), loan);
  }

  @Override
  public Optional<Loan> findById(String id) {
    Preconditions.requireNotBlank(id, "id cannot be null or blank");
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<Loan> findAll() {
    return List.copyOf(store.values());
  }

  @Override
  public List<Loan> findByMember(Member member) {
    Preconditions.checkNotNull(member, "Member cannot be null");
    return store.values().stream()
        .filter(loan -> loan.getMember().getId().equals(member.getId()))
        .toList();
  }

  @Override
  public List<Loan> findActiveByMember(Member member) {
    return findByMember(member).stream().filter(Loan::isActive).toList();
  }
}
