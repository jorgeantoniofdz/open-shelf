package com.openshelf.application.service;

import com.openshelf.application.loan.LoanEventListener;
import com.openshelf.domain.book.Book;
import com.openshelf.domain.book.BookRepository;
import com.openshelf.domain.loan.Loan;
import com.openshelf.domain.loan.LoanRepository;
import com.openshelf.domain.loan.LoanStatus;
import com.openshelf.domain.shared.Preconditions;
import com.openshelf.domain.user.Member;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Coordinates borrowing and returns through repository interfaces.
 *
 * <p>Intended for single-threaded use in the in-memory stage. Inventory availability and automatic
 * fine generation are not part of this service yet.
 */
public final class LoanService {

  private final LoanRepository loanRepository;
  private final BookRepository bookRepository;
  private final List<LoanEventListener> listeners;

  /**
   * Creates a service with its storage dependencies.
   *
   * @param loanRepository storage for loans
   * @param bookRepository catalog of books
   */
  public LoanService(LoanRepository loanRepository, BookRepository bookRepository) {
    this(loanRepository, bookRepository, List.of());
  }

  /**
   * Creates a service with observers invoked in list order after a saved return.
   *
   * @param loanRepository storage for loans
   * @param bookRepository catalog of books
   * @param listeners return observers, copied defensively; no null entries allowed
   */
  public LoanService(
      LoanRepository loanRepository,
      BookRepository bookRepository,
      List<LoanEventListener> listeners) {
    Preconditions.checkNotNull(loanRepository, "Loan repository cannot be null");
    Preconditions.checkNotNull(bookRepository, "Book repository cannot be null");
    Preconditions.checkNotNull(listeners, "Loan listeners cannot be null");
    listeners.forEach(
        listener -> Preconditions.checkNotNull(listener, "Loan listener cannot be null"));
    this.loanRepository = loanRepository;
    this.bookRepository = bookRepository;
    this.listeners = List.copyOf(listeners);
  }

  /**
   * Creates and saves an active loan for an existing catalog book.
   *
   * @param member borrowing member
   * @param isbn exact ISBN in the catalog
   * @param today checkout date, no later than today
   * @return the newly saved loan
   * @throws IllegalArgumentException if arguments are invalid or the book does not exist
   * @throws IllegalStateException if the member already has five or more unreturned loans
   */
  public Loan checkout(Member member, String isbn, LocalDate today) {
    Preconditions.checkNotNull(member, "Member cannot be null");
    Preconditions.requireNotBlank(isbn, "ISBN cannot be null or blank");
    Preconditions.checkNotNull(today, "Checkout date cannot be null");
    Book book =
        bookRepository
            .findByIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));
    long outstandingLoans =
        loanRepository.findByMember(member).stream()
            .filter(loan -> loan.getStatus() != LoanStatus.RETURNED)
            .count();
    if (outstandingLoans >= Member.MAX_ACTIVE_LOANS) {
      throw new IllegalStateException("Member has reached the unreturned loan limit");
    }
    Loan loan = new Loan(UUID.randomUUID().toString(), book, member, today);
    loanRepository.save(loan);
    return loan;
  }

  /**
   * Marks an active or overdue loan as returned, saves it, then notifies observers.
   *
   * <p>Observer failures propagate and stop subsequent delivery. The return remains saved; this
   * in-memory workflow provides no automatic retries or delivery guarantees.
   *
   * @param loanId ID of the loan to return
   * @throws IllegalArgumentException if the ID is invalid or the loan does not exist
   * @throws IllegalStateException if the loan was already returned
   */
  public void returnBook(String loanId) {
    Preconditions.requireNotBlank(loanId, "Loan ID cannot be null or blank");
    Loan loan =
        loanRepository
            .findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
    loan.markReturned();
    loanRepository.save(loan);
    listeners.forEach(listener -> listener.onBookReturned(loan));
  }
}
