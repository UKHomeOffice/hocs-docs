package uk.gov.digital.ho.hocs.document.exception;

public interface ApplicationExceptions {

    class DocumentConversionException extends Exception {
        public DocumentConversionException(String message) {
        }
    }

    class MalwareCheckException extends Exception {
        public MalwareCheckException(String message) {
        }
    }

    class EntityCreationException extends RuntimeException {

        public EntityCreationException(String msg, Object... args) {
            super(String.format(msg, args));
        }
    }

    class EntityNotFoundException extends RuntimeException {

        public EntityNotFoundException(String msg, Object... args) {
            super(String.format(msg, args));
        }
    }
}