package com.openshelf.domain.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MemberTest {

  private Member validMember() {
    return new Member("123", "John Doe", "john.doe@example.com");
  }

  // -------------------- happy path --------------------

  @Test
  void constructor_validIdNameEmail_getRoleReturnsMember() {
    Member member = validMember();
    assertEquals(UserRole.MEMBER, member.getRole());
  }

  // -------------------- id --------------------

  @Test
  void constructor_idIsNull_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> new Member(null, "John Doe", "john.doe@example.com"));
  }

  @Test
  void constructor_idIsBlank_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Member("   ", "John Doe", "john.doe@example.com"));
  }

  @Test
  void constructor_idWithWhitespace_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Member("123 456", "John Doe", "john.doe@example.com"));
  }

  // -------------------- name --------------------

  @Test
  void constructor_nameIsNull_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> new Member("123", null, "john.doe@example.com"));
  }

  @Test
  void constructor_nameIsBlank_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> new Member("123", "   ", "john.doe@example.com"));
  }

  @Test
  void constructor_nameExceedsMaxLength_throwsIllegalArgumentException() {
    String longName = "A".repeat(User.MAX_NAME_LENGTH + 1);
    assertThrows(
        IllegalArgumentException.class, () -> new Member("123", longName, "john.doe@example.com"));
  }

  @Test
  void constructor_nameExactlyMaxLength_doesNotThrow() {
    String maxLengthName = "A".repeat(User.MAX_NAME_LENGTH);
    assertDoesNotThrow(() -> new Member("123", maxLengthName, "john.doe@example.com"));
  }

  // -------------------- email --------------------

  @Test
  void constructor_emailIsNull_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Member("123", "John Doe", null));
  }

  @Test
  void constructor_validEmail_doesNotThrow() {
    assertDoesNotThrow(() -> new Member("123", "John Doe", "john.doe@example.com"));
  }
}
