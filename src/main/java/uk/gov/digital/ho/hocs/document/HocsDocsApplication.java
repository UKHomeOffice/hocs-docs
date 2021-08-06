package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PreDestroy;

@SpringBootApplication
@Slf4j
@EnableRetry
@EnableAsync
public class HocsDocsApplication {

	public static void main(String[] args) {
		log.info("3508");
		SpringApplication.run(HocsDocsApplication.class, args);
	}

	@PreDestroy
	public void stop() {
		log.info("Stopping gracefully");
	}

}
