package uk.gov.digital.ho.hocs.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.client.auditclient.AuditClient;
import uk.gov.digital.ho.hocs.document.client.documentclient.DocumentClient;
import uk.gov.digital.ho.hocs.document.dto.CopyDocumentsRequest;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.repository.DocumentRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
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

    @Mock
    private RequestData requestData;

    private DocumentDataService documentService;

    @Captor
    private ArgumentCaptor<List<DocumentData>> documentDataSetCaptorRepository;

    @Captor
    private ArgumentCaptor<List<DocumentData>> documentDataSetCaptorAudit;

    private static final String USER_ID = "d030c101-3ff6-43d7-9b6c-9cd54ccf5529";

    @Before
    public void setUp() {
        this.documentService = new DocumentDataService(documentRepository, s3DocumentService, auditClient,
            documentClient, requestData);
    }

    @Test
    public void shouldCreateDocumentWithValidParams() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String fileName = "fileName";
        String documentType = "ORIGINAL";
        String convertTo = "PDF";

        when(requestData.userId()).thenReturn(USER_ID);

        ArgumentCaptor<DocumentData> argumentCaptor = ArgumentCaptor.forClass(DocumentData.class);

        UUID documentUUID = documentService.createDocument(
            new CreateDocumentRequest(uuid, displayName, fileName, documentType, convertTo)).getUuid();

        verify(documentRepository).save(argumentCaptor.capture());
        DocumentData capturedDocumentData = argumentCaptor.getValue();

        verify(documentClient).processDocument(documentUUID, fileName, "PDF");
        verify(auditClient).createDocumentAudit(capturedDocumentData);
        verifyNoMoreInteractions(documentRepository);
        verifyNoMoreInteractions(auditClient);
        verifyNoInteractions(s3DocumentService);

        assertEquals(capturedDocumentData.getExternalReferenceUUID(), uuid);
    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotCreateDocumentWhenDocumentUUIDIsNullException() {

        String displayName = "name";
        String fileName = "fileName";
        String documentType = "ORIGINAL";
        String convertTo = "PDF";

        when(requestData.userId()).thenReturn(USER_ID);

        documentService.createDocument(new CreateDocumentRequest(null, displayName, fileName, documentType, convertTo));
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentUUIDIsNull() {

        UUID uuid = UUID.randomUUID();
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";

        when(requestData.userId()).thenReturn(USER_ID);

        try {
            documentService.createDocument(new CreateDocumentRequest(uuid, null, fileName, documentType, convertTo));
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

        when(requestData.userId()).thenReturn(USER_ID);

        documentService.createDocument(new CreateDocumentRequest(uuid, displayName, fileName, null, convertTo));
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentTypeIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String fileName = "fileName";
        String convertTo = "PDF";

        when(requestData.userId()).thenReturn(USER_ID);

        try {
            documentService.createDocument(new CreateDocumentRequest(uuid, displayName, fileName, null, convertTo));
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

        when(requestData.userId()).thenReturn(USER_ID);

        documentService.createDocument(new CreateDocumentRequest(uuid, null, fileName, documentType, convertTo));
    }

    @Test()
    public void shouldNotCreateDocumentWhenDocumentDisplayNameIsNull() {

        String displayName = "name";
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";

        when(requestData.userId()).thenReturn(USER_ID);

        try {
            documentService.createDocument(
                new CreateDocumentRequest(null, displayName, fileName, documentType, convertTo));
        } catch (ApplicationExceptions.EntityCreationException e) {
            // Do Nothing.
        }

        verifyNoInteractions(documentClient);
        verifyNoInteractions(auditClient);
        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(s3DocumentService);
    }

    @Test
    public void shouldUpdateDocumentWithValidParams() throws ApplicationExceptions.EntityUpdateException, ApplicationExceptions.EntityNotFoundException {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";

        UUID uploadOwnerUUID = UUID.fromString(USER_ID);

        DocumentData documentData = new DocumentData(uuid, documentType, displayName, uploadOwnerUUID);
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, documentStatus, link, null);

        verify(documentRepository).findByUuid(uuid);
        verify(documentRepository).save(documentData);
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

    @Test(expected = ApplicationExceptions.EntityUpdateException.class)
    public void shouldNotUpdateDocumentWhenDocumentStatusIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.fromString(USER_ID);
        DocumentData documentData = new DocumentData(uuid, documentType, displayName, uploadOwnerUUID);
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.updateDocument(uuid, null, link, link);
    }

    @Test()
    public void shouldNotUpdateDocumentWhenDocumentStatusIsNull() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.fromString(USER_ID);
        DocumentData documentData = new DocumentData(uuid, documentType, displayName, uploadOwnerUUID);
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        try {
            documentService.updateDocument(uuid, null, link, link);
        } catch (ApplicationExceptions.EntityUpdateException e) {
            // Do Nothing.
        }

        verify(documentRepository).findByUuid(uuid);

        verifyNoMoreInteractions(documentRepository);
        verifyNoInteractions(auditClient);
        verifyNoInteractions(s3DocumentService);

    }

    @Test(expected = ApplicationExceptions.EntityUpdateException.class)
    public void shouldNotUpdateDocumentWhenDocumentFileLinkIsNullException() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.fromString(USER_ID);
        DocumentData documentData = new DocumentData(uuid, documentType, displayName, uploadOwnerUUID);
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
        UUID uploadOwnerUUID = UUID.fromString(USER_ID);
        DocumentData documentData = new DocumentData(uuid, documentType, displayName, uploadOwnerUUID);
        DocumentStatus documentStatus = DocumentStatus.UPLOADED;
        String link = "";

        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        try {
            documentService.updateDocument(uuid, documentStatus, null, link);
        } catch (ApplicationExceptions.EntityUpdateException e) {
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

        when(documentRepository.findAllActiveByExternalReferenceUUID(uuid)).thenReturn(new HashSet<>());

        documentService.getDocumentsByReference(uuid, null);

        verify(documentRepository).findAllActiveByExternalReferenceUUID(uuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldReturnDocumentListForReferenceAndForType() {

        UUID uuid = UUID.randomUUID();

        when(documentRepository.findAllActiveByExternalReferenceUUID(uuid)).thenReturn(new HashSet<>());

        documentService.getDocumentsByReference(uuid, "DRAFT");

        verify(documentRepository).findAllActiveByExternalReferenceUUID(uuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldAuditSuccessfulCreateDocument() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";

        when(requestData.userId()).thenReturn(USER_ID);

        var documentData = documentService.createDocument(
            new CreateDocumentRequest(uuid, displayName, fileName, documentType, convertTo));

        verify(auditClient).createDocumentAudit(documentData);
        verifyNoMoreInteractions(auditClient);

    }

    @Test
    public void shouldAuditSuccessfulDeleteDocument() {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.randomUUID();
        DocumentData documentData = new DocumentData(uuid, documentType, displayName, uploadOwnerUUID);
        documentData.setDeleted();
        when(documentRepository.findByUuid(uuid)).thenReturn(documentData);

        documentService.deleteDocument(uuid);

        verify(auditClient).deleteDocumentAudit(documentData);
        verifyNoMoreInteractions(auditClient);

    }

    @Test(expected = ApplicationExceptions.EntityCreationException.class)
    public void shouldNotAuditWhenCreateDocumentFails() {

        String displayName = "name";
        String documentType = "ORIGINAL";
        String fileName = "fileName";
        String convertTo = "PDF";

        when(requestData.userId()).thenReturn(USER_ID);

        documentService.createDocument(new CreateDocumentRequest(null, displayName, fileName, documentType, convertTo));

        verifyNoInteractions(auditClient);

    }

    @Test(expected = ApplicationExceptions.EntityNotFoundException.class)
    public void shouldNotAuditWhenUpdateDocumentFails() {

        UUID uuid = UUID.randomUUID();
        when(documentRepository.findByUuid(uuid)).thenReturn(null);

        documentService.updateDocument(uuid, DocumentStatus.UPLOADED, "", "");

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
        when(documentRepository.findActiveByUuid(uuid)).thenReturn(documentData);

        documentService.getDocumentFile(uuid);

        verify(s3DocumentService, times(1)).getFileFromTrustedS3("fileLink");
        verifyNoMoreInteractions((s3DocumentService));
        verify(documentRepository, times(1)).findActiveByUuid(uuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldReturnDocumentByPdfUsesPdfLink() throws IOException {
        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getPdfLink()).thenReturn("pdfLink");
        when(s3DocumentService.getFileFromTrustedS3("pdfLink")).thenReturn(null);
        UUID uuid = UUID.randomUUID();
        when(documentRepository.findActiveByUuid(uuid)).thenReturn(documentData);

        documentService.getDocumentPdf(uuid);

        verify(s3DocumentService, times(1)).getFileFromTrustedS3("pdfLink");
        verifyNoMoreInteractions((s3DocumentService));
        verify(documentRepository, times(1)).findActiveByUuid(uuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldReturnEmptyDocumentIfFileLinkIsEmpty() {
        UUID documentUuid = UUID.randomUUID();
        String originalFileName = "TEST";

        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getFileLink()).thenReturn("");
        when(documentData.getDisplayName()).thenReturn(originalFileName);

        when(documentRepository.findActiveByUuid(documentUuid)).thenReturn(documentData);

        var response = documentService.getDocumentFile(documentUuid);

        assertEquals(response.getOriginalFilename(), originalFileName);
        assertEquals(response.getData().length, 0);
        assertNull(response.getFilename());
        assertNull(response.getFileType());
        assertNull(response.getMimeType());

        verify(documentRepository).findActiveByUuid(documentUuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldReturnEmptyDocumentIfPdfLinkIsEmpty() {
        UUID documentUuid = UUID.randomUUID();
        String originalFileName = "TEST";

        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getPdfLink()).thenReturn("");
        when(documentData.getDisplayName()).thenReturn(originalFileName);

        when(documentRepository.findActiveByUuid(documentUuid)).thenReturn(documentData);

        var response = documentService.getDocumentPdf(documentUuid);

        assertEquals(response.getOriginalFilename(), originalFileName);
        assertEquals(response.getData().length, 0);
        assertNull(response.getFilename());
        assertNull(response.getFileType());
        assertNull(response.getMimeType());

        verify(documentRepository).findActiveByUuid(documentUuid);
        verifyNoMoreInteractions(documentRepository);
    }

    @Test
    public void shouldCopyDocumentsWithValidRequest() {
        UUID fromUUID = UUID.randomUUID();
        UUID toUUID = UUID.randomUUID();
        Set<String> types = Set.of("To document");

        DocumentData documentData = new DocumentData(fromUUID, "To document", "name", UUID.randomUUID());
        documentData.update("fileLink", "pdfLink", DocumentStatus.UPLOADED

                           );
        Set<DocumentData> documents = Set.of(documentData);

        CopyDocumentsRequest request = new CopyDocumentsRequest(fromUUID, toUUID, types);

        when(documentRepository.findAllActiveByExternalReferenceUUID(fromUUID)).thenReturn(documents);

        documentService.copyDocuments(request);

        verify(documentRepository, times(1)).saveAll(documentDataSetCaptorRepository.capture());
        verify(auditClient, times(1)).createDocumentsAudit(documentDataSetCaptorAudit.capture());

        List<DocumentData> capturedDocumentData = documentDataSetCaptorRepository.getValue();
        DocumentData capturedDocument = capturedDocumentData.get(0);

        assertEquals(capturedDocument.getExternalReferenceUUID(), toUUID);
        assertEquals(capturedDocument.getType(), "To document");
        assertEquals(capturedDocument.getFileLink(), "fileLink");
        assertEquals(capturedDocument.getPdfLink(), "pdfLink");

        capturedDocumentData = documentDataSetCaptorAudit.getValue();
        capturedDocument = capturedDocumentData.get(0);

        assertEquals(capturedDocument.getExternalReferenceUUID(), toUUID);
        assertEquals(capturedDocument.getType(), "To document");
        assertEquals(capturedDocument.getFileLink(), "fileLink");
        assertEquals(capturedDocument.getPdfLink(), "pdfLink");
    }

}
