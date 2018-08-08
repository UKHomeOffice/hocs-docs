package uk.gov.digital.ho.hocs.document.dto;

import com.amazonaws.services.simplesystemsmanagement.model.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UpdateDocumentFromQueueRequest {

    @JsonProperty("uuid")
    private UUID uuid;

    @JsonProperty("case")
    private UUID caseUUID;

    @JsonProperty("fileLink")
    private String fileLink;

    @JsonProperty("pdfLink")
    private String pdfLink;

    @JsonProperty("status")
    private DocumentStatus status;

}
