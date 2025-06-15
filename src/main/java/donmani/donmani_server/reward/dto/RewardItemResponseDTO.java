package donmani.donmani_server.reward.dto;

import donmani.donmani_server.reward.entity.RewardCategory;
import donmani.donmani_server.reward.entity.RewardItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardItemResponseDTO {
    @Schema(description = "리워드 아이템 id", example = "1")
    private Long id;

    @Schema(description = "리워드 아이템 이름", example = "하트 잔잔")
    private String name;

    @Schema(description = "리워드 아이템 이미지/gif URL", example = "하트 잔잔")
    private String imageUrl;
    private String jsonUrl;
    //private String mp3Url;
    private String thumbnailUrl;
    private RewardCategory category;
    private boolean isHidden;
    private boolean newAcquiredFlag;

    // 가지고 있는 아이템들만 보여줄 때 사용
    public static RewardItemResponseDTO of(RewardItem rewardItem) {
        RewardItemResponseDTO response = new RewardItemResponseDTO();

        String prefix = "https://kr.object.ncloudstorage.com/donmani.bucket/reward_content/";

        response.setId(rewardItem.getId());
        response.setName(rewardItem.getName());
        response.setImageUrl(
                rewardItem.getImageUrl() != null ? prefix + rewardItem.getImageUrl() : null
        );
        response.setJsonUrl(
                rewardItem.getJsonUrl() != null ? prefix + rewardItem.getJsonUrl() : null
        );
//        response.setMp3Url(
//                rewardItem.getMp3Url() != null ? prefix + rewardItem.getMp3Url() : null
//        );
        response.setThumbnailUrl(
                rewardItem.getThumbnailUrl() != null ? prefix + rewardItem.getThumbnailUrl() : null
        );
        response.setCategory(rewardItem.getCategory());
        response.setHidden(rewardItem.isHidden());

        return response;
    }

    public static RewardItemResponseDTO of(RewardItem rewardItem, boolean newAcquiredFlag) {
        RewardItemResponseDTO response = new RewardItemResponseDTO();

        String prefix = "https://kr.object.ncloudstorage.com/donmani.bucket/reward_content/";

        response.setId(rewardItem.getId());
        response.setName(rewardItem.getName());
        response.setImageUrl(
                rewardItem.getImageUrl() != null ? prefix + rewardItem.getImageUrl() : null
        );
        response.setJsonUrl(
                rewardItem.getJsonUrl() != null ? prefix + rewardItem.getJsonUrl() : null
        );
//        response.setMp3Url(
//                rewardItem.getMp3Url() != null ? prefix + rewardItem.getMp3Url() : null
//        );
        response.setThumbnailUrl(
                rewardItem.getThumbnailUrl() != null ? prefix + rewardItem.getThumbnailUrl() : null
        );
        response.setCategory(rewardItem.getCategory());
        response.setHidden(rewardItem.isHidden());
        response.setNewAcquiredFlag(newAcquiredFlag);

        return response;
    }
}
