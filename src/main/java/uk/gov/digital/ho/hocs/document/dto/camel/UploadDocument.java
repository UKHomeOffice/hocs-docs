package uk.gov.digital.ho.hocs.document.dto.camel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UploadDocument {
    private String filename;
    private byte[] data;
    private String caseUUID;
    private String OriginalFileName;
}
