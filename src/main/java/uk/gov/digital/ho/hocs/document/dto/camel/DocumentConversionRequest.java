package uk.gov.digital.ho.hocs.document.dto.camel;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class DocumentConversionRequest {
    private UUID documentUUID;
    private final String fileLink;
    private final String caseUUID;
    private final String fileType;
}