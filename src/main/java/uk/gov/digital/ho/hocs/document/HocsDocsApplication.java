package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PreDestroy;

@SpringBootApplication
@Slf4j
public class HocsDocsApplication {

	public static void main(String[] args) {
		SpringApplication.run(HocsDocsApplication.class, args);
	}

	@PreDestroy
	public void stop() {
		log.info("Stopping gracefully");
	}

}
