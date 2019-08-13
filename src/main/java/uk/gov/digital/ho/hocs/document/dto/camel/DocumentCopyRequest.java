package uk.gov.digital.ho.hocs.document.dto.camel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DocumentCopyRequest {
    private final String fileLink;
    private final String externalReferenceUUID;
    private final String fileType;
    private final String convertTo;
}