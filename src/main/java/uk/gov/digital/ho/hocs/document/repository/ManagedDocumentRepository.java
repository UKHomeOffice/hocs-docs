package uk.gov.digital.ho.hocs.document.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentData;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ManagedDocumentRepository extends CrudRepository<ManagedDocumentData, String> {

    ManagedDocumentData findByUuid(UUID uuid);

    Set<ManagedDocumentData> findAllByExternalReferenceUUID(UUID externalReferenceUUID);
}