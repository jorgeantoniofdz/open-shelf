package com.openshelf.user;

import com.openshelf.shared.EmailValidator;
import com.openshelf.shared.Preconditions;

/**
 * Base class for all library users.
 *
 * <p>Extend this class to create concrete user types (Member, Librarian, Administrator). Each
 * subclass must declare its {@link UserRole}.
 */
public abstract class User {

  /** Maximum allowed length for a user's name. */
  public static final int MAX_NAME_LENGTH = 100;

  private final String id;
  private final String name;
  private final String email;

  /**
   * Creates a User.
   *
   * @param id unique identifier (no whitespace allowed)
   * @param name full name (max {@value #MAX_NAME_LENGTH} characters)
   * @param email valid email address
   */
  protected User(String id, String name, String email) {
    Preconditions.requireNotBlank(id, "User ID cannot be null or blank");
    Preconditions.requireNotWhitespace(id, "User ID cannot contain whitespace");
    this.id = id;

    Preconditions.requireNotBlank(name, "User name cannot be null or blank");
    Preconditions.checkArgument(
        name.length() <= MAX_NAME_LENGTH,
        "User name must not exceed " + MAX_NAME_LENGTH + " characters");
    this.name = name;

    Preconditions.checkArgument(EmailValidator.isValidEmail(email), "Invalid email address");
    this.email = email;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  /**
   * Returns the role this user has in the library system.
   *
   * @return the user's role
   */
  public abstract UserRole getRole();
}
