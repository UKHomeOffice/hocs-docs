ALTER TABLE document_data ADD COLUMN deleted_on timestamp DEFAULT null;

ALTER TABLE document_data RENAME COLUMN created TO created_on;
ALTER TABLE document_data RENAME COLUMN updated TO updated_on;
