package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.model.DocumentType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Slf4j
public class Document {

    @JsonProperty("type")
    private DocumentType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("s3_orig_link")
    private String fileLink;

    @JsonProperty("s3_pdf_link")
    private String pdfLink;

    @JsonProperty("status")
    private DocumentStatus status;

    @JsonProperty("document_uuid")
    private UUID uuid;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("deleted")
    private Boolean deleted;

    public static Document from(DocumentData documentData) {


        return new Document(
                documentData.getType(),
                documentData.getName(),
                urlEncode(documentData.getFileLink()),
                urlEncode(documentData.getPdfLink()),
                documentData.getStatus(),
                documentData.getUuid(),
                documentData.getTimestamp(),
                documentData.getDeleted()
        );
    }

    private static String urlEncode(String value) {

        if(value != null) {
            try {
                return URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage());
            }
        }
        return null;
    }
}