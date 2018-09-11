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
import uk.gov.digital.ho.hocs.document.model.Document;
import uk.gov.digital.ho.hocs.document.dto.DocumentConversionRequest;
import uk.gov.digital.ho.hocs.document.model.UploadDocument;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentConversionConsumerTest extends CamelTestSupport {

    @Mock
    S3DocumentService s3BucketService;

    private final String endpoint = "direct:convertdocument";
    private final String dlq = "mock:cs-dev-document-sqs-dlq";
    private final String toEndpoint = "mock:uploadtrustedfile";
    private final String documentServiceEndpoint = "mock:updaterecord";
    private final String conversionService = "mock:conversion-service";

    private DocumentConversionRequest request = new DocumentConversionRequest("sample.docx", "caseUUID", "docx");


    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
      return new DocumentConversionConsumer(s3BucketService, conversionService, dlq, 0,0,0,toEndpoint, documentServiceEndpoint);
    }

    @Test
    public void shouldCallS3CopyDocument() throws Exception {
        when(s3BucketService.copyToTrustedBucket(any())).thenReturn(getTestDocument());
        MockEndpoint mockConversionService = mockConversionService();
        template.sendBody(endpoint, request);
        verify(s3BucketService, times(1)).copyToTrustedBucket(request);
        mockConversionService.assertIsSatisfied();
    }

    @Test
    public void shouldAddDocumentToCaseQueueOnSuccess() throws Exception {
        Document document = getTestDocument();
        when(s3BucketService.copyToTrustedBucket(any())).thenReturn(document);
        MockEndpoint mockConversionService = mockConversionService();

        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedMessageCount(1);
        template.sendBody(endpoint,request);
        mockEndpoint.assertIsSatisfied();
        mockConversionService.assertIsSatisfied();
    }


    @Test
    public void shouldAddMessagetoDLQAndNotCallConverstionServiceOnS3CopyError() throws Exception {
        when(s3BucketService.copyToTrustedBucket(any())).thenThrow(new IOException());

        MockEndpoint mockConversionService = mockConversionService();

        getMockEndpoint(dlq).expectedMessageCount(1);
        template.sendBody(endpoint,request);
        getMockEndpoint(dlq).assertIsSatisfied();
        mockConversionService.assertIsNotSatisfied();
    }

    @Test
    public void shouldAddMessagetoDLQWhenConversionServiceFails() throws Exception {
        Document document = getTestDocument();
        when(s3BucketService.copyToTrustedBucket(any())).thenReturn(document);

        MockEndpoint mockConversionService = mockFailedConversionService();

        getMockEndpoint(dlq).expectedMessageCount(1);
        template.sendBody(endpoint,request);
        getMockEndpoint(dlq).assertIsSatisfied();
        mockConversionService.assertIsSatisfied();
    }

    @Test
    public void shouldAddPropertiesToExchange() throws Exception {
        Document document = getTestDocument();
        when(s3BucketService.copyToTrustedBucket(any())).thenReturn(document);

        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedPropertyReceived("caseUUID", "somecase");
        template.sendBody(endpoint,request);
        mockEndpoint.assertIsSatisfied();
    }

    private MockEndpoint mockConversionService() throws IOException, URISyntaxException {
        MockEndpoint mock = getMockEndpoint("mock:conversion-service?throwExceptionOnFailure=false");
        mock.expectedMessageCount(1);
        mock.whenAnyExchangeReceived(exchange -> {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            exchange.getIn().setBody(getPDFDocument());
        });
        return mock;
    }

    private MockEndpoint mockFailedConversionService() throws IOException, URISyntaxException {
        MockEndpoint mock = getMockEndpoint("mock:conversion-service?throwExceptionOnFailure=false");
        mock.expectedMessageCount(1);
        mock.whenAnyExchangeReceived(exchange -> {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            exchange.getIn().setBody(null);
        });
        return mock;
    }

    private Document getTestDocument() throws URISyntaxException, IOException {
        byte[] data = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
        return new Document("UUID", "sample.docx", data, "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    private UploadDocument getTestUploadDocument() throws URISyntaxException, IOException {

        return new UploadDocument("UUID", getPDFDocument(),"somecase");
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }
}