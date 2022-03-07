package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.client.auditclient.AuditClient;
import uk.gov.digital.ho.hocs.document.client.documentclient.DocumentClient;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.repository.DocumentRepository;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.value;
import static uk.gov.digital.ho.hocs.document.application.LogEvent.*;

@Service
@Slf4j
public class DocumentDataService {

    private final DocumentRepository documentRepository;
    private final S3DocumentService s3DocumentService;
    private final AuditClient auditClient;
    private final DocumentClient documentClient;
    private final RequestData requestData;


    @Autowired
    public DocumentDataService(DocumentRepository documentRepository, S3DocumentService s3DocumentService, AuditClient auditClient, DocumentClient documentClient, RequestData requestData){
        this.documentRepository = documentRepository;
        this.s3DocumentService = s3DocumentService;
        this.auditClient = auditClient;
        this.documentClient = documentClient;
        this.requestData = requestData;
    }

    public DocumentData createDocument(CreateDocumentRequest request) {
        log.debug("Creating Document: {}, external Reference  UUID: {}",
                request.getName(), request.getExternalReferenceUUID());

        String convertTo = (request.getConvertTo() != null) ? request.getConvertTo() : "PDF";

//        UUID userID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(
                request.getExternalReferenceUUID(),
                request.getActionDataItemUuid(),
                request.getType(),
                request.getName(),
                UUID.fromString(requestData.userId())
//                userID
        );

        documentRepository.save(documentData);
        documentClient.processDocument(documentData.getUuid(), request.getFileLink(), convertTo);
        auditClient.createDocumentAudit(documentData);

        log.info("Created Document: {}, external Reference UUID: {}", documentData.getUuid(), documentData.getExternalReferenceUUID(), value(EVENT, DOCUMENT_CREATED));

        return documentData;
    }

    public void updateDocument(UUID documentUUID, DocumentStatus status, String fileLink, String pdfLink) {
        log.debug("Updating Document: {}", documentUUID);
        DocumentData documentData = getDocumentData(documentUUID);
        documentData.update(fileLink, pdfLink, status);
        documentRepository.save(documentData);
        auditClient.updateDocumentAudit(documentData);
        log.info("Updated Document: {} to status {}", documentData.getUuid(), documentData.getStatus(), value(EVENT, DOCUMENT_UPDATED));
    }

    public DocumentData getDocumentData(String documentUUID) {
        return getDocumentData(UUID.fromString(documentUUID));
    }

    public DocumentData getDocumentData(UUID documentUUID) {
        DocumentData documentData = documentRepository.findByUuid(documentUUID);
        if (documentData != null) {
            return documentData;
        } else {
            throw new ApplicationExceptions.EntityNotFoundException(String.format("Document UUID: %s not found!", documentUUID), DOCUMENT_NOT_FOUND);
        }
    }

    public Set<DocumentData> getDocumentsByReference(UUID externalReferenceUUID) {
        return documentRepository.findAllByExternalReferenceUUID(externalReferenceUUID);
    }
    public Set<DocumentData> getDocumentsByReferenceAndActionDataUuid(
            UUID externalReferenceUUID, UUID actionDataUuid, String type) {
        return documentRepository
                .findAllByExternalReferenceUUIDAndActionDataItemUuidAndType(externalReferenceUUID, actionDataUuid, type);
    }

    public Set<DocumentData> getDocumentsByReferenceForType(UUID externalReferenceUUID, String type) {
        return documentRepository.findAllByExternalReferenceUUIDAndType(externalReferenceUUID,type);
    }

    public void deleteDocument(UUID documentUUID) {
        DocumentData documentData = documentRepository.findByUuid(documentUUID);
        documentData.setDeleted(true);
        documentRepository.save(documentData);
        auditClient.deleteDocumentAudit(documentData);
        log.info("Set Document to deleted: {}", documentUUID, value(EVENT, DOCUMENT_DELETED));
    }

    public S3Document getDocumentFile(UUID documentUuid) {
        return getDocumentFromS3(documentUuid, DocumentType.ORIGINAL);
    }

    public S3Document getDocumentPdf(UUID documentUuid) {
        return getDocumentFromS3(documentUuid, DocumentType.PDF);
    }

    private S3Document getDocumentFromS3(UUID documentUuid, DocumentType documentType) {
        DocumentData documentData = getDocumentData(documentUuid);

        String fileLink = documentType.equals(DocumentType.PDF) ? documentData.getPdfLink() : documentData.getFileLink();

        if (!StringUtils.hasText(fileLink)) {
            return new S3Document(null, documentData.getDisplayName(), new byte[0],
                    null, null);
        }

        try {
            return s3DocumentService.getFileFromTrustedS3(fileLink);
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException(e.getMessage(), DOCUMENT_NOT_FOUND);
        }
    }

    private enum DocumentType {
        ORIGINAL,
        PDF
    }
}
