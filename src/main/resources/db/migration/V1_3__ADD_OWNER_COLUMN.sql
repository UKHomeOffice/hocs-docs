SET search_path TO document;

ALTER TABLE document_data
    ADD COLUMN upload_owner UUID
        DEFAULT null;

