package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentData;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentType;
import uk.gov.digital.ho.hocs.document.repository.ManagedDocumentRepository;

import java.util.UUID;

@Service
@Slf4j
public class ManagedDocumentDataService {

    private final ManagedDocumentRepository managedDocumentRepository;

    @Autowired
    public ManagedDocumentDataService(ManagedDocumentRepository managedDocumentRepository){
        this.managedDocumentRepository = managedDocumentRepository;

    }

    @Transactional
    ManagedDocumentData createManagedDocument(String displayName, ManagedDocumentType type) {
        log.debug("Creating Managed Document: {} Type: {}", displayName, type);
        ManagedDocumentData managedDocumentData = new ManagedDocumentData(type, displayName);
        managedDocumentRepository.save(managedDocumentData);
        log.info("Created Managed Document: {}, Type: {}", managedDocumentData.getUuid(), type);
        return managedDocumentData;
    }

    @Transactional
    public void updateDocument(UUID documentUUID, String fileLink, String pdfLink) {
        log.debug("Updating Managed Document: {}", documentUUID);
        ManagedDocumentData managedDocumentData = getManagedDocumentData(documentUUID);
        managedDocumentData.update(fileLink, pdfLink);
        managedDocumentRepository.save(managedDocumentData);
        log.info("Updated Managed Document: {}", managedDocumentData.getUuid());
    }

    public ManagedDocumentData getManagedDocumentData(UUID documentUUID) {
        ManagedDocumentData managedDocumentData = managedDocumentRepository.findByUuid(documentUUID);
        if (managedDocumentData != null) {
            return managedDocumentData;
        } else {
            throw new ApplicationExceptions.EntityNotFoundException("Managed Document UUID: %s not found!", documentUUID);
        }
    }

    public void deleteManagedDocument(UUID documentUUID) {
        ManagedDocumentData managedDocumentData = managedDocumentRepository.findByUuid(documentUUID);
        managedDocumentData.setDeleted(true);
        managedDocumentRepository.save(managedDocumentData);
        log.info("Set Managed document to deleted: {}", documentUUID);
    }
}