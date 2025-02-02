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
		String[] adjectives = {"Blue", "Red", "Green", "Fast", "Lucky"};
		String[] animals = {"Tiger", "Panda", "Eagle", "Lion", "Wolf"};
		int number = (int) (Math.random() * 100);
		return adjectives[(int) (Math.random() * adjectives.length)] +
			animals[(int) (Math.random() * animals.length)] +
			number;
	}
}