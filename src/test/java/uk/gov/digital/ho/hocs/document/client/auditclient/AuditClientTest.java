package uk.gov.digital.ho.hocs.document.client.auditclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.ProducerTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.application.HocsDocumentServiceConfiguration;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.client.auditclient.dto.CreateAuditRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class AuditClientTest {

    @Mock
    RequestData requestData;

    @Mock
    ProducerTemplate producerTemplate;

    @Captor
    ArgumentCaptor jsonCaptor;

    @Captor
    ArgumentCaptor<HashMap<String,Object>> headerCaptor;

    private HocsDocumentServiceConfiguration configuration = new HocsDocumentServiceConfiguration();
    private ObjectMapper mapper;

    private AuditClient auditClient;
    private String auditQueue ="audit-queue";

    @Before
    public void setUp() {
        when(requestData.correlationId()).thenReturn(randomUUID().toString());
        when(requestData.userId()).thenReturn("some user id");
        when(requestData.groups()).thenReturn("some groups");
        when(requestData.username()).thenReturn("some username");
        mapper = configuration.initialiseObjectMapper();
        auditClient = new AuditClient(producerTemplate, auditQueue,"hocs-docs","namespace", mapper, requestData);

    }

    @Test
    public void shouldSetHeaders()  {
        UUID caseUUID = UUID.randomUUID();
        DocumentData docData = new DocumentData(caseUUID, DocumentType.ORIGINAL, "a document");
        Map<String, Object> expectedHeaders = new HashMap<String, Object>(){{
                put("event_type", EventType.DOCUMENT_CREATED.toString());
                put(RequestData.CORRELATION_ID_HEADER, requestData.correlationId());
                put(RequestData.USER_ID_HEADER, requestData.userId());
                put(RequestData.USERNAME_HEADER, requestData.username());
                put(RequestData.GROUP_HEADER, requestData.groups());}};

        auditClient.createDocumentAudit(docData);
        verify(producerTemplate, times(1)).sendBodyAndHeaders(eq(auditQueue), any(), headerCaptor.capture());
        Map headers = headerCaptor.getValue();

        assertThat(headers).containsAllEntriesOf(expectedHeaders);
    }

    @Test
    public void shouldSetAuditFields() throws IOException {
        UUID caseUUID = UUID.randomUUID();
        DocumentData docData = new DocumentData(caseUUID, DocumentType.ORIGINAL,"a document");

        auditClient.createDocumentAudit(docData);
        verify(producerTemplate, times(1)).sendBodyAndHeaders(eq(auditQueue), jsonCaptor.capture(), any());
        CreateAuditRequest request = mapper.readValue((String)jsonCaptor.getValue(), CreateAuditRequest.class);
        assertThat(request.getType()).isEqualTo(EventType.DOCUMENT_CREATED.toString());
        assertThat(request.getCaseUUID()).isEqualTo(docData.getExternalReferenceUUID());
        assertThat(request.getCorrelationID()).isEqualTo(requestData.correlationId());
        assertThat(request.getNamespace()).isEqualTo("namespace");
        assertThat(request.getRaisingService()).isEqualTo("hocs-docs");
        assertThat(request.getUserID()).isEqualTo(requestData.userId());
    }

    @Test
    public void shouldNotThrowExceptionOnFailure() {
        UUID caseUUID = UUID.randomUUID();
        DocumentData docData = new DocumentData(caseUUID, DocumentType.ORIGINAL, "a document");
        doThrow(new RuntimeException("An error occurred")).when(producerTemplate).sendBodyAndHeaders(eq(auditQueue), jsonCaptor.capture(), any());
        assertThatCode(() -> { auditClient.createDocumentAudit(docData);}).doesNotThrowAnyException();
        verify(producerTemplate, times(1)).sendBodyAndHeaders(eq(auditQueue), jsonCaptor.capture(), any());
    }

    @Test
    public void createDocumentAudit() throws IOException {
        UUID caseUUID = UUID.randomUUID();
        DocumentData docData = new DocumentData(caseUUID, DocumentType.ORIGINAL, "a document");
        auditClient.createDocumentAudit(docData);
        verify(producerTemplate, times(1)).sendBodyAndHeaders(eq(auditQueue), jsonCaptor.capture(), any());
        CreateAuditRequest request = mapper.readValue((String)jsonCaptor.getValue(), CreateAuditRequest.class);
        assertThat(request.getType()).isEqualTo(EventType.DOCUMENT_CREATED.toString());
        assertThat(request.getCaseUUID()).isEqualTo(docData.getExternalReferenceUUID());
    }
}