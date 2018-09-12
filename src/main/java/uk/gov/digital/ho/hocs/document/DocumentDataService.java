package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.model.DocumentType;
import uk.gov.digital.ho.hocs.document.repository.DocumentRepository;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DocumentDataService {

    //private final AuditService auditService;
    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentDataService(DocumentRepository documentRepository){//,
                               //AuditService auditService) {
        this.documentRepository = documentRepository;
        //this.auditService = auditService;
    }

    @Transactional
    DocumentData createDocument(UUID caseUUID, String displayName, DocumentType type) {
        log.debug("Creating Document: {}, Case UUID: {}", displayName, caseUUID);
        DocumentData documentData = new DocumentData(caseUUID, type, displayName);
        documentRepository.save(documentData);
       // auditService.createDocumentEvent(documentData);
        log.info("Created Document: {}, Case UUID: {}", documentData.getUuid(), documentData.getCaseUUID());
        return documentData;
    }

    @Transactional
    public void updateDocument(UUID documentUUID, DocumentStatus status, String fileLink, String pdfLink) {
        log.debug("Updating Document: {}", documentUUID);
        DocumentData documentData = getDocumentData(documentUUID);
        documentData.update(fileLink, pdfLink, status);
        documentRepository.save(documentData);
       // auditService.updateDocumentEvent(documentData);
        log.info("Updated Document: {}", documentData.getUuid());
    }

    public DocumentData getDocumentData(UUID documentUUID) {
        DocumentData documentData = documentRepository.findByUuid(documentUUID);
        if (documentData != null) {
            return documentData;
        } else {
            throw new ApplicationExceptions.EntityNotFoundException("Document UUID: %s not found!", documentUUID);
        }
    }

    public Set<DocumentData> getDocumentsForCase(UUID caseUuid) {
        Set<DocumentData> documents = documentRepository.findAllByCaseUUID(caseUuid);
        return documents;
    }

    public void deleteDocument(UUID documentUUID) {
        DocumentData documentData = documentRepository.findByUuid(documentUUID);
        documentData.setDeleted(true);
        documentRepository.save(documentData);
        log.info("Set document to deleted: {}", documentUUID);
    }
}