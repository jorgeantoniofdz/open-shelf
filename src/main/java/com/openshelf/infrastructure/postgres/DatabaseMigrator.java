package com.openshelf.infrastructure.postgres;

import com.openshelf.domain.shared.Preconditions;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * Applies versioned migrations to the data source's default schema before repositories are used.
 */
public final class DatabaseMigrator {

  private DatabaseMigrator() {}

  /**
   * Validates migration history and applies pending migrations.
   *
   * <p>Existing nonempty schemas without Flyway history require explicit adoption. No automatic
   * baseline or cleanup is performed. Flyway failures propagate to the caller.
   *
   * @param dataSource connection source pointing to the intended default schema
   * @return number of migrations applied by this call
   */
  public static int migrate(DataSource dataSource) {
    Preconditions.checkNotNull(dataSource, "Data source cannot be null");
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .baselineOnMigrate(false)
        .validateMigrationNaming(true)
        .load()
        .migrate()
        .migrationsExecuted;
  }
}
