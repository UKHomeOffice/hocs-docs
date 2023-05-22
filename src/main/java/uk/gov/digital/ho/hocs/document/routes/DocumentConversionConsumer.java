package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.HttpProcessors;
import uk.gov.digital.ho.hocs.document.application.LogEvent;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.camel.UploadDocument;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentConversionExemptTypes;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static uk.gov.digital.ho.hocs.document.application.RequestData.transferHeadersToMDC;
import static uk.gov.digital.ho.hocs.document.application.RequestData.transferMDCToHeaders;

@Component
public class DocumentConversionConsumer extends RouteBuilder {

    private S3DocumentService s3BucketService;

    private DocumentDataService documentDataService;

    private final String hocsConverterPath;

    private String conversionQueue;

    private String convertProducer;

    private String convertConsumer;

    private static final String STATUS = "status";

    private static final String FILENAME = "filename";

    private static final String PDF_FILENAME = "pdfFilename";

    private static final String UUID_TEXT = "uuid";

    private static final String CONVERT_TO = "convertTo";

    private static final String DOCUMENT_TYPE = "documentType";


    public DocumentConversionConsumer(S3DocumentService s3BucketService,
                                      DocumentDataService documentDataService,
                                      @Value("${hocsconverter.path}") String hocsConverterPath,
                                      @Value("${docs.conversion.consumer}") String conversionQueue,
                                      @Value("${docs.convert.producer}") String convertProducer,
                                      @Value("${docs.convert.consumer}") String convertConsumer) {
        this.s3BucketService = s3BucketService;
        this.documentDataService = documentDataService;
        this.hocsConverterPath = String.format("%s?throwExceptionOnFailure=false&useSystemProperties=true",
            hocsConverterPath);
        this.conversionQueue = conversionQueue;
        this.convertProducer = convertProducer;
        this.convertConsumer = convertConsumer;
    }

    @Override
    public void configure() {

        errorHandler(deadLetterChannel("log:conversion-queue"));

        onException(ApplicationExceptions.DocumentConversionException.class, ApplicationExceptions.S3Exception.class)
            .removeHeader(SqsConstants.RECEIPT_HANDLE)
            .handled(true).process(exchange -> {
                UUID documentUUID = UUID.fromString(exchange.getProperty("uuid", String.class));
                documentDataService.updateDocument(documentUUID, DocumentStatus.FAILED_CONVERSION);
        });

        from(conversionQueue).routeId("conversion-queue")
            .onCompletion().onWhen(
                exchangeProperty(STATUS).isNotNull()).setHeader(SqsConstants.RECEIPT_HANDLE,
                exchangeProperty(SqsConstants.RECEIPT_HANDLE))
            .process(exchange -> {
                UUID documentUUID = UUID.fromString(exchange.getProperty(UUID_TEXT, String.class));
                DocumentStatus status = DocumentStatus.valueOf(exchange.getProperty(STATUS).toString());
                String pdfFileLink = exchange.getProperty(PDF_FILENAME, String.class);
                String fileLink = exchange.getProperty(FILENAME, String.class);
                documentDataService.updateDocument(documentUUID, status, fileLink, pdfFileLink);
            })
                .end()
                .log(LoggingLevel.DEBUG,"Attempt to convert document of type ${property.documentType}")
                .process(transferHeadersToMDC())
                .setProperty(CONVERT_TO, simple("${body.convertTo}"))
                .choice()
                .when(skipDocumentConversion)
                    .log(LoggingLevel.DEBUG, "Managed Document - Skipping Conversion: ${body.fileLink}")
                    .setProperty(UUID_TEXT, simple("${body.documentUUID}"))
                    .setProperty(FILENAME, simple("${body.fileLink}"))
                    .setProperty(STATUS, simple(DocumentStatus.UPLOADED.toString()))
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                .endChoice()
                .otherwise()
                    .log(LoggingLevel.DEBUG, "Retrieving document from S3: ${body.fileLink}")
                    .setProperty(UUID_TEXT, simple("${body.documentUUID}"))
                    .setProperty("externalReferenceUUID", simple("${body.externalReferenceUUID}"))
                    .bean(s3BucketService, "getFileFromTrustedS3(${body.fileLink})")
                    .setProperty(FILENAME, simple("${body.filename}"))
                    .setProperty("originalFilename", simple("${body.originalFilename}"))
                    .process(HttpProcessors.buildMultipartEntity())
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                    .process(transferMDCToHeaders())
                    .to(convertProducer)
                    .endChoice();

        from(convertConsumer).routeId("conversion-convert-queue")
                .process(transferHeadersToMDC())
                .log(LoggingLevel.INFO, "Calling document converter service")
                .to(hocsConverterPath)
                .choice()
                .when(HttpProcessors.validateHttpResponse)
                    .log(LoggingLevel.INFO, "Document conversion successful")
                    .process(generateUploadDocument())
                    .log(LoggingLevel.DEBUG, "Uploading file to trusted bucket")
                    .bean(s3BucketService, "uploadFile")
                    .log(LoggingLevel.DEBUG,"PDF Filename: ${body.filename}")
                    .setProperty(PDF_FILENAME, simple("${body.filename}"))
                    .setProperty(STATUS, simple(DocumentStatus.UPLOADED.toString()))
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                    .endChoice()
                .when(HttpProcessors.badRequestResponse)
                    .log(LoggingLevel.WARN, "Document conversion failed")
                    .process(generateFailedDocument())
                    .log(LoggingLevel.DEBUG, "Uploading file to trusted bucket")
                    .bean(s3BucketService, "uploadFile")
                    .log(LoggingLevel.DEBUG,"PDF Filename: ${body.filename}")
                    .setProperty(PDF_FILENAME, simple("${body.filename}"))
                    .setProperty(STATUS, simple(DocumentStatus.FAILED_CONVERSION.toString()))
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                    .endChoice()
                .otherwise()
                    .log(LoggingLevel.ERROR, "Failed to convert document, response: ${body}")
                    .throwException(new ApplicationExceptions.DocumentConversionException("Failed to convert document",
                          LogEvent.DOCUMENT_CONVERSION_FAILURE))
                    .endChoice();

    }

    private Processor generateUploadDocument() {
        return exchange -> {
            byte[] content = exchange.getIn().getBody(byte[].class);
            String filename = exchange.getProperty(FILENAME).toString();
            String externalReferenceUUID = exchange.getProperty("externalReferenceUUID").toString();
            String originalFilename = exchange.getProperty("originalFilename").toString();
            exchange.getMessage().setBody(new UploadDocument(filename, content, externalReferenceUUID, originalFilename));
        };
    }

    private Processor generateFailedDocument() {
        return exchange -> {
            String filename = exchange.getProperty(FILENAME).toString();
            String originalFilename = exchange.getProperty("originalFilename").toString();
            byte[] content = (originalFilename + " failed conversion").getBytes(StandardCharsets.UTF_8);
            String externalReferenceUUID = exchange.getProperty("externalReferenceUUID").toString();
            exchange.getMessage().setBody(new UploadDocument(filename, content, externalReferenceUUID, originalFilename));
        };
    }


    private Predicate skipDocumentType = exchangeProperty(DOCUMENT_TYPE).in(
        Arrays.stream(DocumentConversionExemptTypes.values()).map(
            DocumentConversionExemptTypes::getDisplayValue).toArray(String[]::new));

    private Predicate skipConvertToIsNone = exchangeProperty(CONVERT_TO).isEqualTo("NONE");

    private Predicate skipDocumentConversion = PredicateBuilder.or(skipDocumentType, skipConvertToIsNone);

}
