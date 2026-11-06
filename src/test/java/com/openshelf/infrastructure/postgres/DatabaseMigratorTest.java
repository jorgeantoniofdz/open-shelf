package com.openshelf.infrastructure.postgres;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DatabaseMigratorTest {

  @Test
  void migrate_rejectsMissingDataSource() {
    assertThrows(IllegalArgumentException.class, () -> DatabaseMigrator.migrate(null));
  }
}
