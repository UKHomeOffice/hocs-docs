package uk.gov.digital.ho.hocs.document.dto.camel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@java.lang.SuppressWarnings("squid:S1068")
@AllArgsConstructor
@Getter
public class ProcessDocumentRequest {

    @JsonProperty(value = "uuid", required = true)
    private final String uuid;

    @JsonProperty("fileLink")
    private final String fileLink;

    @JsonProperty("convertTo")
    private final String convertTo;
}
