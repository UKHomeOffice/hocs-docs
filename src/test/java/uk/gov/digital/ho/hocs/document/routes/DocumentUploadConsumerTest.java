package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.model.Document;
import uk.gov.digital.ho.hocs.document.model.UploadDocument;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentUploadConsumerTest extends CamelTestSupport {

    @Mock
    S3DocumentService s3BucketService;

    @Mock
    DocumentDataService documentDataService;

    private final String endpoint = "direct:uploadtrustedfile";
    private final String dlq = "mock:cs-dev-document-sqs-dlq";
    private final String toEndpoint = "mock:case-queue";

    private UploadDocument request = getTestUploadDocument();

    public DocumentUploadConsumerTest() throws IOException, URISyntaxException {
    }


    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
      return new UploadDocumentConsumer(s3BucketService, documentDataService, toEndpoint, dlq, 0,0,0);
    }

    @Test
    public void shouldCallS3UploadDocument() throws Exception {
        when(s3BucketService.uploadFile(any())).thenReturn(getTestDocument());
        template.sendBody(endpoint, request);
        verify(s3BucketService, times(1)).uploadFile(request);
    }

    @Test
    public void shouldAddDocumentToCaseQueueOnSuccess() throws Exception {
        Document document = getTestDocument();
        when(s3BucketService.uploadFile(any())).thenReturn(document);

        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedMessageCount(1);

        template.sendBody(endpoint,request);
        mockEndpoint.assertIsSatisfied();
    }


    @Test
    public void shouldAddMessagetoDLQnS3UploadError() throws Exception {
        when(s3BucketService.uploadFile(any())).thenThrow(new RuntimeException());
        getMockEndpoint(dlq).expectedMessageCount(1);
        getMockEndpoint(toEndpoint).expectedMessageCount(1);
        template.sendBody(endpoint,request);
        getMockEndpoint(dlq).assertIsSatisfied();
        getMockEndpoint(toEndpoint).assertIsNotSatisfied();
    }

    private Document getTestDocument() throws URISyntaxException, IOException {
        return new Document("somecase/convertedUUID", "/somecase/unconvertedUUID", null, "pdf", "application/pdf");
    }

    private UploadDocument getTestUploadDocument() throws URISyntaxException, IOException {

        return new UploadDocument("UUID", getPDFDocument(),"somecase");
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }

}