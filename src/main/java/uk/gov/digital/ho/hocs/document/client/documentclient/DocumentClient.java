package uk.gov.digital.ho.hocs.document.client.documentclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.client.documentclient.dto.ProcessDocumentRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.value;
import static uk.gov.digital.ho.hocs.document.application.LogEvent.*;

@Slf4j
@Component
public class DocumentClient {

    private final String documentQueue;

    private final ProducerTemplate producerTemplate;

    private final ObjectMapper objectMapper;

    private final RequestData requestData;

    @Autowired
    public DocumentClient(ProducerTemplate producerTemplate,
                          @Value("${docs.queue}") String documentQueue,
                          ObjectMapper objectMapper,
                          RequestData requestData) {
        this.producerTemplate = producerTemplate;
        this.documentQueue = documentQueue;
        this.objectMapper = objectMapper;
        this.requestData = requestData;
    }

    public void processDocument(UUID documentUUID, String fileLocation, String convertTo) {
        ProcessDocumentRequest request = new ProcessDocumentRequest(documentUUID, fileLocation, convertTo);
        try {
            producerTemplate.sendBodyAndHeaders(documentQueue, objectMapper.writeValueAsString(request),
                getQueueHeaders());
            log.info("Processed Document {}", documentUUID, value(EVENT, DOCUMENT_CLIENT_PROCESS_SUCCESS));
        } catch (JsonProcessingException e) {
            throw new ApplicationExceptions.EntityCreationException(String.format("Could not process Document: %s", e),
                DOCUMENT_CLIENT_FAILURE);
        }
    }

    private Map<String, Object> getQueueHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(RequestData.CORRELATION_ID_HEADER, requestData.correlationId());
        headers.put(RequestData.USER_ID_HEADER, requestData.userId());
        headers.put(RequestData.USERNAME_HEADER, requestData.username());
        headers.put(RequestData.GROUP_HEADER, requestData.groups());
        return headers;
    }

}
