package com.openshelf.domain.user;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for users of every role, keyed by user ID.
 *
 * <p>Implementations reject null arguments and blank keys with {@link IllegalArgumentException}.
 * Lists are unmodifiable snapshots with no guaranteed ordering; empty results are never null.
 */
public interface UserRepository {

  /**
   * Saves a user, replacing any existing user with the same ID.
   *
   * @param user user to store
   */
  void save(User user);

  /**
   * Looks up a user by ID.
   *
   * @param id exact user ID
   * @return the matching user, or an empty optional if absent
   */
  Optional<User> findById(String id);

  /**
   * Lists all stored users, regardless of role.
   *
   * @return an unmodifiable list of users, possibly empty
   */
  List<User> findAll();
}
