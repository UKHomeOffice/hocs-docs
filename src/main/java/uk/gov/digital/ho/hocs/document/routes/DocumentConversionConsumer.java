package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.dto.camel.DocumentConversionRequest;
import uk.gov.digital.ho.hocs.document.dto.camel.UpdateDocumentRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.HttpProcessors;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.camel.UploadDocument;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;

import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentConversionConsumer extends RouteBuilder {

    private String dlq;
    private final int maximumRedeliveries;
    private final int redeliveryDelay;
    private final int backOffMultiplier;
    private S3DocumentService s3BucketService;
    private final String hocsConverterPath;
    private final String toQueue;

    @Autowired
    public DocumentConversionConsumer(
            S3DocumentService s3BucketService,
            @Value("${hocsconverter.path}") String hocsConverterPath,
            @Value("${docs.queue.dlq}") String dlq,
            @Value("${docs.queue.conversion.maximumRedeliveries}") int maximumRedeliveries,
            @Value("${docs.queue.redeliveryDelay}") int redeliveryDelay,
            @Value("${docs.queue.backOffMultiplier}") int backOffMultiplier,
            @Value("${documentServiceQueueName}") String toQueue) {
        this.s3BucketService = s3BucketService;
        this.hocsConverterPath =  String.format("%s?throwExceptionOnFailure=false&useSystemProperties=true", hocsConverterPath);
        this.dlq = dlq;
        this.maximumRedeliveries = maximumRedeliveries;
        this.redeliveryDelay = redeliveryDelay;
        this.backOffMultiplier = backOffMultiplier;
        this.toQueue = toQueue;
    }

    @Override
    public void configure() {
        errorHandler(deadLetterChannel(dlq)
                .loggingLevel(LoggingLevel.ERROR)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .log("Failed to convert document")
                .useOriginalMessage()
                .maximumRedeliveries(maximumRedeliveries)
                .redeliveryDelay(redeliveryDelay)
                .backOffMultiplier(backOffMultiplier)
                .asyncDelayedRedelivery()
                .logRetryStackTrace(true)
                .onPrepareFailure(exchange -> {
                    exchange.getIn().setHeader("FailureMessage", exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                            Exception.class).getMessage());
                }));



        this.getContext().setStreamCaching(true);

        from("direct:convertdocument").routeId("conversion-queue")
                .onCompletion()
                    .onWhen(exchangeProperty("status").isNotNull())
                    .process(generateDocumentUpdateRequest())
                    .to(toQueue)
                .end()
                .log(LoggingLevel.INFO, "Retrieving document from S3: ${body.fileLink}")
                .setProperty("uuid", simple("${body.documentUUID}"))
                .setProperty("caseUUID", simple("${body.caseUUID}"))
                .bean(s3BucketService, "getFileFromTrustedS3(${body.fileLink})")
                .setProperty("filename", simple("${body.filename}"))
                .setProperty("originalFilename", simple("${body.originalFilename}"))
                .log("Original Filename ${body.originalFilename}")
                .process(HttpProcessors.buildMultipartEntity())
                .to("direct:convert");


        from("direct:convert").routeId("conversion-convert-queue")
                .errorHandler(noErrorHandler())
                .log("Calling document converter service")
                .to(hocsConverterPath)
                .choice()
                .when(HttpProcessors.validateHttpResponse)
                    .log(LoggingLevel.INFO, "Document conversion successful")
                    .process(generateUploadDocument())
                    .log(LoggingLevel.INFO, "Uploading file to trusted bucket")
                    .bean(s3BucketService, "uploadFile")
                    .log("PDF Filename: ${body.filename}")
                    .setProperty("pdfFilename", simple("${body.filename}"))
                    .setProperty("status", simple(DocumentStatus.UPLOADED.toString()))
                .otherwise()
                    .log(LoggingLevel.ERROR, "Failed to convert document, response: ${body}")
                    .setProperty("status", simple(DocumentStatus.FAILED_CONVERSION.toString()))
                    .throwException(new ApplicationExceptions.DocumentConversionException("Document conversion failed for document" + simple("${property.originalFilename}")))
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE));
    }

    private Processor generateUploadDocument() {
        return exchange -> {
            byte[] content =  exchange.getIn().getBody(byte[].class);
            String filename = exchange.getProperty("filename").toString();
            String caseUUID = exchange.getProperty("caseUUID").toString();
            String originalFilename = exchange.getProperty("originalFilename").toString();
            exchange.getOut().setBody(new UploadDocument(filename, content, caseUUID, originalFilename));
        };
    }


    private Processor generateDocumentUpdateRequest() {
        return exchange -> {
            UUID documentUUID = UUID.fromString(exchange.getProperty("uuid").toString());
            DocumentStatus status = DocumentStatus.valueOf(exchange.getProperty("status").toString());
            String pdfFileLink = Optional.ofNullable(exchange.getProperty("pdfFilename")).orElse("").toString();
            String fileLink = Optional.ofNullable(exchange.getProperty("filename")).orElse("").toString();
            exchange.getOut().setBody(new UpdateDocumentRequest(documentUUID, status, fileLink ,pdfFileLink));
        };
    }


}
