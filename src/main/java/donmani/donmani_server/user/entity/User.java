package donmani.donmani_server.user.entity;

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

	public static String generateRandomUsername() {
		String[] adjectives = {"기쁜", "슬픈", "화난", "즐거운", "부끄러운", "당황한", "설레는", "외로운", "초조한", "뿌듯한", "시원한", "우울한", "황홀한", "씁쓸한", "의기양양한", "서운한", "후련한", "억울한", "두근거리는", "운이 좋은"};
		String[] animals = {"고양이", "강아지", "호랑이", "사자", "여우", "늑대", "토끼", "다람쥐", "판다", "코끼리", "원숭이", "곰", "독수리", "올빼미", "수달", "두더지", "너구리", "고슴도치", "하마", "피카츄"};

		return adjectives[(int) (Math.random() * adjectives.length)] + " " + animals[(int) (Math.random() * animals.length)];
	}
}