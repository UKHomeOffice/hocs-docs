package uk.gov.digital.ho.hocs.document.exception;

import uk.gov.digital.ho.hocs.document.application.LogEvent;

public interface ApplicationExceptions {

    class DocumentConversionException extends Exception {
        private final LogEvent event;

        public DocumentConversionException(String message, LogEvent event) {
            super(message);
            this.event = event;
        }

        public LogEvent getEvent() {
            return event;
        }
    }

    class MalwareCheckException extends Exception {
        private final LogEvent event;

        public MalwareCheckException(String message, LogEvent event) {
            super(message);
            this.event = event;
        }

        public LogEvent getEvent() {
            return event;
        }
    }

    class EntityCreationException extends RuntimeException {
        private final LogEvent event;

        public EntityCreationException(String msg, LogEvent event) {
            super(msg);
            this.event = event;
        }

        public LogEvent getEvent() {
            return event;
        }
    }

    class EntityNotFoundException extends RuntimeException {

        private final LogEvent event;

        public EntityNotFoundException(String msg, LogEvent event) {
            super(msg);
            this.event = event;
        }

        public LogEvent getEvent() {
            return event;
        }
    }

    class S3Exception extends RuntimeException {

        private final LogEvent event;

        public S3Exception(String msg, LogEvent event, Exception cause) {
            super(msg, cause);
            this.event = event;
        }

        public LogEvent getEvent() {
            return event;
        }

    }

    class ResourceException extends RuntimeException {
        private final LogEvent event;

        ResourceException(String msg, LogEvent event, Object... args) {
            super(String.format(msg, args));
            this.event = event;
        }

        public LogEvent getEvent() {
            return event;
        }
    }

}