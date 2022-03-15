package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import static uk.gov.digital.ho.hocs.document.application.LogEvent.DOCUMENT_CREATION_FAILURE;
import static uk.gov.digital.ho.hocs.document.application.LogEvent.DOCUMENT_UDPATE_FAILURE;

@Entity
@Table(name = "document_data")
@Where(clause = "not deleted")
@NoArgsConstructor
public class DocumentData implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "uuid")
    @Getter
    private UUID uuid;

    @Column(name = "external_reference_uuid")
    @Getter
    private UUID externalReferenceUUID;

    @Column(name = "type")
    @Getter
    private String type;

    @Column(name = "display_name")
    @Getter
    private String displayName;

    @Column(name = "file_link")
    @Getter
    private String fileLink;

    @Column(name = "pdf_link")
    @Getter
    private String pdfLink;

    @Column(name = "status")
    private String status = DocumentStatus.PENDING.toString();

    @Column(name = "created")
    @Getter
    private LocalDateTime created = LocalDateTime.now();

    @Column(name = "updated")
    @Getter
    private LocalDateTime updated;

    @Column(name = "deleted")
    @Getter
    @Setter
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "action_data_item_uuid")
    @Getter
    @Setter
    private UUID actionDataItemUuid;

    @Column(name = "upload_owner")
    @Getter
    @Setter
    private UUID uploadOwnerUUID;

    public DocumentData(DocumentData documentData, UUID toReferenceUUID) {
        if (toReferenceUUID == null) {
            throw new ApplicationExceptions.EntityCreationException(String.format("Cannot copy document(%s, %s).", documentData.getUuid(), documentData.getDisplayName()), DOCUMENT_CREATION_FAILURE);
        }
        this.uuid = UUID.randomUUID();
        this.type = documentData.getType();
        this.displayName = documentData.getDisplayName();
        this.externalReferenceUUID = toReferenceUUID;
        this.actionDataItemUuid = documentData.getActionDataItemUuid();
        this.uploadOwnerUUID = documentData.getUploadOwnerUUID();
        this.fileLink = documentData.getFileLink();
        this.pdfLink = documentData.getPdfLink();
        this.status = documentData.getStatus().toString();
    }

    public DocumentData(UUID externalReferenceUUID, UUID actionDataItemUuid, String type, String displayName, UUID uploadOwnerUUID) {
        if (externalReferenceUUID == null || type == null || displayName == null) {
            throw new ApplicationExceptions.EntityCreationException(String.format("Cannot create DocumentData(%s, %s, %s).", externalReferenceUUID, type, displayName), DOCUMENT_CREATION_FAILURE);
        }
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.displayName = displayName;
        this.externalReferenceUUID = externalReferenceUUID;
        this.actionDataItemUuid = actionDataItemUuid;
        this.uploadOwnerUUID = uploadOwnerUUID;
    }

    public void update(String fileLink, String pdfLink, DocumentStatus status) {
        if (fileLink == null || status == null) {
            throw new ApplicationExceptions.EntityCreationException(String.format("Cannot call DocumentData.update(%s, %s, %s).", fileLink, pdfLink, status), DOCUMENT_UDPATE_FAILURE);
        }
        this.fileLink = fileLink;
        this.pdfLink = pdfLink;
        this.status = status.toString();
        this.updated = LocalDateTime.now();
    }

    public DocumentStatus getStatus() {
        return DocumentStatus.valueOf(this.status);
    }


}