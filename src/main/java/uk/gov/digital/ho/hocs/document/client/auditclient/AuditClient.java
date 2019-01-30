package uk.gov.digital.ho.hocs.document.client.auditclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.client.auditclient.dto.CreateAuditRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import javax.json.Json;

import java.time.LocalDateTime;
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

    public void createDocumentAudit(DocumentData documentData) {
        String auditPayload = Json.createObjectBuilder().add("documentUUID", documentData.getUuid().toString())
                .add("caseUUID", documentData.getExternalReferenceUUID().toString())
                .build().toString();
        CreateAuditRequest request = generateAuditRequest(auditPayload, EventType.DOCUMENT_CREATED.toString());

        try {
            producerTemplate.sendBody(auditQueue, objectMapper.writeValueAsString(request));
            log.info("Create audit for Create Document, document UUID: {}, case UUID: {}, correlationID: {}, UserID: {}",
                    documentData.getUuid(), documentData.getExternalReferenceUUID(), requestData.correlationId(), requestData.userId(), value(EVENT, AUDIT_EVENT_CREATED));
        } catch (Exception e) {
            log.error("Failed to create audit event for document UUID {} for reason {}", documentData.getUuid(), e, value(EVENT, AUDIT_FAILED));
        }
    }


    public void updateDocumentAudit(DocumentData documentData) {
        String auditPayload = Json.createObjectBuilder().add("documentUUID", documentData.getUuid().toString())
                .add("caseUUID", documentData.getExternalReferenceUUID().toString())
                .build().toString();
        CreateAuditRequest request = generateAuditRequest(auditPayload, EventType.DOCUMENT_UPDATED.toString());

        try {
            producerTemplate.sendBody(auditQueue, objectMapper.writeValueAsString(request));
            log.info("Create audit for Update Document, document UUID: {}, case UUID: {}, correlationID: {}, UserID: {}",
                    documentData.getUuid(), documentData.getExternalReferenceUUID(), requestData.correlationId(), requestData.userId(), value(EVENT, AUDIT_EVENT_CREATED));
        } catch (Exception e) {
            log.error("Failed to create audit event for document UUID {} for reason {}", documentData.getUuid(), e, value(EVENT, AUDIT_FAILED));
        }
    }

    public void downloadDocumentAudit(DocumentData documentData) {
        String auditPayload = Json.createObjectBuilder().add("documentUUID", documentData.getUuid().toString())
                .add("caseUUID", documentData.getExternalReferenceUUID().toString())
                .build().toString();
        CreateAuditRequest request = generateAuditRequest(auditPayload, EventType.DOCUMENT_DOWNLOADED.toString());

        try {
            producerTemplate.sendBody(auditQueue, objectMapper.writeValueAsString(request));
            log.info("Create audit for Download Document, document UUID: {}, case UUID: {}, correlationID: {}, UserID: {}",
                    documentData.getUuid(), documentData.getExternalReferenceUUID(), requestData.correlationId(), requestData.userId(), value(EVENT, AUDIT_EVENT_CREATED));
        } catch (Exception e) {
            log.error("Failed to create audit event for document UUID {} for reason {}", documentData.getUuid(), e, value(EVENT, AUDIT_FAILED));
        }
    }


    public void deleteDocumentAudit(DocumentData documentData) {
        String auditPayload = Json.createObjectBuilder().add("documentUUID", documentData.getUuid().toString())
                .add("caseUUID", documentData.getExternalReferenceUUID().toString())
                .build().toString();
        CreateAuditRequest request = generateAuditRequest(auditPayload, EventType.DOCUMENT_DELETED.toString());

        try {
            producerTemplate.sendBody(auditQueue, objectMapper.writeValueAsString(request));
            log.info("Create audit for Delete Document, document UUID: {}, case UUID: {}, correlationID: {}, UserID: {}",
                    documentData.getUuid(), documentData.getExternalReferenceUUID(), requestData.correlationId(), requestData.userId(), value(EVENT, AUDIT_EVENT_CREATED));
        } catch (Exception e) {
            log.error("Failed to create audit event for document UUID {} for reason {}", documentData.getUuid(), e, value(EVENT, AUDIT_FAILED));
        }
    }

    private CreateAuditRequest generateAuditRequest(String auditPayload, String eventType) {
        return new CreateAuditRequest(
                requestData.correlationId(),
                raisingService,
                auditPayload,
                namespace,
                LocalDateTime.now(),
                eventType,
                requestData.userId());
    }

}