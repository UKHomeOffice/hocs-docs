package uk.gov.digital.ho.hocs.document.client.auditclient;

import org.apache.camel.impl.DefaultDebugger;

public enum EventType {
    DOCUMENT_CREATED,
    DOCUMENT_UPDATED,
    DOCUMENT_DELETED,
    DOCUMENT_DOWNLOADED;
}