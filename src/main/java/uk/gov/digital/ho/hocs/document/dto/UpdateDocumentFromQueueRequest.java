package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class UpdateDocumentFromQueueRequest {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("case")
    private String caseUUID;

    @JsonProperty("pdfLink")
    private String pdfLink;

    @JsonProperty("originalFileLink")
    private String originalFileLink;

    public UpdateDocumentFromQueueRequest(String uuid, String caseUUID, String pdfLink, String originalFileLink) {
        this.uuid = uuid;
        this.caseUUID = caseUUID;
        this.pdfLink = pdfLink;
        this.originalFileLink = originalFileLink;
    }
}
