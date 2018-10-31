package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;

import java.util.Arrays;

public enum ManagedDocumentType {

    TEMPLATE("TEMPLATE"),
    STANDARD_LINE("STANDARD_LINE");

    @Getter
    private String displayValue;

    ManagedDocumentType(String value) {
        displayValue = value;
    }

}
