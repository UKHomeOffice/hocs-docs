package uk.gov.digital.ho.hocs.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GetDocumentsResponse {

    @JsonProperty("documents")
    private Set<DocumentDto> documentDtos;

    public static GetDocumentsResponse from(Set<DocumentData> documents) {
        Set<DocumentDto> documentDtoResponse = documents.stream().map(DocumentDto::from).collect(Collectors.toSet());

        return new GetDocumentsResponse(documentDtoResponse);
    }

}