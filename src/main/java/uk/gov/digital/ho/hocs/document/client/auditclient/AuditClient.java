package uk.gov.digital.ho.hocs.document.client.auditclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import javax.json.Json;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.client.auditclient.dto.CreateAuditRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.value;
import static uk.gov.digital.ho.hocs.document.application.LogEvent.*;

@Slf4j
@Component
public class AuditClient {

    private final String auditQueue;
    private final String raisingService;
    private final String namespace;
    private final ProducerTemplate producerTemplate;
    private final ObjectMapper objectMapper;
    private final String EVENT_TYPE_HEADER ="event_type";
    private final RequestData requestData;

    @Autowired
    public AuditClient(ProducerTemplate producerTemplate,
                       @Value("${audit.queue}") String auditQueue,
                       @Value("${auditing.deployment.name}") String raisingService,
                       @Value("${auditing.deployment.namespace}") String namespace,
                       ObjectMapper objectMapper,
                       RequestData requestData) {
        this.producerTemplate = producerTemplate;
        this.auditQueue = auditQueue;
        this.raisingService = raisingService;
        this.namespace = namespace;
        this.objectMapper = objectMapper;
        this.requestData = requestData;
    }

    @Async
    public void createDocumentAudit(DocumentData documentData) {
        sendAuditMessage(documentData, EventType.DOCUMENT_CREATED);
    }

    @Async
    public void updateDocumentAudit(DocumentData documentData) {
        sendAuditMessage(documentData, EventType.DOCUMENT_UPDATED);
    }

    @Async
    public void deleteDocumentAudit(DocumentData documentData) {
        sendAuditMessage(documentData, EventType.DOCUMENT_DELETED);
    }

    @Retryable(maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.delay}"))
    private void sendAuditMessage(DocumentData documentData, EventType eventType) {
        CreateAuditRequest request = generateAuditRequest(documentData.getExternalReferenceUUID(),
                createAuditPayload(documentData),
                eventType.toString());
        try {
            producerTemplate.sendBodyAndHeaders(auditQueue, objectMapper.writeValueAsString(request), getQueueHeaders(eventType.toString()));
            log.info("Create audit of type {}, document UUID: {}, case UUID: {}, correlationID: {}, UserID: {}",
                    eventType,
                    documentData.getUuid(),
                    documentData.getExternalReferenceUUID(),
                    requestData.correlationId(),
                    requestData.userId(),
                    value(EVENT, AUDIT_EVENT_CREATED));
        } catch (Exception e) {
            log.error("Failed to create audit event for document UUID {} for reason {}", documentData.getUuid(), e, value(EVENT, AUDIT_FAILED));
        }
    }

    private String createAuditPayload(DocumentData documentData) {

        return Json.createObjectBuilder()
                .add("documentUUID", documentData.getUuid().toString())
                .add("documentTitle", documentData.getDisplayName())
                .add("documentType", documentData.getType().toString())
                .build()
                .toString();
    }

    private CreateAuditRequest generateAuditRequest(UUID caseUUID, String auditPayload, String eventType) {
        return new CreateAuditRequest(
                requestData.correlationId(),
                caseUUID,
                raisingService,
                auditPayload,
                namespace,
                LocalDateTime.now(),
                eventType,
                requestData.userId());
    }

    private Map<String, Object> getQueueHeaders(String eventType) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(EVENT_TYPE_HEADER, eventType);
        headers.put(RequestData.CORRELATION_ID_HEADER, requestData.correlationId());
        headers.put(RequestData.USER_ID_HEADER, requestData.userId());
        headers.put(RequestData.USERNAME_HEADER, requestData.username());
        headers.put(RequestData.GROUP_HEADER, requestData.groups());
        return headers;
    }
}