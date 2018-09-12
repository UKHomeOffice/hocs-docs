package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentResponse;
import uk.gov.digital.ho.hocs.document.dto.Document;
import uk.gov.digital.ho.hocs.document.dto.GetDocumentsResponse;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.util.Set;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@Slf4j
@RestController
class DocumentDataResource {

    private final DocumentDataService documentDataService;

    @Autowired
    public DocumentDataResource(DocumentDataService documentDataService) {
        this.documentDataService = documentDataService;
    }

    @PostMapping(value = "/case/{caseUUID}/document", consumes = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CreateDocumentResponse> createDocument(@PathVariable UUID caseUUID,
                                                                 @RequestBody CreateDocumentRequest request) {
        DocumentData documentData = documentDataService.createDocument(caseUUID, request.getName(), request.getType());
        return ResponseEntity.ok(CreateDocumentResponse.from(documentData));
    }

    @GetMapping(value = "/case/{caseUUID}/document", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<GetDocumentsResponse> getDocumentsForCase(@PathVariable UUID caseUUID) {
        Set<DocumentData> documents = documentDataService.getDocumentsForCase(caseUUID);
        return ResponseEntity.ok(GetDocumentsResponse.from(documents));
    }

    @GetMapping(value = "/case/{caseUUID}/document/{documentUUID}", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Document> getDocumentResourceLocation(@PathVariable UUID caseUUID, @PathVariable UUID documentUUID) {
        DocumentData document = documentDataService.getDocumentData(documentUUID);
        return ResponseEntity.ok(Document.from(document));
    }

    @DeleteMapping(value = "/case/{caseUUID}/document/{documentUUID}")
    public ResponseEntity<Document> deleteDocument(@PathVariable UUID caseUUID, @PathVariable UUID documentUUID) {
        documentDataService.deleteDocument(documentUUID);
        return ResponseEntity.ok().build();
    }
}