package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class HocsDocsApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(HocsDocsApplication.class, args);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
}
