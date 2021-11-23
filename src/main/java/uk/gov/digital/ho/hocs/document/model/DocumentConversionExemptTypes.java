package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;

public enum DocumentConversionExemptTypes {

    TEMPLATE("TEMPLATE"),
    STANDARD_LINE("STANDARD_LINE");

    @Getter
    private String displayValue;

    DocumentConversionExemptTypes(String value) {
        displayValue = value;
    }

}
