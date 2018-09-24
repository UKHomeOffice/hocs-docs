package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.digital.ho.hocs.document.model.ManagedDocumentData;

import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GetManagedDocumentsResponse {

    @JsonProperty("documents")
    private Set<ManagedDocumentDto> documentDtos;

    public static GetManagedDocumentsResponse from(Set<ManagedDocumentData> documents) {
        Set<ManagedDocumentDto> documentDtoResponse = documents
                .stream()
                .map(ManagedDocumentDto::from)
                .collect(Collectors.toSet());

        return new GetManagedDocumentsResponse(documentDtoResponse);
    }
}