DROP TABLE IF EXISTS managed_document_data cascade;

CREATE TABLE IF NOT EXISTS managed_document_data
(
  id                         BIGSERIAL PRIMARY KEY,
  uuid                       UUID      NOT NULL,
  external_reference_uuid    UUID      NOT NULL,
  type                       TEXT      NOT NULL,
  display_name               TEXT      NOT NULL,
  orig_link                  TEXT,
  status                     TEXT      NOT NULL,
  created                    TIMESTAMP NOT NULL,
  updated                    TIMESTAMP,
  expires                    TIMESTAMP,
  deleted                    BOOLEAN   NOT NULL DEFAULT FALSE,

  CONSTRAINT managed_document_uuid_idempotent UNIQUE (uuid)
);

CREATE INDEX  idx_manged_document_data_uuid
  ON managed_document_data (uuid);

CREATE INDEX  idx_manged_document_data_type
  ON managed_document_data (type);

CREATE INDEX  idx_manged_document_data_external_reference_uuid
  ON managed_document_data (external_reference_uuid);

CREATE INDEX  idx_manged_document_data_deleted
  ON managed_document_data (deleted);