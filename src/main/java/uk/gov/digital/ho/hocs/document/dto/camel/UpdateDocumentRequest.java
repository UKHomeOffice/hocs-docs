package uk.gov.digital.ho.hocs.document.dto.camel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class UpdateDocumentRequest {
    private UUID uuid;
    private DocumentStatus status;
    private String fileLink;
    private String pdfLink;
}
