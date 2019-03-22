package uk.gov.digital.ho.hocs.document.client.auditclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.application.RestHelper;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentType;


import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditClientIntegrationTest extends CamelTestSupport {

    @Mock
    RequestData requestData;

    @Mock
    RestHelper restHelper;

    private final String toEndpoint = "mock:audit-queue";

    ObjectMapper mapper = new ObjectMapper();

    private AuditClient auditClient;


    @Before
    public void setup() {
        when(requestData.correlationId()).thenReturn(UUID.randomUUID().toString());
        when(requestData.userId()).thenReturn("some user");
        auditClient = new AuditClient(template, toEndpoint,"hocs-docs","namespace", mapper, requestData);
    }

    @Test
    public void shouldPutMessageOnAuditQueue() throws InterruptedException {
        UUID caseUUID = UUID.randomUUID();
        DocumentData docData = new DocumentData(caseUUID, DocumentType.ORIGINAL, "a document");
        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        auditClient.createDocumentAudit(docData);
        mockEndpoint.assertIsSatisfied();
        mockEndpoint.expectedBodyReceived().body().convertToString().contains(String.format("\"\"documentUUID\"\":\"%s\"", docData.getUuid().toString()));
    }

}