package uk.gov.digital.ho.hocs.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HocsDocsApplication {

	public static void main(String[] args) {

		try {
			SpringApplication.run(HocsDocsApplication.class, args);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
