package uk.gov.digital.ho.hocs.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRetry
@EnableAsync
public class HocsDocsApplication {

	public static void main(String[] args) {
			SpringApplication.run(HocsDocsApplication.class, args);
	}
}
