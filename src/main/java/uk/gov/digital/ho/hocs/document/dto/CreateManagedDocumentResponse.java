package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentData;

import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CreateManagedDocumentResponse {

    @JsonProperty("uuid")
    private final UUID uuid;

    public static CreateManagedDocumentResponse from(ManagedDocumentData documentData) {
        return new CreateManagedDocumentResponse(documentData.getUuid());
    }
}
