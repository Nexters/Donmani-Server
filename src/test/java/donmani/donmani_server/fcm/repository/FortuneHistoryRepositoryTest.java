package donmani.donmani_server.fcm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class FortuneHistoryRepositoryTest {

	@Test
	void findReadFortunesQueryFetchesOnlyReadHistoriesInTargetDateOrder() throws NoSuchMethodException {
		Query query = FortuneHistoryRepository.class
			.getMethod(
				"findReadFortunesByTargetDateBetween",
				String.class,
				LocalDate.class,
				LocalDate.class
			)
			.getAnnotation(Query.class);

		assertThat(query.value()).contains("JOIN FETCH fh.fortune f");
		assertThat(query.value()).contains("fh.readAt IS NOT NULL");
		assertThat(query.value()).contains("ORDER BY f.targetDate ASC");
	}
}
