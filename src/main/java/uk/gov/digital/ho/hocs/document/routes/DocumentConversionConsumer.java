package uk.gov.digital.ho.hocs.document.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.application.LogEvent;
import uk.gov.digital.ho.hocs.document.application.RequestData;
import uk.gov.digital.ho.hocs.document.dto.camel.UpdateDocumentRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.HttpProcessors;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.camel.UploadDocument;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.model.DocumentConversionExemptTypes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentConversionConsumer extends RouteBuilder {

    private S3DocumentService s3BucketService;
    private final String hocsConverterPath;
    private final String toQueue;

    private static final String STATUS = "status";
    private static final String FILENAME = "filename";
    private static final String PDF_FILENAME = "pdfFilename";
    private static final String UUID_TEXT = "uuid";

    @Autowired
    public DocumentConversionConsumer(
            S3DocumentService s3BucketService,
            @Value("${hocsconverter.path}") String hocsConverterPath,
            @Value("${documentServiceQueueName}") String toQueue) {
        this.s3BucketService = s3BucketService;
        this.hocsConverterPath =  String.format("%s?throwExceptionOnFailure=false&useSystemProperties=true", hocsConverterPath);
        this.toQueue = toQueue;
    }

    @Override
    public void configure() {

        from("direct:convertdocument").routeId("conversion-queue")
                .onCompletion()
                    .onWhen(exchangeProperty(STATUS).isNotNull())
                    .process(generateDocumentUpdateRequest())
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                    .process(RequestData.transferHeadersToQueue())
                    .to(toQueue)
                .end()
                .log(LoggingLevel.INFO,"Attempt to convert document of type ${property.documentType}")
                .log(LoggingLevel.DEBUG,"Value convertTo = ${body.convertTo}")
                .log(LoggingLevel.DEBUG,"Should convert "+ skipDocumentConversion)
                .choice()
                .when(skipDocumentConversion)
                    .log(LoggingLevel.INFO, "Managed Document - Skipping Conversion: ${body.fileLink}")
                    .setProperty(FILENAME, simple("${body.fileLink}"))
                    .setProperty(STATUS, simple(DocumentStatus.UPLOADED.toString()))
                    .endChoice()
                .otherwise()
                    .log(LoggingLevel.INFO, "Retrieving document from S3: ${body.fileLink}")
                    .setProperty(UUID_TEXT, simple("${body.documentUUID}"))
                    .setProperty("externalReferenceUUID", simple("${body.externalReferenceUUID}"))
                    .bean(s3BucketService, "getFileFromTrustedS3(${body.fileLink})")
                    .setProperty(FILENAME, simple("${body.filename}"))
                    .setProperty("originalFilename", simple("${body.originalFilename}"))
                    .log(LoggingLevel.DEBUG, "Original Filename ${body.originalFilename}")
                    .process(HttpProcessors.buildMultipartEntity())
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                    .process(RequestData.transferHeadersToQueue())
                    .to("direct:convert")
                    .endChoice();



        from("direct:convert").routeId("conversion-convert-queue")
                .errorHandler(noErrorHandler())
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
                    .setProperty(STATUS, simple(DocumentStatus.FAILED_CONVERSION.toString()))
                    .throwException(new ApplicationExceptions.DocumentConversionException("Failed to convert document, response: ${body}", 
                            LogEvent.DOCUMENT_CONVERSION_FAILURE))
                    .setHeader(SqsConstants.RECEIPT_HANDLE, exchangeProperty(SqsConstants.RECEIPT_HANDLE))
                    .endChoice();

    }

    private Processor generateUploadDocument() {
        return exchange -> {
            byte[] content =  exchange.getIn().getBody(byte[].class);
            String filename = exchange.getProperty(FILENAME).toString();
            String externalReferenceUUID = exchange.getProperty("externalReferenceUUID").toString();
            String originalFilename = exchange.getProperty("originalFilename").toString();
            exchange.getOut().setBody(new UploadDocument(filename, content, externalReferenceUUID, originalFilename));
        };
    }

    private Processor generateFailedDocument() {
        return exchange -> {
            String filename = exchange.getProperty(FILENAME).toString();
            String originalFilename = exchange.getProperty("originalFilename").toString();
            byte[] content =  (originalFilename + " failed conversion").getBytes(StandardCharsets.UTF_8);
            String externalReferenceUUID = exchange.getProperty("externalReferenceUUID").toString();
            exchange.getOut().setBody(new UploadDocument(filename, content, externalReferenceUUID, originalFilename));
        };
    }

    private Processor generateDocumentUpdateRequest() {
        return exchange -> {
            UUID documentUUID = UUID.fromString(exchange.getProperty(UUID_TEXT).toString());
            DocumentStatus status = DocumentStatus.valueOf(exchange.getProperty(STATUS).toString());
            String pdfFileLink = Optional.ofNullable(exchange.getProperty(PDF_FILENAME)).orElse("").toString();
            String fileLink = Optional.ofNullable(exchange.getProperty(FILENAME)).orElse("").toString();
            exchange.getOut().setBody(new UpdateDocumentRequest(documentUUID, status, fileLink ,pdfFileLink));
        };
    }

    private Predicate skipDocumentType = exchangeProperty("documentType").in(Arrays.stream(DocumentConversionExemptTypes.values()).map(DocumentConversionExemptTypes::getDisplayValue).toArray(String[]::new));
    private Predicate skipConvertToIsNone = exchangeProperty("convertTo").isEqualTo("NONE");
    private Predicate skipDocumentConversion = PredicateBuilder.or(skipDocumentType, skipConvertToIsNone);
}
