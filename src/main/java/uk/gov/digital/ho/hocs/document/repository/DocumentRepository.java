package uk.gov.digital.ho.hocs.document.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.util.Set;
import java.util.UUID;

@Repository
public interface DocumentRepository extends CrudRepository<DocumentData, String> {

    DocumentData findByUuid(UUID uuid);

    Set<DocumentData> findAllByExternalReferenceUUID(UUID externalReferenceUUID);

    Set<DocumentData> findAllByExternalReferenceUUIDAndType(UUID externalReferenceUUID, String type);
}