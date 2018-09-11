package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;


@Getter
public class ProcessDocumentRequest {

    private String uuid;

    private String caseUUID;

    private String fileLink;

    @JsonCreator
    public ProcessDocumentRequest(@JsonProperty("uuid") String uuid,@JsonProperty("caseUUID") String caseUUID,@JsonProperty("fileLink") String fileLink) {
        this.uuid = uuid;
        this.caseUUID = caseUUID;
        this.fileLink = fileLink;
    }
}
