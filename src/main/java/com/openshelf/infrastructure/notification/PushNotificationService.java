package com.openshelf.infrastructure.notification;

import com.openshelf.domain.notification.Notification;
import com.openshelf.domain.notification.NotificationChannel;
import com.openshelf.domain.notification.NotificationSender;
import com.openshelf.domain.notification.NotificationService;
import com.openshelf.domain.shared.Preconditions;
import com.openshelf.domain.user.User;

/** Routes PUSH notifications to an injected sender using the recipient's logical user ID. */
public final class PushNotificationService implements NotificationService {

  private final NotificationSender sender;

  /**
   * Creates a channel with a delivery or recording adapter.
   *
   * @param sender adapter receiving notification snapshots
   */
  public PushNotificationService(NotificationSender sender) {
    Preconditions.checkNotNull(sender, "Notification sender cannot be null");
    this.sender = sender;
  }

  @Override
  public void notify(User user, String message) {
    Preconditions.checkNotNull(user, "Notification recipient cannot be null");
    sender.send(new Notification(NotificationChannel.PUSH, user.getId(), message));
  }
}
