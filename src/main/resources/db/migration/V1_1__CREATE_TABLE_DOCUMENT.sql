DROP TABLE IF EXISTS document_data cascade;

CREATE TABLE IF NOT EXISTS document_data
(
  id           BIGSERIAL PRIMARY KEY,
  uuid         UUID      NOT NULL,
  case_uuid    UUID      NOT NULL,
  type         TEXT      NOT NULL,
  display_name TEXT      NOT NULL,
  orig_link    TEXT,
  pdf_link     TEXT,
  status       TEXT      NOT NULL,
  created      TIMESTAMP NOT NULL,
  updated      TIMESTAMP,
  deleted      BOOLEAN   NOT NULL DEFAULT FALSE,

  CONSTRAINT document_uuid_idempotent UNIQUE (uuid)
);

CREATE INDEX  idx_document_data_uuid
  ON document_data (uuid);

CREATE INDEX  idx_document_data_case_uuid
  ON document_data (case_uuid);