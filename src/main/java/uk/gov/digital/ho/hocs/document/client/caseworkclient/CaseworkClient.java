package uk.gov.digital.ho.hocs.document.client.caseworkclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.digital.ho.hocs.document.application.RestHelper;
import uk.gov.digital.ho.hocs.document.client.caseworkclient.dto.PostCaseNoteRequest;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;


import java.util.UUID;

@Slf4j
@Component
public class CaseworkClient {


    private final RestHelper restHelper;
    private final String serviceBaseURL;

    @Autowired
    public CaseworkClient(RestHelper restHelper,
                          @Value("${hocs.case-service}") String caseService) {
        this.restHelper = restHelper;
        this.serviceBaseURL = caseService;
    }

//
//
//    public UUID createDocumentCaseNote(UUID caseUUID) {
//        PostCaseNoteRequest postCaseNoteRequest = new PostCaseNoteRequest()
//                ResponseEntity<UUID> response = restHelper.post(serviceBaseURL, String.format("/case/%s/note", caseUUID), postCaseNoteRequest, ResponseEntity.class);
//
//        if (response.getStatusCodeValue() == 200) {
//            log.info("Got Team for stage: {}", stageUUID);
//            return response.getBody();
//        } else {
//            throw new ApplicationExceptions.EntityNotFoundException("Could not get Team for stage %s; response: %s", stageUUID, response.getStatusCodeValue());
//        }
//    }
//

}
