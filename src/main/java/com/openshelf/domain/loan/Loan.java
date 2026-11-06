package com.openshelf.domain.loan;

import com.openshelf.domain.book.Book;
import com.openshelf.domain.shared.Preconditions;
import com.openshelf.domain.user.Member;
import java.time.LocalDate;

/**
 * Represents a book borrowing transaction.
 *
 * <p>A loan starts in {@link LoanStatus#ACTIVE} and transitions to {@link LoanStatus#RETURNED} when
 * the book is brought back, or to {@link LoanStatus#OVERDUE} when the due date passes.
 */
public final class Loan {

  /** Standard loan duration in days. */
  public static final int DURATION_DAYS = 14;

  private final String id;
  private final Book book;
  private final Member member;
  private final LocalDate loanDate;
  private final LocalDate dueDate;
  private LoanStatus status;

  /**
   * Creates an active Loan.
   *
   * @param id unique identifier
   * @param book the borrowed book
   * @param member the borrowing member
   * @param loanDate the date the book was checked out
   */
  public Loan(String id, Book book, Member member, LocalDate loanDate) {
    Preconditions.requireNotBlank(id, "Loan ID cannot be null or blank");
    this.id = id;

    Preconditions.checkNotNull(book, "Book cannot be null");
    this.book = book;

    Preconditions.checkNotNull(member, "Member cannot be null");
    this.member = member;

    Preconditions.checkNotNull(loanDate, "Loan date cannot be null");
    Preconditions.checkArgument(
        !loanDate.isAfter(LocalDate.now()), "Loan date cannot be in the future");
    this.loanDate = loanDate;
    this.dueDate = loanDate.plusDays(DURATION_DAYS);
    this.status = LoanStatus.ACTIVE;
  }

  public String getId() {
    return id;
  }

  public Book getBook() {
    return book;
  }

  public Member getMember() {
    return member;
  }

  public LocalDate getLoanDate() {
    return loanDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public LoanStatus getStatus() {
    return status;
  }

  /**
   * Marks this loan as returned.
   *
   * @throws IllegalStateException if the current status does not allow a return transition
   */
  public void markReturned() {
    if (!status.canTransitionTo(LoanStatus.RETURNED)) {
      throw new IllegalStateException("Cannot return a loan with status: " + status);
    }
    this.status = LoanStatus.RETURNED;
  }

  /**
   * Transitions this loan to OVERDUE if today is past the due date and the loan is still active.
   *
   * @param today the current date used to evaluate overdue condition
   */
  public void refreshOverdueStatus(LocalDate today) {
    if (status.canTransitionTo(LoanStatus.OVERDUE) && today.isAfter(dueDate)) {
      this.status = LoanStatus.OVERDUE;
    }
  }

  /**
   * Returns true if the loan is currently active.
   *
   * @return true if status is {@link LoanStatus#ACTIVE}
   */
  public boolean isActive() {
    return status == LoanStatus.ACTIVE;
  }
}
