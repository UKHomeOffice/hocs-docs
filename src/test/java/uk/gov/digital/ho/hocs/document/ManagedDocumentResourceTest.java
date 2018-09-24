package uk.gov.digital.ho.hocs.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.digital.ho.hocs.document.dto.CreateManagedDocumentRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentData;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ManagedDocumentResourceTest {

    @Mock
    private ManagedDocumentDataService documentService;

    private ManagedDocumentDataResource documentResource;

    @Before
    public void setUp(){
        documentResource = new ManagedDocumentDataResource(documentService);
    }

    @Test
    public void shouldCreateManagedDocumentWithValidParams() throws ApplicationExceptions.EntityCreationException {

        UUID uuid = UUID.randomUUID();
        String displayName = "name";
        ManagedDocumentType documentType = ManagedDocumentType.STANDARD_LINE;
        ManagedDocumentData documentData = new ManagedDocumentData(uuid, documentType, displayName);

        when(documentService.createManagedDocument(uuid, displayName, documentType)).thenReturn(documentData);

        CreateManagedDocumentRequest request = new CreateManagedDocumentRequest(displayName, documentType, uuid);

        ResponseEntity response = documentResource.createDocument(request);

        verify(documentService, times(1)).createManagedDocument(uuid, displayName, documentType);

        verifyNoMoreInteractions(documentService);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
