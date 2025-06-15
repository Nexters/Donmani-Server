package donmani.donmani_server.reward.entity;

import donmani.donmani_server.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEquippedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private RewardItem background;

    @ManyToOne
    private RewardItem effect;

    @ManyToOne
    private RewardItem decoration;

    @ManyToOne
    private RewardItem byeoltongCase;

//    @ManyToOne
//    private RewardItem bgm;

    private LocalDateTime savedAt;

    public void updateEquippedStatus(RewardItem background, RewardItem effect, RewardItem decoration,
            RewardItem byeoltongCase, LocalDateTime savedAt) {
        this.background = background;
        this.effect = effect;
        this.decoration = decoration;
        this.byeoltongCase = byeoltongCase;
        //this.bgm = bgm;
        this.savedAt = savedAt;
    }
}
