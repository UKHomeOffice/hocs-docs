package uk.gov.digital.ho.hocs.document.client.documentclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.dto.camel.ProcessDocumentRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;

import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.value;
import static uk.gov.digital.ho.hocs.document.application.LogEvent.*;

@Slf4j
@Component
public class DocumentClient {

    private final String documentQueue;
    private final ProducerTemplate producerTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public DocumentClient(ProducerTemplate producerTemplate,
                          @Value("${docs.queue}") String documentQueue,
                          ObjectMapper objectMapper){
        this.producerTemplate = producerTemplate;
        this.documentQueue = documentQueue;
        this.objectMapper = objectMapper;
    }

    @Async
    public void processDocument(UUID documentUUID, String fileLocation, boolean convertToPdf, UUID externalReferenceUUID) {
        ProcessDocumentRequest request = new ProcessDocumentRequest(documentUUID, fileLocation, convertToPdf, externalReferenceUUID);
        try {
            sendMessage(objectMapper.writeValueAsString(request));
            log.info("Processed Document {}", documentUUID, value(EVENT, DOCUMENT_CLIENT_PROCESS_SUCCESS));
        } catch (JsonProcessingException e) {
            throw new ApplicationExceptions.EntityCreationException(String.format("Could not process Document: %s", e.toString()), DOCUMENT_CLIENT_FAILURE);
        }
    }

    @Retryable(maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.delay}"))
    private void sendMessage(String message) {
        producerTemplate.sendBody(documentQueue, message);
    }
}
