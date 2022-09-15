package uk.gov.digital.ho.hocs.document.dto.camel;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@java.lang.SuppressWarnings("squid:S1068")
@AllArgsConstructor
@Getter
public class DocumentConversionRequest {

    private UUID documentUUID;

    private final String fileLink;

    private final String externalReferenceUUID;

    private final String fileType;

    private final String convertTo;

}