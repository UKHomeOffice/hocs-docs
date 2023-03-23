package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.camel.DocumentConversionRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.digital.ho.hocs.document.model.DocumentStatus.FAILED_CONVERSION;
import static uk.gov.digital.ho.hocs.document.model.DocumentStatus.UPLOADED;

@RunWith(MockitoJUnitRunner.class)
public class DocumentConversionConsumerTest extends CamelTestSupport {

    @Mock
    S3DocumentService s3BucketService;

    @Mock
    DocumentDataService documentDataService;

    private final String endpoint = "direct:convertdocument";

    public final String mockNoConvertEndEndpoint = "mock:noConvertEndEndpoint";

    private final String conversionService = "mock:conversion-service";

    private DocumentConversionRequest request = new DocumentConversionRequest(UUID.randomUUID(), "sample.docx",
        "externalReferenceUUID", "docx", "PDF");

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new DocumentConversionConsumer(s3BucketService, documentDataService, conversionService);
    }

    @Test
    public void shouldUpdateDocumentStatusToUploadedOnSuccess() throws Exception {
        MockEndpoint mockConversionService = mockConversionService();
        when(s3BucketService.getFileFromTrustedS3(any())).thenReturn(getTestDocument());
        when(s3BucketService.uploadFile(any())).thenReturn(getTestDocument());

        doNothing().when(documentDataService).updateDocument(any(), eq(UPLOADED), any(), any());
        template.sendBody(endpoint, request);

        mockConversionService.assertIsSatisfied();
        verify(documentDataService, times(1)).updateDocument(any(), eq(UPLOADED), any(), any());
    }

    @Test
    public void shouldNotAddDocumentToDocumentServiceQueueWhenShouldNotConvert() throws Exception {
        DocumentConversionRequest noneRequest = new DocumentConversionRequest(UUID.randomUUID(), "sample.docx",
            "externalReferenceUUID", "docx", "NONE");

        doNothing().when(documentDataService).updateDocument(any(), eq(UPLOADED), any(), any());
        context.getRouteDefinition("conversion-queue").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveAddLast().to(mockNoConvertEndEndpoint);
            }
        });

        MockEndpoint mockConversionService = mockConversionService();
        template.sendBody(endpoint, noneRequest);

        mockConversionService.assertIsNotSatisfied();
        verify(documentDataService, times(1)).updateDocument(any(), eq(UPLOADED), any(), any());
        assertTrue(getMockEndpoint(mockNoConvertEndEndpoint).getReceivedExchanges().get(
            0).getMessage().getHeaders().containsKey(SqsConstants.RECEIPT_HANDLE));
    }

    @Test
    public void shouldSetStatusToConvertedOnSuccess() throws Exception {
        MockEndpoint mockConversionService = mockConversionService();
        when(s3BucketService.getFileFromTrustedS3(any())).thenReturn(getTestDocument());
        when(s3BucketService.uploadFile(any())).thenReturn(getTestDocument());
        doNothing().when(documentDataService).updateDocument(any(), eq(UPLOADED), any(), any());

        template.sendBody(endpoint, request);

        verify(documentDataService, times(1)).updateDocument(any(), eq(UPLOADED), any(), any());
        mockConversionService.assertIsSatisfied();
        verify(s3BucketService).uploadFile(any());
    }

    @Test
    public void shouldSetStatusToFailedOnConversionError() throws Exception {
        MockEndpoint mockConversionService = mockFailedConversionService(400);
        when(s3BucketService.getFileFromTrustedS3(any())).thenReturn(getTestDocument());

        doNothing().when(documentDataService).updateDocument(any(), eq(FAILED_CONVERSION), any(), any());
        template.sendBody(endpoint, request);

        verify(documentDataService, times(1)).updateDocument(any(), eq(FAILED_CONVERSION), any(), any());
        mockConversionService.assertIsSatisfied();
        verify(s3BucketService).uploadFile(any());
    }

    @Test
    public void shouldNotCallConversionServiceOnS3Error() throws Exception {

        MockEndpoint mockConversionService = mockConversionService();
        when(s3BucketService.getFileFromTrustedS3(any())).thenThrow(new IOException());
        template.sendBody(endpoint, request);
        mockConversionService.assertIsNotSatisfied();
    }

    @Test
    public void shouldCallUpdateDocumentWhenConversionServiceReturnsBadRequest() throws Exception {
        when(s3BucketService.getFileFromTrustedS3(any())).thenReturn(getTestDocument());
        MockEndpoint mockConversionService = mockFailedConversionService(400);

        doNothing().when(documentDataService).updateDocument(any(), eq(FAILED_CONVERSION), any(), any());
        template.sendBody(endpoint, request);

        verify(documentDataService, times(1)).updateDocument(any(), eq(FAILED_CONVERSION), any(), any());
        mockConversionService.assertIsSatisfied();
    }

    @Test
    public void shouldCallUpdateDocumentWhenConversionServiceFails() throws Exception {
        when(s3BucketService.getFileFromTrustedS3(any())).thenReturn(getTestDocument());
        MockEndpoint mockConversionService = mockFailedConversionService(500);
        doNothing().when(documentDataService).updateDocument(any(), eq(FAILED_CONVERSION), any(), any());
        template.sendBody(endpoint, request);

        verify(documentDataService, times(1)).updateDocument(any(), eq(FAILED_CONVERSION), any(), any());
        mockConversionService.assertIsSatisfied();
    }


    private MockEndpoint mockConversionService() {
        MockEndpoint mock = getMockEndpoint(
            "mock:conversion-service?throwExceptionOnFailure=false&useSystemProperties=true");
        mock.expectedMessageCount(1);
        mock.whenAnyExchangeReceived(exchange -> {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            exchange.getIn().setBody(getPDFDocument());
        });
        return mock;
    }

    private MockEndpoint mockFailedConversionService(int responseCode) {
        MockEndpoint mock = getMockEndpoint(
            "mock:conversion-service?throwExceptionOnFailure=false&useSystemProperties=true");
        mock.expectedMessageCount(1);
        mock.whenAnyExchangeReceived(exchange -> {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
            exchange.getIn().setBody(null);
        });
        return mock;
    }

    private S3Document getTestDocument() throws URISyntaxException, IOException {
        byte[] data = Files.readAllBytes(
            Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.docx").toURI()));
        return new S3Document("someexternalReferenceUUID/UUID.pdf", "sample.docx", data, "docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    private byte[] getPDFDocument() throws URISyntaxException, IOException {
        return Files.readAllBytes(
            Paths.get(this.getClass().getClassLoader().getResource("testdata/sample.pdf").toURI()));
    }

}
