package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentType;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class CreateManagedDocumentRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private ManagedDocumentType type;

    @JsonProperty("externalReferenceUUID")
    private UUID externalReferenceUUID;

}