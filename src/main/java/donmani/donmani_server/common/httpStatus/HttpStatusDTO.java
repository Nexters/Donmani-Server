package donmani.donmani_server.common.httpStatus;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Data
@Getter
@Setter
public class HttpStatusDTO<T> {
	private int statusCode;
	private String responseMessage;
	private T responseData;

	public static<T> HttpStatusDTO<T> response(final int statusCode, final String responseMessage, final T responseData) {
		return HttpStatusDTO.<T>builder()
			.statusCode(statusCode)
			.responseMessage(responseMessage)
			.responseData(responseData)
			.build();
	}
}