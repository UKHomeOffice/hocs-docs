package uk.gov.digital.ho.hocs.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DocumentCopyRequest {
    private final String fileLink;
    private final String caseUUID;
    private final String fileType;
}