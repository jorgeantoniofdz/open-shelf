package com.openshelf.domain.user;

/** A library member who can borrow books. */
public final class Member extends User {

  /** Maximum number of books a member can borrow concurrently. */
  public static final int MAX_ACTIVE_LOANS = 5;

  /**
   * Creates a Member.
   *
   * @param id unique identifier
   * @param name full name
   * @param email email address
   */
  public Member(String id, String name, String email) {
    super(id, name, email);
  }

  @Override
  public UserRole getRole() {
    return UserRole.MEMBER;
  }
}
