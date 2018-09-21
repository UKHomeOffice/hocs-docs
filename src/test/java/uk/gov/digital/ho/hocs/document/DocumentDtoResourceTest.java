package uk.gov.digital.ho.hocs.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.model.DocumentType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentDtoResourceTest {

    @Mock
    private DocumentDataService documentService;

    private DocumentDataResource documentResource;

    @Before
    public void setUp(){
        documentResource = new DocumentDataResource(documentService);
    }

    @Test
    public void shouldCreateDocumentWithValidParams() throws ApplicationExceptions.EntityCreationException {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        DocumentType documentType = DocumentType.ORIGINAL;
        DocumentData documentData = new DocumentData(uuid, documentType, displayName);

        when(documentService.createDocument(uuid, displayName, documentType)).thenReturn(documentData);

        CreateDocumentRequest request = new CreateDocumentRequest(displayName, documentType);

        ResponseEntity response = documentResource.createDocument(uuid, request);

        verify(documentService, times(1)).createDocument(uuid, displayName, documentType);

        verifyNoMoreInteractions(documentService);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
