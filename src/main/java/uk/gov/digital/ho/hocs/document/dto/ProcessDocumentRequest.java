package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;



public class ProcessDocumentRequest {


    private String caseUUID;


    private String fileLink;

    public String getCaseUUID() {
        return caseUUID;
    }

    public String getFileLink() {
        return fileLink;
    }

    @JsonCreator
    public ProcessDocumentRequest(@JsonProperty("caseUUID") String caseUUID,@JsonProperty("fileLink") String fileLink) {
        this.caseUUID = caseUUID;
        this.fileLink = fileLink;
    }
}
