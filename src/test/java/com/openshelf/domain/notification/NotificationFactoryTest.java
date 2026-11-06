package com.openshelf.domain.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openshelf.domain.user.Administrator;
import com.openshelf.domain.user.Librarian;
import com.openshelf.domain.user.Member;
import com.openshelf.domain.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NotificationFactoryTest {

  static Stream<Arguments> channels() {
    Function<NotificationSender, NotificationService> email = NotificationFactory::email;
    Function<NotificationSender, NotificationService> sms = NotificationFactory::sms;
    Function<NotificationSender, NotificationService> push = NotificationFactory::push;
    return Stream.of(
        Arguments.of(email, NotificationChannel.EMAIL),
        Arguments.of(sms, NotificationChannel.SMS),
        Arguments.of(push, NotificationChannel.PUSH));
  }

  @ParameterizedTest
  @MethodSource("channels")
  void notify_recordsCorrectChannelRecipientAndMessageForEveryRole(
      Function<NotificationSender, NotificationService> factory, NotificationChannel channel) {
    List<Notification> recorded = new ArrayList<>();
    NotificationService service = factory.apply(recorded::add);
    List<User> users =
        List.of(
            new Member("member", "Member", "member@example.com"),
            new Librarian("librarian", "Librarian", "librarian@example.com"),
            new Administrator("admin", "Administrator", "admin@example.com"));
    String message = "Libro devuelto: programación — edición 2.\nPodés consultar tu reserva.";
    for (User user : users) {
      service.notify(user, message);
    }

    assertEquals(
        users.stream().map(user -> new Notification(channel, user.getId(), message)).toList(),
        recorded);
  }

  @ParameterizedTest
  @MethodSource("channels")
  void invalidInputs_doNotReachSender(
      Function<NotificationSender, NotificationService> factory, NotificationChannel channel) {
    List<Notification> recorded = new ArrayList<>();
    NotificationService service = factory.apply(recorded::add);
    User user = new Member("m-1", "Member", "member@example.com");
    assertThrows(IllegalArgumentException.class, () -> factory.apply(null));
    assertThrows(IllegalArgumentException.class, () -> service.notify(null, "message"));
    assertThrows(IllegalArgumentException.class, () -> service.notify(user, null));
    for (String message : List.of("", " ", "\t\n")) {
      assertThrows(IllegalArgumentException.class, () -> service.notify(user, message));
    }
    assertTrue(recorded.isEmpty());
  }

  @ParameterizedTest
  @MethodSource("channels")
  void senderFailure_propagatesWithoutRetry(
      Function<NotificationSender, NotificationService> factory, NotificationChannel channel) {
    List<Notification> attempts = new ArrayList<>();
    IllegalStateException failure = new IllegalStateException("Delivery failed");
    NotificationService service =
        factory.apply(
            notification -> {
              attempts.add(notification);
              throw failure;
            });
    User user = new Member("m-1", "Member", "member@example.com");

    assertSame(
        failure, assertThrows(IllegalStateException.class, () -> service.notify(user, "message")));
    assertEquals(List.of(new Notification(channel, user.getId(), "message")), attempts);
  }

  @Test
  void factories_keepInjectedSendersIndependent() {
    List<Notification> first = new ArrayList<>();
    List<Notification> second = new ArrayList<>();
    NotificationService firstService = NotificationFactory.email(first::add);
    NotificationService secondService = NotificationFactory.email(second::add);
    User user = new Member("m-1", "Member", "member@example.com");
    firstService.notify(user, "First");
    secondService.notify(user, "Second");

    assertEquals(List.of(new Notification(NotificationChannel.EMAIL, "m-1", "First")), first);
    assertEquals(List.of(new Notification(NotificationChannel.EMAIL, "m-1", "Second")), second);
  }

  @Test
  void defaultFactories_createConcreteSimulationChannels() {
    assertInstanceOf(EmailNotificationService.class, NotificationFactory.email());
    assertInstanceOf(SmsNotificationService.class, NotificationFactory.sms());
    assertInstanceOf(PushNotificationService.class, NotificationFactory.push());
  }
}
