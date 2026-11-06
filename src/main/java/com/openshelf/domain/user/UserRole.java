package com.openshelf.domain.user;

/**
 * Defines all valid roles within the library system.
 *
 * <p>Single source of truth for user roles. Extend this enum to add new roles; the type system
 * propagates the change everywhere {@link UserRole} is referenced.
 */
public enum UserRole {
  MEMBER,
  LIBRARIAN,
  ADMINISTRATOR
}
