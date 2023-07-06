package uk.gov.digital.ho.hocs.document.dto.camel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("squid:S1068")
@AllArgsConstructor
@Getter
public class S3DocumentMetaData {

    private final String filename;

    private final String originalFilename;

    private final String fileType;

    private final String mimeType;

}
