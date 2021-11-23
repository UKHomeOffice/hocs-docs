package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class CreateDocumentRequest {
    public CreateDocumentRequest(
            UUID externalReferenceUUID,
            String name,
            String fileLink,
            String type,
            String convertTo) {
        this.name = name;
        this.type = type;
        this.fileLink = fileLink;
        this.externalReferenceUUID = externalReferenceUUID;
        this.convertTo = convertTo;
    }
    @JsonProperty("externalReferenceUUID")
    private UUID externalReferenceUUID;

    @JsonProperty("actionDataItemUuid")
    private UUID actionDataItemUuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("fileLink")
    private String fileLink;

    @JsonProperty("type")
    private String type;

    @JsonProperty("convertTo")
    private String convertTo = "PDF";
}
