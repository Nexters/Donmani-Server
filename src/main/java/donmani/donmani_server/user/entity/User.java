package donmani.donmani_server.user.entity;

import donmani.donmani_server.fcm.entity.FCMToken;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String userKey;

	private String name;

	private int level;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private FCMToken fcmToken;

	private boolean isNoticeRead;

	public static String generateRandomUsername() {
		String[] adjectives = {"기쁜", "활발한", "멋있는", "즐거운", "당황한", "설레는", "귀여운", "뿌듯한", "시원한", "황홀한"};
		String[] animals = {"고양이", "강아지", "호랑이", "사자", "여우", "늑대", "토끼", "다람쥐", "판다", "코끼리", "원숭이", "곰", "독수리", "올빼미", "수달", "두더지", "너구리", "하마", "피카츄"};

		// 최대 여섯글자까지
		return adjectives[(int) (Math.random() * adjectives.length)] + animals[(int) (Math.random() * animals.length)];
	}
}