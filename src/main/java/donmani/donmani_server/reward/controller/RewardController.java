package donmani.donmani_server.reward.controller;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.reward.dto.RewardItemResponseDTO;
import donmani.donmani_server.reward.dto.RewardItemSaveRequestDTO;
import donmani.donmani_server.reward.entity.RewardCategory;
import donmani.donmani_server.reward.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reward")
public class RewardController {

    private final RewardService rewardService;

    /**
     * 열지 않은 선물의 개수
     * @param userKey
     * @return
     */
    @GetMapping("/not-open/{userKey}")
    public ResponseEntity<HttpStatusDTO<Integer>> getNotOpenedItemSize(@PathVariable String userKey) {
        return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", rewardService.getNotOpenedItemSize(userKey)));
    }

    /***
     * 선물 열기
     * @param userKey
     */
    @PutMapping("/open/{userKey}")
    public ResponseEntity<HttpStatusDTO<List<RewardItemResponseDTO>>> openItems(@PathVariable String userKey) {
        return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", rewardService.openItems(userKey)));
    }

    /***
     * 꾸미기 탭 조회
     * @param userKey
     */
    @GetMapping("/edit/{userKey}")
    public ResponseEntity<HttpStatusDTO<Map<RewardCategory, List<RewardItemResponseDTO>>>>
            getAcquiredItem(@PathVariable String userKey) {
        return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", rewardService.getAcquiredItem(userKey)));
    }

    /***
     * 꾸미기 저장
     */
    @PutMapping("/{userKey}")
    public ResponseEntity<HttpStatusDTO> saveItem(@RequestBody RewardItemSaveRequestDTO request) {
        try {
            rewardService.saveItem(request);
            return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", null));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), null));
        }

    }

    /***
     * 꾸미기 상태 불러오기
     */
    @GetMapping("/{userKey}")
    public ResponseEntity<HttpStatusDTO<List<RewardItemResponseDTO>>> getSavedItem(
            @PathVariable String userKey, @RequestParam int year, @RequestParam int month
    ) {
        return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", rewardService.getSavedItem(userKey, year, month)));
    }
}
