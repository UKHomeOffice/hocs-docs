package uk.gov.digital.ho.hocs.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentData;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentStatus;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentType;
import uk.gov.digital.ho.hocs.document.repository.ManagedDocumentRepository;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ManagedDocumentServiceTest {

    @Mock
    private ManagedDocumentRepository documentRepository;

    @Mock
    private S3DocumentService s3DocumentService;

    private ManagedDocumentDataService documentService;

    @Before
    public void setUp() {
        this.documentService = new ManagedDocumentDataService(
                documentRepository, s3DocumentService);
    }

    @Test
    public void shouldCreateManagedDocumentWithValidParams() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;

        documentService.createManagedDocument(uuid, displayName, documentType);

        verify(documentRepository, times(1)).save(any(ManagedDocumentData.class));
        verifyNoMoreInteractions(documentRepository);
        verifyZeroInteractions(s3DocumentService);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateManagedDocumentWhenManagedDocumentUUIDIsNullException() {

        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;

        documentService.createManagedDocument(null, displayName, documentType);
    }

    @Test()
    public void shouldNotCreateManagedDocumentWhenManagedDocumentUUIDIsNull() {

        UUID uuid = UUID.randomUUID();
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;

        try {
            documentService.createManagedDocument(uuid, null, documentType);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoMoreInteractions(documentRepository);
        verifyZeroInteractions(s3DocumentService);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateManagedDocumentWhenManagedDocumentTypeIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";

        documentService.createManagedDocument(uuid, displayName, null);
    }

    @Test()
    public void shouldNotCreateManagedDocumentWhenManagedDocumentTypeIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";

        try {
            documentService.createManagedDocument(uuid, displayName, null);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoMoreInteractions(documentRepository);
        verifyZeroInteractions(s3DocumentService);

    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateManagedDocumentWhenManagedDocumentDisplayNameIsNullException() {

        UUID uuid = UUID.randomUUID();
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;

        documentService.createManagedDocument(uuid, null, documentType);
    }

    @Test()
    public void shouldNotCreateManagedDocumentWhenManagedDocumentDisplayNameIsNull() {

        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;

        try {
            documentService.createManagedDocument(null, displayName, documentType);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoMoreInteractions(documentRepository);
        verifyZeroInteractions(s3DocumentService);
    }

    @Test
    public void shouldUpdateManagedDocumentWithValidParams() throws ApplicationExceptions.EntityCreationException, ApplicationExceptions.EntityNotFoundException {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;
        ManagedDocumentData documentData = new ManagedDocumentData(uuid, documentType, displayName);
        ManagedDocumentStatus documentStatus = ManagedDocumentStatus.ACTIVE;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateManagedDocument(uuid, documentStatus, link, null);

        verify(documentRepository, times(1)).findByUuid(uuid);
        verify(documentRepository, times(1)).save(documentData);

        verifyNoMoreInteractions(documentRepository);
        verifyZeroInteractions(s3DocumentService);
    }

    @Test(expected = ApplicationExceptions.EntityNotFoundException.class)
    public void shouldNotUpdateManagedDocumentWhenNoManagedDocumentFound() throws ApplicationExceptions.EntityCreationException, ApplicationExceptions.EntityNotFoundException {

        UUID uuid = UUID.randomUUID();
        ManagedDocumentStatus documentStatus = ManagedDocumentStatus.ACTIVE;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(null);

        documentService.updateManagedDocument(uuid, documentStatus, link, link);
    }

    @Test(expected = ApplicationExceptions.EntityNotFoundException.class)
    public void shouldNotUpdateManagedDocumentWhenManagedDocumentUUIDIsNullException() {

        ManagedDocumentStatus documentStatus = ManagedDocumentStatus.ACTIVE;
        String link = "";

        documentService.updateManagedDocument(null, documentStatus, link, link);
    }

    @Test()
    public void shouldNotUpdateManagedDocumentWhenManagedDocumentUUIDIsNull() {

        ManagedDocumentStatus documentStatus = ManagedDocumentStatus.ACTIVE;
        String link = "";

        try {
            documentService.updateManagedDocument(null, documentStatus, link, link);
        } catch (ApplicationExceptions.EntityNotFoundException e) {
            // Do Nothing.
        }

        verify(documentRepository, times(1)).findByUuid(null);

        verifyNoMoreInteractions(documentRepository);
        verifyZeroInteractions(s3DocumentService);

    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotUpdateManagedDocumentWhenManagedDocumentStatusIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;
        ManagedDocumentData documentData = new ManagedDocumentData(uuid, documentType, displayName);
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateManagedDocument(uuid, null, link, link);
    }

    @Test()
    public void shouldNotUpdateManagedDocumentWhenManagedDocumentStatusIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;
        ManagedDocumentData documentData = new ManagedDocumentData(uuid, documentType, displayName);
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        try {
            documentService.updateManagedDocument(uuid, null, link, link);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verify(documentRepository, times(1)).findByUuid(uuid);

        verifyNoMoreInteractions(documentRepository);
        verifyZeroInteractions(s3DocumentService);

    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotUpdateManagedDocumentWhenManagedDocumentFileLinkIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;
        ManagedDocumentData documentData = new ManagedDocumentData(uuid, documentType, displayName);
        ManagedDocumentStatus documentStatus = ManagedDocumentStatus.ACTIVE;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateManagedDocument(uuid, documentStatus, null, link);
    }

    @Test()
    public void shouldNotUpdateManagedDocumentWhenManagedDocumentFileLinkIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;
        ManagedDocumentData documentData = new ManagedDocumentData(uuid, documentType, displayName);
        ManagedDocumentStatus documentStatus = ManagedDocumentStatus.ACTIVE;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        try {
            documentService.updateManagedDocument(uuid, documentStatus, null, link);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verify(documentRepository, times(1)).findByUuid(uuid);

        verifyNoMoreInteractions(documentRepository);
        verifyZeroInteractions(s3DocumentService);
    }
}