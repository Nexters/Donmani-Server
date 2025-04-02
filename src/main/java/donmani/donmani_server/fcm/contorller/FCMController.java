package donmani.donmani_server.fcm.contorller;

import donmani.donmani_server.fcm.service.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class FCMController {

    private final FCMService fcmService;

    @PostMapping("/{userKey}/token")
    public ResponseEntity<String> saveOrUpdateToken(@PathVariable String userKey,
                                           @RequestBody String token) {
        fcmService.saveOrUpdateToken(userKey, token);
        return ResponseEntity.ok("SUCCESS"); // TODO : 응답 포맷팅 작업에서 수정 필요
    }

    @PostMapping("/send-messages/{userKey}")
    public ResponseEntity<String> saveOrUpdateToken(@PathVariable String userKey) {
        fcmService.sendMessageTest(userKey);
        return ResponseEntity.ok("SUCCESS"); // TODO : 응답 포맷팅 작업에서 수정 필요
    }
}
