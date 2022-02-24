package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Slf4j
public class DocumentDto {

    @JsonProperty("uuid")
    private UUID uuid;

    @JsonProperty("externalReferenceUUID")
    private UUID externalReferenceUUID;

    @JsonProperty("type")
    private String type;

    @JsonProperty("displayName")
    private String displayName;

    private UUID uploadOwnerUUID;

    @JsonProperty("status")
    private DocumentStatus status;

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("updated")
    private LocalDateTime updated;

    @JsonProperty("deleted")
    private Boolean deleted;

    @JsonProperty("hasPdf")
    private Boolean hasPdf;

    public static DocumentDto from(DocumentData documentData) {

        return new DocumentDto(
                documentData.getUuid(),
                documentData.getExternalReferenceUUID(),
                documentData.getType(),
                documentData.getDisplayName(),
                documentData.getUploadOwnerUUID(),
                documentData.getStatus(),
                documentData.getCreated(),
                documentData.getUpdated(),
                documentData.getDeleted(),
                documentData.getPdfLink() != null && !documentData.getPdfLink().isEmpty()
        );
    }
}