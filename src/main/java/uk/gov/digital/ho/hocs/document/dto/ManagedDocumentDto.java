package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.digital.ho.hocs.document.model.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Slf4j
public class ManagedDocumentDto {

    @JsonProperty("UUID")
    private UUID uuid;

    @JsonProperty("externalReferenceUUID")
    private UUID externalReferenceUUID;

    @JsonProperty("type")
    private ManagedDocumentType type;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("fileLink")
    private String fileLink;

    @JsonProperty("status")
    private ManagedDocumentStatus status;

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("updated")
    private LocalDateTime updated;

    @JsonProperty("expires")
    private LocalDate expires;

    @JsonProperty("deleted")
    private Boolean deleted;

    public static ManagedDocumentDto from(ManagedDocumentData documentData) {

        return new ManagedDocumentDto(
                documentData.getUuid(),
                documentData.getExternalReferenceUUID(),
                documentData.getType(),
                documentData.getDisplayName(),
                urlEncode(documentData.getFileLink()),
                documentData.getStatus(),
                documentData.getCreated(),
                documentData.getUpdated(),
                documentData.getExpires(),
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