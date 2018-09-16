package uk.gov.digital.ho.hocs.document.repository;

import org.hibernate.annotations.Where;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentData;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentType;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Repository
@Where(clause = "deleted =false")
public interface ManagedDocumentRepository extends CrudRepository<ManagedDocumentData, String> {

    ManagedDocumentData findByUuid(UUID uuid);

    Set<ManagedDocumentData> findAllByTypeAndExpiresBefore(ManagedDocumentType type, LocalDate now);
}