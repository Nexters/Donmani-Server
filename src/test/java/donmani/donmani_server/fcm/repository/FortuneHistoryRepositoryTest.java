package donmani.donmani_server.fcm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class FortuneHistoryRepositoryTest {

	@Test
	void findFortuneByTargetDateQueryFiltersUserKeyAndTargetDate() throws NoSuchMethodException {
		Query query = FortuneHistoryRepository.class
			.getMethod(
				"findFortuneByTargetDate",
				String.class,
				LocalDate.class
			)
			.getAnnotation(Query.class);

		assertThat(query.value()).contains("fh.user.userKey = :userKey");
		assertThat(query.value()).contains("fh.fortune.targetDate = :targetDate");
	}
}
