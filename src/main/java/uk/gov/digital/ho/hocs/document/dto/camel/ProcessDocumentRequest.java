package uk.gov.digital.ho.hocs.document.dto.camel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProcessDocumentRequest {

    @JsonProperty("uuid")
    private final String uuid;
    @JsonProperty("caseUUID")
    private final String caseUUID;
    @JsonProperty("fileLink")
    private final String fileLink;

}
