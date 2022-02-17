package uk.gov.digital.ho.hocs.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.client.auditclient.AuditClient;
import uk.gov.digital.ho.hocs.document.client.documentclient.DocumentClient;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.repository.DocumentRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private S3DocumentService s3DocumentService;

    @Mock
    private DocumentClient documentClient;

    @Mock
    private AuditClient auditClient;

    private DocumentDataService documentService;

    @Before
    public void setUp() {
        this.documentService = new DocumentDataService(
                documentRepository, s3DocumentService, auditClient, documentClient);
    }

    @Test
    public void shouldCreateDocumentWithValidParams() {

        UUID uuid = UUID.randomUUID();
        UUID actionDataItemUuid = UUID.randomUUID();
        String displayName = "name";
        String fileName = "fileName";
        String documentType = "ORIGINAL";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();
        String correlationId = "correlationId4321";

        ArgumentCaptor<DocumentData> argumentCaptor = ArgumentCaptor.forClass(DocumentData.class);

        UUID documentUUID = documentService.createDocument
                (new CreateDocumentRequest(uuid, actionDataItemUuid, displayName, fileName, documentType, convertTo, uploadOwnerUUID))
                .getUuid();

        verify(documentRepository).save(argumentCaptor.capture());
        verify(documentClient).processDocument(documentUUID, fileName, "PDF");
        verify(auditClient).createDocumentAudit(any());
        verifyNoMoreInteractions(documentRepository);
        verifyNoMoreInteractions(auditClient);
        verifyNoInteractions(s3DocumentService);

        DocumentData capturedDocumentData = argumentCaptor.getValue();

        assertEquals(capturedDocumentData.getExternalReferenceUUID(), uuid);
        assertEquals(capturedDocumentData.getActionDataItemUuid(), actionDataItemUuid);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateDocumentWhenDocumentUUIDIsNullException() {

        String displayName = "name";
        String fileName = "fileName";
        String documentType = "ORIGINAL";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();

        documentService.createDocument(new CreateDocumentRequest(
                null,
                null,
                displayName,
                fileName,
                documentType,
                convertTo,
                uploadOwnerUUID));
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentUUIDIsNull() {

        UUID uuid = UUID.randomUUID();
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();


        try {
            documentService.createDocument(
                    new CreateDocumentRequest(
                            uuid,
                            null,
                            null,
                            fileName,
                            documentType,
                            convertTo,
                            uploadOwnerUUID)
            );
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoInteractions(documentClient);
        verifyNoInteractions(auditClient);
        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(s3DocumentService);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateDocumentWhenDocumentTypeIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String fileName = "fileName";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();

        documentService.createDocument(
                new CreateDocumentRequest(uuid, null, displayName,  fileName,null, convertTo, uploadOwnerUUID));
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentTypeIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String fileName = "fileName";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();

        try {
            documentService.createDocument(
                    new CreateDocumentRequest(uuid, null, displayName, fileName, null, convertTo, uploadOwnerUUID));
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoInteractions(documentClient);
        verifyNoInteractions(auditClient);
        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(s3DocumentService);

    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateDocumentWhenDocumentDisplayNameIsNullException() {

        UUID uuid = UUID.randomUUID();
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();

        documentService.createDocument(
                new CreateDocumentRequest(uuid, null, null, fileName, documentType, convertTo, uploadOwnerUUID));
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentDisplayNameIsNull() {

        String displayName = "name";
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();

        try {
            documentService.createDocument(
                    new CreateDocumentRequest(
                            null,
                            null,
                            displayName,
                            fileName,
                            documentType,
                            convertTo,
                            uploadOwnerUUID));
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoInteractions(documentClient);
        verifyNoInteractions(auditClient);
        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(s3DocumentService);
    }

    @Test
    public void shouldUpdateDocumentWithValidParams() throws ApplicationExceptions.EntityCreationException, ApplicationExceptions.EntityNotFoundException {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(uuid, null, documentType, displayName, uploadOwnerUUID);
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, documentStatus, link, null);

        verify(documentRepository).findByUuid(uuid);
        verify(documentRepository).save(documentData);
        verify(auditClient).updateDocumentAudit(any());
        verifyNoInteractions(documentClient);
        verifyNoMoreInteractions(auditClient);
        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(s3DocumentService);
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

        verify(documentRepository).findByUuid(null);

        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(auditClient);
        verifyNoInteractions(s3DocumentService);

    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotUpdateDocumentWhenDocumentStatusIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(uuid, null, documentType, displayName, uploadOwnerUUID);
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, null, link, link);
    }

    @Test()
    public void shouldNotUpdateDocumentWhenDocumentStatusIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(uuid, null, documentType, displayName, uploadOwnerUUID);
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        try {
            documentService.updateDocument(uuid, null, link, link);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verify(documentRepository).findByUuid(uuid);

        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(auditClient);
        verifyNoInteractions(s3DocumentService);

    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotUpdateDocumentWhenDocumentFileLinkIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(uuid, null, documentType, displayName, uploadOwnerUUID);
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, documentStatus, null, link);
    }

    @Test()
    public void shouldNotUpdateDocumentWhenDocumentFileLinkIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(uuid, null, documentType, displayName, uploadOwnerUUID);
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        try {
            documentService.updateDocument(uuid, documentStatus, null, link);
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verify(documentRepository).findByUuid(uuid);

        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(auditClient);
        verifyNoInteractions(s3DocumentService);
    }

    @Test
    public void shouldReturnDocumentListForReference() {

        UUID uuid = UUID.randomUUID();

        when(documentRepository.findAllByExternalReferenceUUID(uuid)).thenReturn(new HashSet<>());

        documentService.getDocumentsByReference(uuid);

        verify(documentRepository).findAllByExternalReferenceUUID(uuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldReturnDocumentListForReferenceAndForType() {

        UUID uuid = UUID.randomUUID();

        when(documentRepository.findAllByExternalReferenceUUIDAndType(uuid,"DRAFT" )).thenReturn(new HashSet<>());

        documentService.getDocumentsByReferenceForType(uuid, "DRAFT");

        verify(documentRepository).findAllByExternalReferenceUUIDAndType(uuid, "DRAFT");
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldAuditSuccessfulCreateDocument() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();

        documentService.createDocument(
                new CreateDocumentRequest(uuid, null, displayName, fileName, documentType, convertTo, uploadOwnerUUID));

        verify(auditClient).createDocumentAudit(any());
        verifyNoMoreInteractions(auditClient);

    }

    @Test
    public void shouldAuditSuccessfulUpdateDocument() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(uuid, null, documentType, displayName,uploadOwnerUUID);
        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, DocumentStatus.UPLOADED,"", "");

        verify(auditClient).updateDocumentAudit(documentData);
        verifyNoMoreInteractions(auditClient);
    }

    @Test
    public void shouldAuditSuccessfulDeleteDocument() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(uuid, null, documentType, displayName, uploadOwnerUUID);
        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.deleteDocument(uuid);

        verify(auditClient).deleteDocumentAudit(any());
        verifyNoMoreInteractions(auditClient);

    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotAuditWhenCreateDocumentFails() {

        String displayName = "name";
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";
        UUID uploadOwnerUUID = UUID.randomUUID();

        documentService.createDocument(
                new CreateDocumentRequest(
                        null,
                        null,
                        displayName,
                        fileName,
                        documentType,
                        convertTo,
                        uploadOwnerUUID
                ));

        verifyNoInteractions(auditClient);

    }

    @Test(expected = ApplicationExceptions.EntityNotFoundException.class)
    public void shouldNotAuditWhenUpdateDocumentFails() {

        UUID uuid = UUID.randomUUID();
        when(documentRepository.findByUuid(uuid)).thenReturn(null);

        documentService.updateDocument(uuid, DocumentStatus.UPLOADED,"", "");

        verifyNoInteractions(auditClient);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotAuditWhenDeleteDocumentFails() {

        UUID uuid = UUID.randomUUID();
        when(documentRepository.findByUuid(uuid)).thenReturn(null);

        documentService.deleteDocument(uuid);

        verifyNoMoreInteractions(auditClient);
    }

    @Test
    public void shouldReturnDocumentByFileUsesFileLink() throws IOException {
        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getFileLink()).thenReturn("fileLink");
        when(s3DocumentService.getFileFromTrustedS3("fileLink")).thenReturn(null);
        UUID uuid = UUID.randomUUID();
        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.getDocumentFile(uuid);

        verify(s3DocumentService, times(1)).getFileFromTrustedS3("fileLink");
        verifyNoMoreInteractions((s3DocumentService));
        verify(documentRepository, times(1)).findByUuid(uuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldReturnDocumentByPdfUsesPdfLink() throws IOException {
        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getPdfLink()).thenReturn("pdfLink");
        when(s3DocumentService.getFileFromTrustedS3("pdfLink")).thenReturn(null);
        UUID uuid = UUID.randomUUID();
        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.getDocumentPdf(uuid);

        verify(s3DocumentService, times(1)).getFileFromTrustedS3("pdfLink");
        verifyNoMoreInteractions((s3DocumentService));
        verify(documentRepository, times(1)).findByUuid(uuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldReturnEmptyDocumentIfFileLinkIsEmpty() {
        UUID documentUuid = UUID.randomUUID();
        String originalFileName = "TEST";

        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getFileLink()).thenReturn("");
        when(documentData.getDisplayName()).thenReturn(originalFileName);

        when(documentRepository.findByUuid(documentUuid)).thenReturn(documentData);

        var response = documentService.getDocumentFile(documentUuid);

        assertEquals(response.getOriginalFilename(), originalFileName);
        assertEquals(response.getData().length, 0);
        assertNull(response.getFilename());
        assertNull(response.getFileType());
        assertNull(response.getMimeType());

        verify(documentRepository).findByUuid(documentUuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldReturnEmptyDocumentIfPdfLinkIsEmpty() {
        UUID documentUuid = UUID.randomUUID();
        String originalFileName = "TEST";

        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getPdfLink()).thenReturn("");
        when(documentData.getDisplayName()).thenReturn(originalFileName);

        when(documentRepository.findByUuid(documentUuid)).thenReturn(documentData);

        var response = documentService.getDocumentPdf(documentUuid);

        assertEquals(response.getOriginalFilename(), originalFileName);
        assertEquals(response.getData().length, 0);
        assertNull(response.getFilename());
        assertNull(response.getFileType());
        assertNull(response.getMimeType());

        verify(documentRepository).findByUuid(documentUuid);
        verifyNoMoreInteractions(documentRepository);
    }
}
