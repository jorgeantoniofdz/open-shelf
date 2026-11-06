package com.openshelf.domain.book;

/**
 * Classifies books into subject areas.
 *
 * <p>This enum is the single source of truth for all valid book categories. Add new categories
 * here; all consuming code picks them up automatically via the type system.
 */
public enum Category {
  PROGRAMMING,
  SCIENCE,
  HISTORY,
  FANTASY,
  FINANCE
}
