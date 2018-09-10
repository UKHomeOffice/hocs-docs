package uk.gov.digital.ho.hocs.document.dto;

public class DocumentConversionRequest {


    private final String fileLink;
    private final String caseUUID;
    private final String fileType;

    public DocumentConversionRequest(String fileLink, String caseUUID, String fileType) {
        this.fileLink = fileLink;
        this.caseUUID = caseUUID;
        this.fileType = fileType;
    }

    public String getFileLink() {
        return fileLink;
    }

    public String getCaseUUID() {
        return caseUUID;
    }

    public String getFileType() {
        return fileType;
    }
}
