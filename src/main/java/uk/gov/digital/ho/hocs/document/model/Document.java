package uk.gov.digital.ho.hocs.document.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Document {

    private final String filename;
    private final String originalFilename;
    private final byte[] data;
    private final String fileType;
    private final String mimeType;
}
