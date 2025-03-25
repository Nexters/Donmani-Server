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

	public static String generateRandomUsername() {
		String[] adjectives = {"Blue", "Red", "Green", "Fast", "Lucky"};
		String[] animals = {"Tiger", "Panda", "Eagle", "Lion", "Wolf"};
		int number = (int) (Math.random() * 100);
		return adjectives[(int) (Math.random() * adjectives.length)] +
			animals[(int) (Math.random() * animals.length)] +
			number;
	}
}