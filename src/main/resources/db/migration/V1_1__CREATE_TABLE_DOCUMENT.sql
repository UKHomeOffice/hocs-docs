DROP TABLE IF EXISTS document_data cascade;

CREATE TABLE IF NOT EXISTS document_data
(
  id                         BIGSERIAL PRIMARY KEY,
  uuid                       UUID      NOT NULL,
  external_reference_uuid    UUID      NOT NULL,
  type                       TEXT      NOT NULL,
  display_name               TEXT      NOT NULL,
  file_link                  TEXT,
  pdf_link                   TEXT,
  status                     TEXT      NOT NULL,
  created                    TIMESTAMP NOT NULL,
  updated                    TIMESTAMP,
  deleted                    BOOLEAN   NOT NULL DEFAULT FALSE,

  CONSTRAINT document_uuid_idempotent UNIQUE (uuid)
);

CREATE INDEX  idx_document_data_uuid
  ON document_data (uuid, deleted);

CREATE INDEX  idx_document_data_external_reference_uuid
  ON document_data (external_reference_uuid, deleted);