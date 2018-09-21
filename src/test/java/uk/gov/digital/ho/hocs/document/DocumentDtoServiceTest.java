package uk.gov.digital.ho.hocs.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.model.DocumentType;
import uk.gov.digital.ho.hocs.document.repository.DocumentRepository;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentDtoServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    //private AuditService auditService;

    private DocumentDataService documentService;

    @Before
    public void setUp() {
        this.documentService = new DocumentDataService(
                documentRepository);
    }

    @Test
    public void shouldCreateDocumentWithValidParams() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;

        documentService.createDocument(uuid, displayName, documentType);

        verify(documentRepository, times(1)).save(any(DocumentData.class));
        //verify(auditService, times(1)).createDocumentEvent(any(DocumentData.class));

        verifyNoMoreInteractions(documentRepository);
        //verifyNoMoreInteractions(auditService);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateDocumentWhenDocumentUUIDIsNullException() {

        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;

        documentService.createDocument(null, displayName, documentType);
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentUUIDIsNull() {

        UUID uuid = UUID.randomUUID();
        DocumentType documentType = DocumentType.ORIGINAL;

        try {
            documentService.createDocument(uuid, null, documentType);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoMoreInteractions(documentRepository);
        //verifyZeroInteractions(auditService);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateDocumentWhenDocumentTypeIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";

        documentService.createDocument(uuid, displayName, null);
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentTypeIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";

        try {
            documentService.createDocument(uuid, displayName, null);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoMoreInteractions(documentRepository);
        //verifyZeroInteractions(auditService);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateDocumentWhenDocumentDisplayNameIsNullException() {

        UUID uuid = UUID.randomUUID();
        DocumentType documentType = DocumentType.ORIGINAL;

        documentService.createDocument(uuid, null, documentType);
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentDisplayNameIsNull() {

        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;

        try {
            documentService.createDocument(null, displayName, documentType);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoMoreInteractions(documentRepository);
        //verifyZeroInteractions(auditService);
    }

    @Test
    public void shouldUpdateDocumentWithValidParams() throws ApplicationExceptions.EntityCreationException, ApplicationExceptions.EntityNotFoundException {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;
        DocumentData documentData = new DocumentData(uuid, documentType, displayName);
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, documentStatus, link, null);

        verify(documentRepository, times(1)).findByUuid(uuid);
        verify(documentRepository, times(1)).save(documentData);
        //verify(auditService, times(1)).updateDocumentEvent(documentData);

        verifyNoMoreInteractions(documentRepository);
        //verifyZeroInteractions(auditService);
    }

    @Test(expected = ApplicationExceptions.EntityNotFoundException.class)
    public void shouldNotUpdateDocumentWhenNoDocumentFound() throws ApplicationExceptions.EntityCreationException, ApplicationExceptions.EntityNotFoundException {

        UUID uuid = UUID.randomUUID();
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(null);

        documentService.updateDocument(uuid, documentStatus, link, link);
    }

    @Test(expected = ApplicationExceptions.EntityNotFoundException.class)
    public void shouldNotUpdateDocumentWhenDocumentUUIDIsNullException() {

        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        documentService.updateDocument(null, documentStatus, link, link);
    }

    @Test()
    public void shouldNotUpdateDocumentWhenDocumentUUIDIsNull() {

        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        try {
            documentService.updateDocument(null, documentStatus, link, link);
        } catch (ApplicationExceptions.EntityNotFoundException e) {
            // Do Nothing.
        }

        verify(documentRepository, times(1)).findByUuid(null);

        verifyNoMoreInteractions(documentRepository);
        //verifyZeroInteractions(auditService);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotUpdateDocumentWhenDocumentStatusIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;
        DocumentData documentData = new DocumentData(uuid, documentType, displayName);
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, null, link, link);
    }

    @Test()
    public void shouldNotUpdateDocumentWhenDocumentStatusIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;
        DocumentData documentData = new DocumentData(uuid, documentType, displayName);
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        try {
            documentService.updateDocument(uuid, null, link, link);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verify(documentRepository, times(1)).findByUuid(uuid);

        verifyNoMoreInteractions(documentRepository);
        //verifyZeroInteractions(auditService);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotUpdateDocumentWhenDocumentFileLinkIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;
        DocumentData documentData = new DocumentData(uuid, documentType, displayName);
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, documentStatus, null, link);
    }

    @Test()
    public void shouldNotUpdateDocumentWhenDocumentFileLinkIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;
        DocumentData documentData = new DocumentData(uuid, documentType, displayName);
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        try {
            documentService.updateDocument(uuid, documentStatus, null, link);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verify(documentRepository, times(1)).findByUuid(uuid);

        verifyNoMoreInteractions(documentRepository);
        //verifyZeroInteractions(auditService);
    }

}