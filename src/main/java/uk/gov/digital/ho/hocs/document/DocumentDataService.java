package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.Document;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.model.DocumentType;
import uk.gov.digital.ho.hocs.document.repository.DocumentRepository;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DocumentDataService {

    private final DocumentRepository documentRepository;
    private final S3DocumentService s3DocumentService;

    @Autowired
    public DocumentDataService(DocumentRepository documentRepository, S3DocumentService s3DocumentService){
        this.documentRepository = documentRepository;
        this.s3DocumentService = s3DocumentService;
    }

    @Transactional
    DocumentData createDocument(UUID caseUUID, String displayName, DocumentType type) {
        log.debug("Creating DocumentDto: {}, Case UUID: {}", displayName, caseUUID);
        DocumentData documentData = new DocumentData(caseUUID, type, displayName);
        documentRepository.save(documentData);
        log.info("Created DocumentDto: {}, Case UUID: {}", documentData.getUuid(), documentData.getCaseUUID());
        return documentData;
    }

    @Transactional
    public void updateDocument(UUID documentUUID, DocumentStatus status, String fileLink, String pdfLink) {
        log.debug("Updating DocumentDto: {}", documentUUID);
        DocumentData documentData = getDocumentData(documentUUID);
        documentData.update(fileLink, pdfLink, status);
        documentRepository.save(documentData);
        log.info("Updated DocumentDto: {}", documentData.getUuid());
    }

    public DocumentData getDocumentData(UUID documentUUID) {
        DocumentData documentData = documentRepository.findByUuid(documentUUID);
        if (documentData != null) {
            return documentData;
        } else {
            throw new ApplicationExceptions.EntityNotFoundException("DocumentDto UUID: %s not found!", documentUUID);
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

    public Document getDocumentOriginal(UUID documentUUID) {
        DocumentData documentData = getDocumentData(documentUUID);
        try {
            return s3DocumentService.getFileFromTrustedS3(documentData.getFileLink());
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException(e.getMessage());
        }
    }

    public Document getDocumentPdf(UUID documentUUID) {
        DocumentData documentData = getDocumentData(documentUUID);
        try{
            return s3DocumentService.getFileFromTrustedS3(documentData.getPdfLink());
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException(e.getMessage());
        }
    }
}