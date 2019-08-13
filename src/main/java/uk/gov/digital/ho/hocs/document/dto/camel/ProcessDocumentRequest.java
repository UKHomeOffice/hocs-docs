package uk.gov.digital.ho.hocs.document.dto.camel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class ProcessDocumentRequest {

    @JsonProperty("uuid")
    private UUID uuid;

    @JsonProperty("fileLink")
    private String fileLink;

    @JsonProperty("convertToPdf")
    private boolean convertToPdf;

    @JsonProperty("externalReferenceUUID")
    private UUID externalReferenceUUID;
}
