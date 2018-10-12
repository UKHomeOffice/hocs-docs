package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentResponse;
import uk.gov.digital.ho.hocs.document.dto.DocumentDto;
import uk.gov.digital.ho.hocs.document.dto.GetDocumentsResponse;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
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

    @PostMapping(value = "/document", consumes = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CreateDocumentResponse> createDocument(@RequestBody CreateDocumentRequest request) {
        DocumentData documentData = documentDataService.createDocument(request.getCaseUUID(),request.getName(), request.getType());
        return ResponseEntity.ok(CreateDocumentResponse.from(documentData));
    }

    @GetMapping(value = "/document/case/{caseUUID}", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<GetDocumentsResponse> getDocumentsForCase(@PathVariable UUID caseUUID) {
        Set<DocumentData> documents = documentDataService.getDocumentsByReference(caseUUID);
        return ResponseEntity.ok(GetDocumentsResponse.from(documents));
    }

    @GetMapping(value = "/document/case/{caseUUID}/{type}", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<GetDocumentsResponse> getDocumentsForCaseForType(@PathVariable UUID caseUUID, @PathVariable String type) {
        Set<DocumentData> documents = documentDataService.getDocumentsByReferenceForType(caseUUID,type);
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + document.getFilename())
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

}