package com.openshelf.domain.notification;

import com.openshelf.domain.shared.Preconditions;

/**
 * Immutable notification addressed to a logical user ID.
 *
 * <p>A real delivery adapter must resolve the ID to an email address, phone or device token.
 *
 * @param channel selected delivery channel
 * @param recipientId logical user ID, not a phone number or device token
 * @param message notification body
 */
public record Notification(NotificationChannel channel, String recipientId, String message) {

  /** Creates a validated notification snapshot. */
  public Notification {
    Preconditions.checkNotNull(channel, "Notification channel cannot be null");
    Preconditions.requireNotBlank(recipientId, "Recipient ID cannot be null or blank");
    Preconditions.requireNotBlank(message, "Notification message cannot be null or blank");
  }
}
