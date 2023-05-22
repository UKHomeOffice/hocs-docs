package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;

public enum DocumentStatus {

    PENDING("Pending"),
    UPLOADED("Uploaded"),
    FAILED_VIRUS("Malware Found"),
    FAILED_MALWARE_SCAN("Malware Scan Failed"),
    FAILED_CONVERSION("Failed PDF Conversion"),
    AWAITING_MALWARE_SCAN("Awaiting Malware Scan"),
    AWAITING_CONVERSION("Awaiting Document Conversion");

    @Getter
    private String displayValue;

    DocumentStatus(String value) {
        displayValue = value;
    }
}
