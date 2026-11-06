package com.openshelf.domain.notification;

/** Boundary between channel selection and delivery or recording of notifications. */
@FunctionalInterface
public interface NotificationSender {

  /**
   * Sends or records an immutable notification.
   *
   * @param notification validated notification
   */
  void send(Notification notification);
}
