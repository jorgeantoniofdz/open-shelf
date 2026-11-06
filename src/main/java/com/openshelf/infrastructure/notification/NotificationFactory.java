package com.openshelf.infrastructure.notification;

import com.openshelf.domain.notification.Notification;
import com.openshelf.domain.notification.NotificationSender;
import com.openshelf.domain.notification.NotificationService;

/** Creates notification channels; no-argument methods log simulations without external delivery. */
public final class NotificationFactory {

  private static final System.Logger LOGGER = System.getLogger(NotificationFactory.class.getName());

  private NotificationFactory() {}

  /**
   * Creates an email channel that logs simulated delivery.
   *
   * @return simulated email notification service
   */
  public static NotificationService email() {
    return email(NotificationFactory::logSimulation);
  }

  /**
   * Creates an SMS channel that logs simulated delivery.
   *
   * @return simulated SMS notification service
   */
  public static NotificationService sms() {
    return sms(NotificationFactory::logSimulation);
  }

  /**
   * Creates a push channel that logs simulated delivery.
   *
   * @return simulated push notification service
   */
  public static NotificationService push() {
    return push(NotificationFactory::logSimulation);
  }

  /**
   * Creates an email channel with a supplied delivery adapter.
   *
   * @param sender notification delivery or recording adapter
   * @return email notification service
   */
  public static NotificationService email(NotificationSender sender) {
    return new EmailNotificationService(sender);
  }

  /**
   * Creates an SMS channel with a supplied delivery adapter.
   *
   * @param sender notification delivery or recording adapter
   * @return SMS notification service
   */
  public static NotificationService sms(NotificationSender sender) {
    return new SmsNotificationService(sender);
  }

  /**
   * Creates a push channel with a supplied delivery adapter.
   *
   * @param sender notification delivery or recording adapter
   * @return push notification service
   */
  public static NotificationService push(NotificationSender sender) {
    return new PushNotificationService(sender);
  }

  private static void logSimulation(Notification notification) {
    LOGGER.log(
        System.Logger.Level.INFO,
        "SIMULATED {0} notification for user {1}: {2}",
        notification.channel(),
        notification.recipientId(),
        notification.message());
  }
}
