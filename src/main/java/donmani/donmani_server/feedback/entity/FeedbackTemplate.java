package donmani.donmani_server.feedback.entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class FeedbackTemplate {
	private final String title;
	private final String content;
}
