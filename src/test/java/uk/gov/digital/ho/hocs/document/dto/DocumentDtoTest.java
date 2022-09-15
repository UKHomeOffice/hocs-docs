package uk.gov.digital.ho.hocs.document.dto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.model.DocumentData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentDtoTest {

    @Test
    public void whenPdfLinkThenHasPdfIsTrue() {

        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getPdfLink()).thenReturn("pdfLink");

        DocumentDto documentDto = DocumentDto.from(documentData);

        assertThat(documentDto.getHasPdf()).isTrue();
    }

    @Test
    public void whenNoPdfLinkThenHasPdfIsFalse() {

        DocumentData documentData = mock(DocumentData.class);
        when(documentData.getPdfLink()).thenReturn("");

        DocumentDto documentDto = DocumentDto.from(documentData);

        assertThat(documentDto.getHasPdf()).isFalse();
    }

}