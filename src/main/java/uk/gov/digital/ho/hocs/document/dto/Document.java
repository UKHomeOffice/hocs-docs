package uk.gov.digital.ho.hocs.document.dto;


public class Document {

    private String filename;
    private String originalFilename;
    private byte[] data;
    private String fileType;

    public String getFileType() {
        return fileType;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public String getOriginalFilename() { return originalFilename; }

    public Document(String filename, String originalFilename, byte[] data, String fileType) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.data = data;
        this.fileType = fileType;
    }
}
