package com.openshelf.domain.user;

/** A librarian who manages the catalog and processes loans. */
public final class Librarian extends User {

  /**
   * Creates a Librarian.
   *
   * @param id unique identifier
   * @param name full name
   * @param email email address
   */
  public Librarian(String id, String name, String email) {
    super(id, name, email);
  }

  @Override
  public UserRole getRole() {
    return UserRole.LIBRARIAN;
  }
}
