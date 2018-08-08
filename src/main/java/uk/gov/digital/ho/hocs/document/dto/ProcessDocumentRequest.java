package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
public class ProcessDocumentRequest {

    @JsonProperty("caseUUID")
    private String caseUUID;

    public String getCaseUUID() {
        return caseUUID;
    }

    public String getFileLink() {
        return fileLink;
    }

    @JsonProperty("fileLink")
    private String fileLink;
}
