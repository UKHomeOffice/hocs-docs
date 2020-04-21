package uk.gov.digital.ho.hocs.document;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;

import java.io.ByteArrayInputStream;

import static org.apache.camel.builder.Builder.header;

public class HttpProcessors {

    private HttpProcessors() {
    }

    public static Processor buildMultipartEntity() {
        return exchange -> {
            S3Document response = exchange.getIn().getBody(S3Document.class);
            ContentBody content = new InputStreamBody(new ByteArrayInputStream(response.getData()), response.getFilename());
            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addPart("file", content)
                    .addTextBody("name", response.getFilename());
            exchange.getOut().setBody(multipartEntityBuilder.build());
        };
    }

    public static final Predicate validateHttpResponse = header(Exchange.HTTP_RESPONSE_CODE).isLessThan(300);

}
