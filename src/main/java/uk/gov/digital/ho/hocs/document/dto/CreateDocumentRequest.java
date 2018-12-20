package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.digital.ho.hocs.document.model.DocumentType;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class CreateDocumentRequest {

    @JsonProperty(value="name", required=true)
    private String name;

    @JsonProperty(value="type", required=true)
    private DocumentType type;

    @JsonProperty(value="externalReferenceUUID", required=true)
    private UUID externalReferenceUUID;

}