package com.openshelf.infrastructure.inmemory;

import com.openshelf.domain.shared.Preconditions;
import com.openshelf.domain.user.User;
import com.openshelf.domain.user.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory user storage for single-threaded use.
 *
 * <p>Stores entity references and returns unmodifiable list snapshots.
 */
public final class InMemoryUserRepository implements UserRepository {

  private final Map<String, User> store = new HashMap<>();

  @Override
  public void save(User user) {
    Preconditions.checkNotNull(user, "User cannot be null");
    store.put(user.getId(), user);
  }

  @Override
  public Optional<User> findById(String id) {
    Preconditions.requireNotBlank(id, "id cannot be null or blank");
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<User> findAll() {
    return List.copyOf(store.values());
  }
}
