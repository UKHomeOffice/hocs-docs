package uk.gov.digital.ho.hocs.document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.DocumentDto;
import uk.gov.digital.ho.hocs.document.dto.GetDocumentsResponse;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.ISOLATED;
import static uk.gov.digital.ho.hocs.document.model.DocumentStatus.PENDING;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:afterTest.sql", config = @SqlConfig(transactionMode = ISOLATED), executionPhase = AFTER_TEST_METHOD)
@ActiveProfiles("test")
public class DocumentCreateIntegrationTest {
    public static final String INTEGRATION_TEST_DOCUMENT_TYPE = "INTEGRATION_TEST_DOCUMENT_TYPE";
    @LocalServerPort
    int port;

    private HttpHeaders headers;

    @Before
    public void setup() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void shouldCreateANewDocumentAndReturnUuid() {
        CreateDocumentRequest request = new CreateDocumentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Document",
                "test_doc.docx",
                INTEGRATION_TEST_DOCUMENT_TYPE,
                "PDF"
                );

        HttpEntity<DocumentData> httpEntity = new HttpEntity(request, headers);
        ResponseEntity<UUID> result = restTemplate.exchange(
                getBasePath() + "/document", HttpMethod.POST, httpEntity, UUID.class);

        assertThat(result.getBody()).isInstanceOf(UUID.class);
    }

    @Test
    public void shouldCreateANewDocumentAndGetByCase() {
        final UUID caseUuid = UUID.randomUUID();
        final CreateDocumentRequest request = new CreateDocumentRequest(
                caseUuid,
                "Test Document",
                "test_doc.docx",
                INTEGRATION_TEST_DOCUMENT_TYPE,
                "PDF"
        );

        final HttpEntity<DocumentData> httpPostEntity = new HttpEntity(request, headers);
        restTemplate.exchange(
                getBasePath() + "/document", HttpMethod.POST, httpPostEntity, UUID.class);

        final HttpEntity<DocumentData> httpGetEntity = new HttpEntity(headers);
        final ResponseEntity<GetDocumentsResponse> result = restTemplate.exchange(
                getBasePath() + "/document/reference/" + caseUuid,
                HttpMethod.GET,
                httpGetEntity,
                GetDocumentsResponse.class
        );

        final GetDocumentsResponse resultBody = result.getBody();

        assertThat(resultBody.getDocumentDtos().size()).isEqualTo(1);

        final DocumentDto firstDoc = resultBody.getDocumentDtos().stream().findFirst().get();

        assertThat(firstDoc.getType()).isEqualTo(INTEGRATION_TEST_DOCUMENT_TYPE);
        assertThat(firstDoc.getDisplayName()).isEqualTo("Test Document");
        assertThat(firstDoc.getStatus()).isEqualTo(PENDING);
        assertThat(firstDoc.getCreated()).isNotNull();
        assertThat(firstDoc.getDeleted()).isFalse();
        assertThat(firstDoc.getHasPdf()).isFalse();
    }

    @Test
    public void shouldCreateANewDocumentAndGetByCaseAndAction() {
        final UUID caseUuid = UUID.randomUUID();
        final UUID actionUUID = UUID.randomUUID();
        final CreateDocumentRequest request = new CreateDocumentRequest(
                caseUuid,
                actionUUID,
                "Test Document",
                "test_doc.docx",
                INTEGRATION_TEST_DOCUMENT_TYPE,
                "PDF"
        );

        final HttpEntity<DocumentData> httpPostEntity = new HttpEntity(request, headers);
        restTemplate.exchange(
                getBasePath() + "/document", HttpMethod.POST, httpPostEntity, UUID.class);

        final HttpEntity<DocumentData> httpGetEntity = new HttpEntity(headers);
        final ResponseEntity<GetDocumentsResponse> result = restTemplate.exchange(
                getBasePath()
                        + "/document/reference/"
                        + caseUuid
                        + "/actionDataUuid/"
                        + actionUUID
                        + "/type/"
                        + INTEGRATION_TEST_DOCUMENT_TYPE,
                HttpMethod.GET,
                httpGetEntity,
                GetDocumentsResponse.class
        );

        final GetDocumentsResponse resultBody = result.getBody();

        assertThat(resultBody.getDocumentDtos().size()).isEqualTo(1);

        final DocumentDto firstDoc = resultBody.getDocumentDtos().stream().findFirst().get();

        assertThat(firstDoc.getType()).isEqualTo(INTEGRATION_TEST_DOCUMENT_TYPE);
        assertThat(firstDoc.getDisplayName()).isEqualTo("Test Document");
        assertThat(firstDoc.getStatus()).isEqualTo(PENDING);
        assertThat(firstDoc.getCreated()).isNotNull();
        assertThat(firstDoc.getDeleted()).isFalse();
        assertThat(firstDoc.getHasPdf()).isFalse();
    }

    private String getBasePath() {
        return "http://localhost:" + port;
    }
}