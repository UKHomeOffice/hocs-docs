package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.*;
import uk.gov.digital.ho.hocs.document.repository.ManagedDocumentRepository;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ManagedDocumentDataService {

    private final ManagedDocumentRepository managedDocumentRepository;
    private final S3DocumentService s3DocumentService;

    @Autowired
    public ManagedDocumentDataService(ManagedDocumentRepository managedDocumentRepository, S3DocumentService s3DocumentService){
        this.managedDocumentRepository = managedDocumentRepository;
        this.s3DocumentService = s3DocumentService;
    }

    @Transactional
    ManagedDocumentData createManagedDocument(UUID referenceUUID, String displayName, ManagedDocumentType type) {
        log.debug("Creating Managed Document: {}, Reference UUID: {}", displayName, referenceUUID);
        ManagedDocumentData managedDocumentData = new ManagedDocumentData(referenceUUID, type, displayName);
        managedDocumentRepository.save(managedDocumentData);
        log.info("Created Managed Document: {}, Reference UUID: {}", managedDocumentData.getUuid(), referenceUUID);
        return managedDocumentData;
    }

    @Transactional
    public void updateManagedDocument(UUID documentUUID, ManagedDocumentStatus status, String fileLink, String pdfLink) {
        log.debug("Updating Manged Document: {}", documentUUID);
        ManagedDocumentData managedDocumentData = getManagedDocumentData(documentUUID);
        managedDocumentData.update(fileLink, status);
        managedDocumentRepository.save(managedDocumentData);
        log.info("Updated Document: {}", managedDocumentData.getUuid());
    }


    public ManagedDocumentData getManagedDocumentData(UUID documentUUID) {
        ManagedDocumentData managedDocumentData = managedDocumentRepository.findByUuid(documentUUID);
        if (managedDocumentData != null) {
            return managedDocumentData;
        } else {
            throw new ApplicationExceptions.EntityNotFoundException("Managed DocumentDto UUID: %s not found!", documentUUID);
        }
    }

    public Set<ManagedDocumentData> getManagedDocumentsByReference(UUID caseUuid) {
        Set<ManagedDocumentData> documents = managedDocumentRepository.findAllByExternalReferenceUUID(caseUuid);
        return documents;
    }

    public void deleteManagedDocument(UUID documentUUID) {
        ManagedDocumentData managedDocumentData = managedDocumentRepository.findByUuid(documentUUID);
        managedDocumentData.setDeleted(true);
        managedDocumentRepository.save(managedDocumentData);
        log.info("Set Managed document to deleted: {}", documentUUID);
    }

    public Document getDocumentFile(UUID documentUUID) {
        ManagedDocumentData managedDocumentData = getManagedDocumentData(documentUUID);
        try {
            return s3DocumentService.getFileFromTrustedS3(managedDocumentData.getFileLink());
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException(e.getMessage());
        }
    }
}