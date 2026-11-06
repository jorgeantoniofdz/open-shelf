-- Initial book schema, versioned and validated by Flyway.
-- Author details belong to each book snapshot, matching the in-memory repository contract.
CREATE TABLE books (
    isbn TEXT PRIMARY KEY,
    title VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    publication_date DATE NOT NULL,
    page_count INTEGER NOT NULL CHECK (page_count > 0),
    language TEXT NOT NULL,
    author_first_name VARCHAR(50) NOT NULL,
    author_last_name VARCHAR(50) NOT NULL,
    author_birth_date DATE NOT NULL,
    author_nationality TEXT NOT NULL,
    author_biography TEXT NOT NULL,
    categories TEXT[] NOT NULL DEFAULT '{}'
);

CREATE INDEX books_author_identity_idx
    ON books (author_first_name, author_last_name, author_birth_date);
