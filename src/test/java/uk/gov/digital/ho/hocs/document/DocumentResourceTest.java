package uk.gov.digital.ho.hocs.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.GetDocumentsResponse;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentResourceTest {

    public static final java.util.UUID UUID = java.util.UUID.randomUUID();
    @Mock
    private DocumentDataService documentService;

    private DocumentDataResource documentResource;

    private UUID uuid = UUID.randomUUID();

    @Before
    public void setUp() {
        documentResource = new DocumentDataResource(documentService);
    }

    @Test
    public void shouldCreateDocumentWithValidParams() throws ApplicationExceptions.EntityCreationException {


        String displayName = "name";
        String documentType = "ORIGINAL";
        DocumentData documentData = new DocumentData(uuid, documentType, displayName);
        String fileName = "fileName";
        String convertTo = "convertTo";

        when(documentService.createDocument(uuid, displayName, fileName, documentType, convertTo)).thenReturn(documentData);

        CreateDocumentRequest request = new CreateDocumentRequest(displayName, documentType, fileName, uuid, convertTo);

        ResponseEntity response = documentResource.createDocument(request);

        verify(documentService, times(1)).createDocument(uuid, displayName, fileName, documentType, convertTo);

        verifyNoMoreInteractions(documentService);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldReturnListOfDocumentsForAType() {
        when(documentService.getDocumentsByReferenceForType(uuid, "DRAFT")).thenReturn(new HashSet<>());

        ResponseEntity<GetDocumentsResponse> response = documentResource.getDocumentsForCaseForType(uuid, "DRAFT");

        verify(documentService, times(1)).getDocumentsByReferenceForType(uuid, "DRAFT");
        verifyNoMoreInteractions(documentService);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldNotReturnMimeTypeIfDoesNotExist() {
        S3Document s3Document = mock(S3Document.class);
        String fileName = "TEST";

        when(s3Document.getMimeType()).thenReturn(null);
        when(s3Document.getData()).thenReturn(new byte[0]);
        when(s3Document.getOriginalFilename()).thenReturn(fileName);

        when(documentService.getDocumentPdf(uuid)).thenReturn(s3Document);

        var response = documentResource.getDocumentPdf(uuid);

        assertFalse(response.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
    }


}
