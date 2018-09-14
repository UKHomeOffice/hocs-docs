DROP TABLE IF EXISTS managed_document_data cascade;

CREATE TABLE IF NOT EXISTS managed_document_data
(
  id           BIGSERIAL PRIMARY KEY,
  uuid         UUID      NOT NULL,
  type         TEXT      NOT NULL,
  display_name TEXT      NOT NULL,
  orig_link    TEXT,
  pdf_link     TEXT,
  status       TEXT      NOT NULL,
  created      TIMESTAMP NOT NULL,
  expires      TIMESTAMP,
  deleted      BOOLEAN   NOT NULL DEFAULT FALSE,

  CONSTRAINT managed_document_uuid_idempotent UNIQUE (uuid)
);

CREATE INDEX  idx_manged_document_data_uuid
  ON managed_document_uuid_idempotent (uuid);

