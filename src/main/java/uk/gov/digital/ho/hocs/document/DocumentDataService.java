package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.client.auditclient.AuditClient;
import uk.gov.digital.ho.hocs.document.client.documentclient.DocumentClient;
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

    @Autowired
    public DocumentDataService(DocumentRepository documentRepository, S3DocumentService s3DocumentService, AuditClient auditClient, DocumentClient documentClient){
        this.documentRepository = documentRepository;
        this.s3DocumentService = s3DocumentService;
        this.auditClient = auditClient;
        this.documentClient = documentClient;
    }

    public DocumentData createDocument(UUID externalReferenceUUID, String displayName, String fileName, String type, String convertTo) {
        log.debug("Creating Document: {}, external Reference  UUID: {}", displayName, externalReferenceUUID);
        DocumentData documentData = new DocumentData(externalReferenceUUID, type, displayName);
        documentRepository.save(documentData);
        documentClient.processDocument(documentData.getUuid(), fileName, convertTo);
        auditClient.createDocumentAudit(documentData);
        log.info("Created Document: {}, external Reference UUID: {}", documentData.getUuid(), documentData.getExternalReferenceUUID(), value(EVENT, DOCUMENT_CREATED));
        return documentData;
    }

    public void updateDocument(UUID documentUUID, DocumentStatus status, String fileLink, String pdfLink) {
        log.debug("Updating Document: {}", documentUUID);
        DocumentData documentData = getDocumentData(documentUUID);
        documentData.update(fileLink, pdfLink, status);
        auditClient.updateDocumentAudit(documentData);
        documentRepository.save(documentData);
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

    public S3Document getDocumentFile(UUID documentUUID) {
        DocumentData documentData = getDocumentData(documentUUID);
        try {
            log.debug("Getting Document File: {}", documentUUID);
            return s3DocumentService.getFileFromTrustedS3(documentData.getFileLink());
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException(e.getMessage(),DOCUMENT_NOT_FOUND);
        }
    }

    public S3Document getDocumentPdf(UUID documentUUID) {
        DocumentData documentData = getDocumentData(documentUUID);
        try{
            log.debug("Getting Document PDF: {}", documentUUID);
            return s3DocumentService.getFileFromTrustedS3(documentData.getPdfLink());
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException(e.getMessage(), DOCUMENT_NOT_FOUND);
        }
    }
}