package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "managed_document_data")
@Where(clause = "not deleted")
@NoArgsConstructor
public class ManagedDocumentData implements Serializable {

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
    private String type;

    @Column(name = "display_name")
    @Getter
    private String displayName;

    @Column(name = "file_link")
    @Getter
    private String fileLink;

    @Column(name = "status")
    private String status = ManagedDocumentStatus.PENDING.toString();

    @Column(name = "created")
    @Getter
    private LocalDateTime created = LocalDateTime.now();

    @Column(name = "updated")
    @Getter
    private LocalDateTime updated;

    @Column(name = "expires")
    @Getter
    private LocalDate expires;

    @Column(name = "deleted")
    @Getter
    @Setter
    private Boolean deleted = Boolean.FALSE;

    public ManagedDocumentData(UUID externalReferenceUUID, ManagedDocumentType type, String displayName) {
        if (externalReferenceUUID == null || type == null || displayName == null) {
            throw new ApplicationExceptions.EntityCreationException("Cannot create ManagedDocumentData(%s, %s, %s).", externalReferenceUUID, type, displayName);
        }
        this.uuid = UUID.randomUUID();
        this.type = type.toString();
        this.displayName = displayName;
        this.externalReferenceUUID = externalReferenceUUID;
    }

    public void update(String fileLink, ManagedDocumentStatus status) {
        if (fileLink == null || status == null) {
            throw new ApplicationExceptions.EntityCreationException("Cannot call ManagedDocumentData.update(%s, %s).", fileLink, status);
        }
        this.fileLink = fileLink;
        this.status = status.toString();
        this.updated = LocalDateTime.now();
    }

    public ManagedDocumentType getType() {
        return ManagedDocumentType.valueOf(this.type);
    }

    public ManagedDocumentStatus getStatus() {
        return this.expires.isBefore(LocalDate.now()) ? ManagedDocumentStatus.valueOf(status) : ManagedDocumentStatus.EXPIRED ;
    }

}