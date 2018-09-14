package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "managed_document_data")
@NoArgsConstructor
public class ManagedDocumentData implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "type")
    private String type;

    @Column(name = "display_name")
    @Getter
    private String name;

    @Column(name = "orig_link")
    @Getter
    private String fileLink;

    @Column(name = "pdf_link")
    @Getter
    private String pdfLink;

    @Column(name = "status")
    private String status = DocumentStatus.PENDING.toString();

    @Column(name = "uuid")
    @Getter
    private UUID uuid;

    @Column(name = "created")
    @Getter
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "expires")
    @Getter
    private LocalDate expires;

    @Column(name = "deleted")
    @Getter
    @Setter
    private Boolean deleted = Boolean.FALSE;

    public ManagedDocumentData(ManagedDocumentType type, String name) {
        if (type == null || name == null) {
            throw new ApplicationExceptions.EntityCreationException("Cannot create ManagedDocumentData(%s, %s, %s).", type, name);
        }
        this.uuid = UUID.randomUUID();
        this.type = type.toString();
        this.name = name;
    }

    public void update(String fileLink, String pdfLink) {
        if (fileLink == null || status == null) {
            throw new ApplicationExceptions.EntityCreationException("Cannot call ManagedDocumentData.update(%s, %s, %s).", fileLink, pdfLink, status);
        }
        this.fileLink = fileLink;
        this.pdfLink = pdfLink;
    }

    public ManagedDocumentType getType() {
        return ManagedDocumentType.valueOf(this.type);
    }

    public ManagedDocumentStatus getStatus() {
        return this.expires.isBefore(LocalDate.now()) ? ManagedDocumentStatus.ACTIVE : ManagedDocumentStatus.EXPIRED ;
    }

}