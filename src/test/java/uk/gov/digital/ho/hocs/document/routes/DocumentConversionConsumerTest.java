package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.camel.DocumentConversionRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentConversionConsumerTest extends CamelTestSupport {

    @Mock
    S3DocumentService s3BucketService;

    private final String endpoint = "direct:convertdocument";
    private final String dlq = "mock:cs-dev-document-sqs-dlq";
    private final String toEndpoint = "mock:updaterecord";
    private final String conversionService = "mock:conversion-service";
    private UUID documentUUID = UUID.randomUUID();
    private DocumentConversionRequest request = new DocumentConversionRequest(UUID.randomUUID(),"sample.docx", "caseUUID", "docx");


    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
      return new DocumentConversionConsumer(s3BucketService, conversionService, dlq, 0,0,0,toEndpoint);
    }


    @Test
    public void shouldAddDocumentToDocumentServiceQueueOnSuccess() throws Exception {
        MockEndpoint mockConversionService = mockConversionService();
        when(s3BucketService.getFileFromTrustedS3(any())).thenReturn(getTestDocument());
        when(s3BucketService.uploadFile(any())).thenReturn(getTestDocument());
        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedMessageCount(1);
        template.sendBody(endpoint,request);
        mockEndpoint.assertIsSatisfied();
        mockConversionService.assertIsSatisfied();
    }

    @Test
    public void shouldAddMessagetoDLQAndNotCallConverstionServiceOnS3Error() throws Exception {

        MockEndpoint mockConversionService = mockConversionService();
        when(s3BucketService.getFileFromTrustedS3(any())).thenThrow(new IOException());
        getMockEndpoint(dlq).expectedMessageCount(1);
        template.sendBody(endpoint,request);
        getMockEndpoint(dlq).assertIsSatisfied();
        mockConversionService.assertIsNotSatisfied();
    }

    @Test
    public void shouldAddMessagetoDLQWhenConversionServiceFails() throws Exception {
        when(s3BucketService.getFileFromTrustedS3(any())).thenReturn(getTestDocument());
        MockEndpoint mockConversionService = mockFailedConversionService();
        getMockEndpoint(dlq).expectedMessageCount(1);
        template.sendBody(endpoint,request);
        getMockEndpoint(dlq).assertIsSatisfied();
        mockConversionService.assertIsSatisfied();
    }

    @Test
    public void shouldAddPropertiesToExchange() throws Exception {
        when(s3BucketService.getFileFromTrustedS3(any())).thenReturn(getTestDocument());
        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedPropertyReceived("caseUUID", "caseUUID");
        template.sendBody(endpoint,request);
        mockEndpoint.assertIsSatisfied();
    }

    private MockEndpoint mockConversionService() {
        MockEndpoint mock = getMockEndpoint("mock:conversion-service?throwExceptionOnFailure=false&useSystemProperties=true");
        mock.expectedMessageCount(1);
        mock.whenAnyExchangeReceived(exchange -> {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            exchange.getIn().setBody(getPDFDocument());
        });
        return mock;
    }

    private MockEndpoint mockFailedConversionService() {
        MockEndpoint mock = getMockEndpoint("mock:conversion-service?throwExceptionOnFailure=false&useSystemProperties=true");
        mock.expectedMessageCount(1);
        mock.whenAnyExchangeReceived(exchange -> {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            exchange.getIn().setBody(null);
        });
        return mock;
    }

    private S3Document getTestDocument() throws URISyntaxException, IOException {
        byte[] data = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
        return new S3Document("somecaseUUID/UUID.pdf", "sample.docx", data, "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }
}