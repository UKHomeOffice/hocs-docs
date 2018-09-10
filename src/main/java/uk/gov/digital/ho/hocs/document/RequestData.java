package uk.gov.digital.ho.hocs.document;

import org.apache.camel.Processor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class RequestData {


    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String USER_ID_HEADER = "X-Auth-Userid";
    private static final String USERNAME_HEADER = "X-Auth-Username";

    public String correlationId() {
        return MDC.get(CORRELATION_ID_HEADER);
    }
    public String userId() { return MDC.get(USER_ID_HEADER); }
    public String username() { return MDC.get(USERNAME_HEADER); }

    public static Processor transferHeadersToMDC() {
        return ex -> {
            MDC.put(CORRELATION_ID_HEADER, ex.getIn().getHeader(CORRELATION_ID_HEADER, String.class));
            MDC.put(USER_ID_HEADER, ex.getIn().getHeader(USER_ID_HEADER, String.class));
            MDC.put(USERNAME_HEADER, ex.getIn().getHeader(USERNAME_HEADER, String.class));
        };
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.equals("");
    }

}
