package uk.gov.digital.ho.hocs.document.application;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration

public class SpringConfiguration implements WebMvcConfigurer {

    @Bean
    public RequestData createRequestData() {
        return new RequestData();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(createRequestData());
    }


    @Bean
    public CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                context.setUseMDCLogging(true);
                context.setStreamCaching(true);
  
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // no changes after started required.
            }
        };
    }

    @Bean
    Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger("uk.gov.homeoffice.hocs.docs");
    }


}
