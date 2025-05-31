package donmani.donmani_server.reward.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private RewardCategory category;

    @Column
    private String imageUrl;

    @Column
    private String jsonUrl;

    @Column
    private String mp3Url;

    private boolean hidden; // 히든 아이템 여부
}
