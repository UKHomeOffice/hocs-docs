package uk.gov.digital.ho.hocs.document.dto;


public class UploadDocument {

    private String filename;
    private byte[] data;

    public String getCaseUUID() {
        return caseUUID;
    }

    private String caseUUID;

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
}
