package com.openshelf.domain.user;

/** An administrator with full system access. */
public final class Administrator extends User {

  /**
   * Creates an Administrator.
   *
   * @param id unique identifier
   * @param name full name
   * @param email email address
   */
  public Administrator(String id, String name, String email) {
    super(id, name, email);
  }

  @Override
  public UserRole getRole() {
    return UserRole.ADMINISTRATOR;
  }
}
