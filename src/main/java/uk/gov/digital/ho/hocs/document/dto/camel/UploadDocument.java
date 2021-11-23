package uk.gov.digital.ho.hocs.document.dto.camel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@java.lang.SuppressWarnings("squid:S1068")
@AllArgsConstructor
@Getter
public class UploadDocument {
    private String filename;
    private byte[] data;
    private String externalReferenceUUID;
    private String originalFileName;
}
