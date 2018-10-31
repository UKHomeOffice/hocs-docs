package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.digital.ho.hocs.document.model.DocumentType;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class CreateDocumentRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private DocumentType type;

    @JsonProperty("externalReferenceUUID")
    private UUID externalReferenceUUID;

}