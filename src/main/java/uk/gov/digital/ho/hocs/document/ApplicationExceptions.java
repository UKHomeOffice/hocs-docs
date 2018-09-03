package uk.gov.digital.ho.hocs.document;

public interface ApplicationExceptions {

    class DocumentConversionException extends Exception {
        public DocumentConversionException(String document_failed_malware_check) {
        }
    }
    class MalwareCheckException extends Exception {
        public MalwareCheckException(String message) {
        }
    }
}
