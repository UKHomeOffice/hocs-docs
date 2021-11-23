package uk.gov.digital.ho.hocs.document.dto.camel;

import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.util.UUID;

@java.lang.SuppressWarnings("squid:S1068")
public class UpdateDocumentRequest {
    private UUID uuid;
    private DocumentStatus status;
    private String fileLink;
    private String pdfLink;

    public UpdateDocumentRequest(UUID uuid, DocumentStatus status, String fileLink, String pdfLink) {
        this.uuid = uuid;
        this.status = status;
        this.fileLink = fileLink;
        this.pdfLink = pdfLink;
    }

    public UUID getUuid() {
        return uuid;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getFileLink() {
        return fileLink;
    }

    public String getPdfLink() {
        return pdfLink;
    }
}
