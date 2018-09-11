package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;

public enum DocumentStatus {

    PENDING("Pending"),
    UPLOADED("Uploaded"),
    FAILED_VIRUS("Failed Virus Scan"),
    FAILED_CONVERSION("Failed PDF Conversion");

    @Getter
    private String displayValue;

    DocumentStatus(String value) {
        displayValue = value;
    }
}
