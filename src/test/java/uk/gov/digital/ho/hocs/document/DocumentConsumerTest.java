package uk.gov.digital.ho.hocs.document;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.*;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:application-test.properties")
public class DocumentConsumerTest {

    @Autowired
    private CamelContext context;

    @Autowired
    private ProducerTemplate template;


    @Before
    public void setUp() throws Exception {


    }
}