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

    @Mock
    private DocumentDataService documentService;

    private DocumentDataResource documentResource;

    private static final String USER_ID = "d030c101-3ff6-43d7-9b6c-9cd54ccf5529";
    private static final UUID DOC_ID = UUID.fromString("16664dda-e680-4d3b-951f-c90afb10d62f");

    @Before
    public void setUp() {
        documentResource = new DocumentDataResource(documentService);
    }

    @Test
    public void shouldCreateDocumentWithValidParams() throws ApplicationExceptions.EntityCreationException {
        String displayName = "name";
        String documentType = "ORIGINAL";
        UUID uploadOwnerUUID = UUID.fromString(USER_ID);
        DocumentData documentData = new DocumentData(DOC_ID, null, documentType, displayName, uploadOwnerUUID);
        String fileName = "fileName";
        String convertTo = "convertTo";

        final CreateDocumentRequest createDocumentRequest = new CreateDocumentRequest(
                DOC_ID,
                null,
                displayName,
                fileName,
                documentType,
                convertTo);

        when(documentService.createDocument(createDocumentRequest)).thenReturn(documentData);

        ResponseEntity response = documentResource.createDocument(createDocumentRequest);

        verify(documentService, times(1))
                .createDocument(createDocumentRequest);

        verifyNoMoreInteractions(documentService);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldReturnListOfDocumentsForAType() {
        when(documentService.getDocumentsByReferenceForType(DOC_ID, "DRAFT")).thenReturn(new HashSet<>());

        ResponseEntity<GetDocumentsResponse> response = documentResource.getDocumentsForCaseForType(DOC_ID, "DRAFT");

        verify(documentService, times(1)).getDocumentsByReferenceForType(DOC_ID, "DRAFT");
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

        when(documentService.getDocumentPdf(DOC_ID)).thenReturn(s3Document);

        var response = documentResource.getDocumentPdf(DOC_ID);

        assertFalse(response.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
    }


}
