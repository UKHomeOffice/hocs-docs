package uk.gov.digital.ho.hocs.document;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.dto.camel.UpdateDocumentRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

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

    public static Processor generateDocumentUpdateRequest() {
        return exchange -> {
            UUID documentUUID = UUID.fromString(exchange.getProperty("uuid").toString());
            DocumentStatus status = DocumentStatus.valueOf(exchange.getProperty("status").toString());
            String pdfFileLink = Optional.ofNullable(exchange.getProperty("pdfFilename")).orElse(null).toString();
            String fileLink = Optional.ofNullable(exchange.getProperty("filename")).orElse(null).toString();
            exchange.getOut().setBody(new UpdateDocumentRequest(documentUUID, status, fileLink ,pdfFileLink));
        };
    }


    public static Predicate validateHttpResponse = header(Exchange.HTTP_RESPONSE_CODE).isLessThan(300);


}
