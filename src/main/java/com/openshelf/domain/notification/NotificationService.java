package com.openshelf.domain.notification;

import com.openshelf.domain.user.User;

/** Notification boundary; implementations deliver, record or simulate messages as configured. */
@FunctionalInterface
public interface NotificationService {

  /**
   * Sends a message through the configured delivery, recording or simulation adapter.
   *
   * @param user intended recipient, never null
   * @param message nonblank notification text
   * @throws IllegalArgumentException if the recipient is null or the message is null or blank
   */
  void notify(User user, String message);
}
