package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.digital.ho.hocs.document.dto.*;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentData;

import java.util.Set;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@Slf4j
@RestController
class ManagedDocumentDataResource {

    private final ManagedDocumentDataService managedDocumentDataService;

    @Autowired
    public ManagedDocumentDataResource(ManagedDocumentDataService managedDocumentDataService) {
        this.managedDocumentDataService = managedDocumentDataService;
    }

    @PostMapping(value = "/managedDocument", consumes = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CreateManagedDocumentResponse> createDocument(@RequestBody CreateManagedDocumentRequest request) {
        ManagedDocumentData documentData = managedDocumentDataService.createManagedDocument(request.getExternalReferenceUUID(), request.getName(), request.getType());
        return ResponseEntity.ok(CreateManagedDocumentResponse.from(documentData));
    }

    @GetMapping(value = "/managedDocument/reference/{referenceUUID}", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<GetManagedDocumentsResponse> getDocumentsByReference(@PathVariable UUID referenceUUID) {
        Set<ManagedDocumentData> documents = managedDocumentDataService.getManagedDocumentsByReference(referenceUUID);
        return ResponseEntity.ok(GetManagedDocumentsResponse.from(documents));
    }

    @GetMapping(value = "/managedDocument/{documentUUID}", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ManagedDocumentDto> getDocument(@PathVariable UUID documentUUID) {
        ManagedDocumentData document = managedDocumentDataService.getManagedDocumentData(documentUUID);
        return ResponseEntity.ok(ManagedDocumentDto.from(document));
    }

    @DeleteMapping(value = "/managedDocument/{documentUUID}")
    public ResponseEntity<DocumentDto> deleteDocument(@PathVariable UUID documentUUID) {
        managedDocumentDataService.deleteManagedDocument(documentUUID);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/managed/{documentUUID}/original", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ByteArrayResource> getDocumentFile(@PathVariable UUID documentUUID) {
        S3Document document = managedDocumentDataService.getDocumentFile(documentUUID);

        ByteArrayResource resource = new ByteArrayResource(document.getData());
        MediaType mediaType = MediaType.valueOf(document.getMimeType());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + document.getFilename())
                .contentType(mediaType)
                .contentLength(document.getData().length)
                .body(resource);
    }
}