package uk.gov.digital.ho.hocs.document.dto;


public class Document {

    private final String filename;
    private final String originalFilename;
    private final byte[] data;
    private final String fileType;
    private final String mimeType;


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

    public String getMimeType() { return mimeType; }

    public Document(String filename, String originalFilename, byte[] data, String fileType, String mimeType) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.data = data;
        this.fileType = fileType;
        this.mimeType = mimeType;
    }
}
