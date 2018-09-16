package uk.gov.digital.ho.hocs.document.model;


public class UploadDocument {

    private String filename;

    private byte[] data;

    private String caseUUID;

    public String getCaseUUID() {
        return caseUUID;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public UploadDocument(String filename, byte[] data, String caseUUID) {
        this.filename = filename;
        this.data = data;
        this.caseUUID = caseUUID;
    }

    public static UploadDocument from(String caseUUID, Document document) {
        return new UploadDocument(document.getOriginalFilename(), document.getData(),caseUUID);
    }
}
