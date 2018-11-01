package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;

public enum DocumentType {

    ORIGINAL("Original"),
    DRAFT("Draft"),

    TEMPLATE("TEMPLATE"),
    STANDARD_LINE("STANDARD_LINE");

    @Getter
    private String displayValue;

    DocumentType(String value) {
        displayValue = value;
    }
}
