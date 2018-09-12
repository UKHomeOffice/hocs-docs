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

@Entity
@Table(name = "document_data")
@Where(clause = "not deleted")
@NoArgsConstructor
public class DocumentData implements Serializable {

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

    @Column(name = "case_uuid")
    @Getter
    private UUID caseUUID;

    @Column(name = "created")
    @Getter
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "deleted")
    @Getter
    @Setter
    private Boolean deleted = Boolean.FALSE;

    public DocumentData(UUID caseUUID, DocumentType type, String name) {
        if (caseUUID == null || type == null || name == null) {
            throw new ApplicationExceptions.EntityCreationException("Cannot create DocumentData(%s, %s, %s).", caseUUID, type, name);
        }
        this.uuid = UUID.randomUUID();
        this.type = type.toString();
        this.name = name;
        this.caseUUID = caseUUID;
    }

    public void update(String fileLink, String pdfLink, DocumentStatus status) {
        if (fileLink == null || status == null) {
            throw new ApplicationExceptions.EntityCreationException("Cannot call DocumentData.update(%s, %s, %s).", fileLink, pdfLink, status);
        }
        this.fileLink = fileLink;
        this.pdfLink = pdfLink;
        this.status = status.toString();
    }

    public DocumentType getType() {
        return DocumentType.valueOf(this.type);
    }

    public DocumentStatus getStatus() {
        return DocumentStatus.valueOf(this.status);
    }


}