package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import uk.gov.digital.ho.hocs.document.dto.CopyDocumentsRequest;
import uk.gov.digital.ho.hocs.document.dto.CreateDocumentRequest;
import uk.gov.digital.ho.hocs.document.dto.DocumentDto;
import uk.gov.digital.ho.hocs.document.dto.GetDocumentsResponse;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
class DocumentDataResource {

    private final DocumentDataService documentDataService;

    @Autowired
    public DocumentDataResource(DocumentDataService documentDataService) {
        this.documentDataService = documentDataService;
    }

    @PostMapping(value = "/document", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createDocument(@RequestBody CreateDocumentRequest request) {

        DocumentData documentData = documentDataService.createDocument(request);
        return ResponseEntity.ok(documentData.getUuid());
    }

    @PostMapping(value = "/documents/copy", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> copyDocuments(@RequestBody CopyDocumentsRequest request) {
        documentDataService.copyDocuments(request);
        return ResponseEntity.ok(null);
    }

    @GetMapping(value = "/document/reference/{externalReferenceUUID}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GetDocumentsResponse> getDocumentsForCaseForType(@PathVariable UUID externalReferenceUUID, @RequestParam(name = "type", required = false) String type) {
        Set<DocumentData> documents = documentDataService.getDocumentsByReference(externalReferenceUUID, type);
        return ResponseEntity.ok(GetDocumentsResponse.from(documents));
    }

    @GetMapping(
            value = "/document/reference/{externalReferenceUUID}/actionDataUuid/{actionDataUuid}/type/{type}",
            produces = APPLICATION_JSON_VALUE
    )
    public ResponseEntity<GetDocumentsResponse> getDocumentsForCaseAndAction(
            @PathVariable UUID externalReferenceUUID,
            @PathVariable UUID actionDataUuid,
            @PathVariable String type) {
        Set<DocumentData> documents = documentDataService.getDocumentsByReferenceAndActionDataUuid(externalReferenceUUID, actionDataUuid, type);

        return ResponseEntity.ok(GetDocumentsResponse.from(documents));
    }

    @GetMapping(value = "/document/{documentUUID}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentDto> getDocumentResourceLocation(@PathVariable UUID documentUUID) {
        DocumentData document = documentDataService.getDocumentData(documentUUID);
        return ResponseEntity.ok(DocumentDto.from(document));
    }

    @DeleteMapping(value = "/document/{documentUUID}")
    public ResponseEntity<DocumentDto> deleteDocument(@PathVariable UUID documentUUID) {
        documentDataService.deleteDocument(documentUUID);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/document/{documentUUID}/file", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ByteArrayResource> getDocumentFile(@PathVariable UUID documentUUID) {
        S3Document document = documentDataService.getDocumentFile(documentUUID);

        return generateFileResponseEntity(document);
    }

    @GetMapping(value = "/document/{documentUUID}/pdf", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ByteArrayResource> getDocumentPdf(@PathVariable UUID documentUUID) {
        S3Document document = documentDataService.getDocumentPdf(documentUUID);

        return generateFileResponseEntity(document);
    }

    private static ResponseEntity<ByteArrayResource> generateFileResponseEntity(S3Document document) {
        ByteArrayResource resource = new ByteArrayResource(document.getData());

        var response = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + document.getOriginalFilename());

        if (StringUtils.hasText(document.getMimeType())) {
            MediaType mediaType = MediaType.valueOf(document.getMimeType());
            response = response.contentType(mediaType);
        }

        return response.contentLength(document.getData().length)
                .body(resource);
    }

    @GetMapping(value = "/document/{documentUUID}/name", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDocumentName(@PathVariable UUID documentUUID) {
        DocumentData documentData = documentDataService.getDocumentData(documentUUID);
        // 'ApplicationExceptions.EntityNotFoundException' thrown in getDocumentData if null
        return ResponseEntity.ok(documentData.getDisplayName());
    }

}
