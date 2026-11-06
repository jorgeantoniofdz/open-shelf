package com.openshelf.domain.notification;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NotificationTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void constructor_rejectsInvalidRecipientOrMessage(String value) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Notification(NotificationChannel.EMAIL, value, "message"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Notification(NotificationChannel.EMAIL, "m-1", value));
  }

  @Test
  void constructor_rejectsNullChannel() {
    assertThrows(IllegalArgumentException.class, () -> new Notification(null, "m-1", "message"));
  }
}
