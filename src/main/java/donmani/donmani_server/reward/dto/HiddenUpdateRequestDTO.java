package donmani.donmani_server.reward.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiddenUpdateRequestDTO {
    private String userKey;

    private int year;
    private int month;
}
