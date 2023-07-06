package uk.gov.digital.ho.hocs.document.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface DocumentRepository extends CrudRepository<DocumentData, String> {

    @Query(value = "SELECT * FROM document_data WHERE uuid = ?1", nativeQuery = true)
    DocumentData findByUuid(UUID uuid);

    @Query(value = "SELECT * FROM document_data WHERE uuid = ?1 AND NOT deleted", nativeQuery = true)
    DocumentData findActiveByUuid(UUID uuid);

    @Query(value = "SELECT * FROM document_data WHERE external_reference_uuid = ?1 AND NOT deleted", nativeQuery = true)
    Set<DocumentData> findAllActiveByExternalReferenceUUID(UUID externalReferenceUUID);

    @Query(value = "SELECT * FROM document_data WHERE status = ?1 AND NOT deleted AND file_link IS NOT NULL", nativeQuery = true)
    List<DocumentData> findAllByStatus(String status);
}
