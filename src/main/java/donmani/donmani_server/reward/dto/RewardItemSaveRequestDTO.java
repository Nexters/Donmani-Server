package donmani.donmani_server.reward.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardItemSaveRequestDTO {
    private String userKey;

    private int year;
    private int month;

    private Long backgroundId;
    private Long effectId;
    private Long decorationId;
    private Long byeoltongCaseId;
    //private Long bgmId;
}
