package uk.gov.digital.ho.hocs.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


public class Document {

    private String filename;
    private byte[] data;
    private String md5;

    public String getMd5() {
        return md5;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }


    public Document(String filename, byte[] data, String md5) {
        this.filename = filename;
        this.data = data;
        this.md5 = md5;
    }

}
