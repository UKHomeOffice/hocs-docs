package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.DocumentDto;
import uk.gov.digital.ho.hocs.document.dto.GetDocumentsResponse;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.util.HashSet;
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

    @PostMapping(value = "/document", consumes = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<UUID> createDocument(@RequestBody CreateDocumentRequest request) {
        DocumentData documentData = documentDataService.createDocument(request.getExternalReferenceUUID(),request.getName(), request.getFileLink(), request.getType());
        return ResponseEntity.ok(documentData.getUuid());
    }

    @GetMapping(value = "/document/reference/{externalReferenceUUID}", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<GetDocumentsResponse> getDocumentsForCaseForType(@PathVariable UUID externalReferenceUUID, @RequestParam(name = "type", required = false) String type) {
        Set<DocumentData> documents = new HashSet<>();
        if(type == null) {
            documents.addAll(documentDataService.getDocumentsByReference(externalReferenceUUID));
        } else {
           documents.addAll(documentDataService.getDocumentsByReferenceForType(externalReferenceUUID, type));
        }
        return ResponseEntity.ok(GetDocumentsResponse.from(documents));
    }

    @GetMapping(value = "/document/{documentUUID}", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DocumentDto> getDocumentResourceLocation(@PathVariable UUID documentUUID) {
        DocumentData document = documentDataService.getDocumentData(documentUUID);
        return ResponseEntity.ok(DocumentDto.from(document));
    }

    @DeleteMapping(value = "/document/{documentUUID}")
    public ResponseEntity<DocumentDto> deleteDocument(@PathVariable UUID documentUUID) {
        documentDataService.deleteDocument(documentUUID);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/document/{documentUUID}/file", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ByteArrayResource> getDocumentFile(@PathVariable UUID documentUUID) {
        S3Document document = documentDataService.getDocumentFile(documentUUID);

        ByteArrayResource resource = new ByteArrayResource(document.getData());
        MediaType mediaType = MediaType.valueOf(document.getMimeType());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + document.getOriginalFilename())
                .contentType(mediaType)
                .contentLength(document.getData().length)
                .body(resource);
    }

    @GetMapping(value = "/document/{documentUUID}/pdf", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ByteArrayResource> getDocumentPdf(@PathVariable UUID documentUUID) {
        S3Document document = documentDataService.getDocumentPdf(documentUUID);

        ByteArrayResource resource = new ByteArrayResource(document.getData());
        MediaType mediaType = MediaType.valueOf(document.getMimeType());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + document.getOriginalFilename())
                .contentType(mediaType)
                .contentLength(document.getData().length)
                .body(resource);
    }

    @GetMapping(value = "/document/{documentUUID}/name", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getDocumentName(@PathVariable UUID documentUUID) {
        DocumentData documentData = documentDataService.getDocumentData(documentUUID);
        // 'ApplicationExceptions.EntityNotFoundException' thrown in getDocumentData if null
        return ResponseEntity.ok(documentData.getDisplayName());
    }

}