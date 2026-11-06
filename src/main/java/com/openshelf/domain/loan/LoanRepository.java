package com.openshelf.domain.loan;

import com.openshelf.domain.user.Member;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for loans, keyed by loan ID.
 *
 * <p>Implementations reject null arguments and blank keys with {@link IllegalArgumentException}.
 * Lists are unmodifiable snapshots of membership, not deep copies of mutable loans, and have no
 * guaranteed ordering. Empty results are never null. Callers must save changed loans to persist
 * updates; queries do not refresh overdue status automatically.
 */
public interface LoanRepository {

  /**
   * Saves a loan, replacing any existing loan with the same ID.
   *
   * @param loan loan to store
   */
  void save(Loan loan);

  /**
   * Looks up a loan by ID.
   *
   * @param id exact loan ID
   * @return the matching loan, or an empty optional if absent
   */
  Optional<Loan> findById(String id);

  /**
   * Finds all loans for a member, comparing member IDs rather than object instances.
   *
   * @param member member whose loan history is requested
   * @return an unmodifiable list of matching loans in any state, possibly empty
   */
  List<Loan> findByMember(Member member);

  /**
   * Finds loans with stored status {@link LoanStatus#ACTIVE}, matching by member ID.
   *
   * <p>Overdue and returned loans are excluded. A borrowing limit that includes overdue loans must
   * instead consider all unreturned loans from {@link #findByMember(Member)}.
   *
   * @param member member whose active loans are requested
   * @return an unmodifiable list of active loans, possibly empty
   */
  List<Loan> findActiveByMember(Member member);

  /**
   * Lists all stored loans in any state.
   *
   * @return an unmodifiable list of loans, possibly empty
   */
  List<Loan> findAll();
}
