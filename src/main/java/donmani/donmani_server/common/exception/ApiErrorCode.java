package donmani.donmani_server.common.exception;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
	USER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR.value(), "유저 정보 없음", HttpStatus.OK),
	FEEDBACK_NOT_AVAILABLE(HttpStatus.INTERNAL_SERVER_ERROR.value(), "모든 피드백을 열었습니다.", HttpStatus.OK),
	FORTUNE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "운세 정보 없음", HttpStatus.OK),
	FORTUNE_READ_NOT_FOUND(HttpStatus.NOT_FOUND.value(), null, HttpStatus.OK),
	BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), "잘못된 요청", HttpStatus.OK),
	AUTOMATION_BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), null, HttpStatus.OK),
	AUTOMATION_UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "인증 실패", HttpStatus.UNAUTHORIZED),
	REWARD_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), null, HttpStatus.OK),
	HIDDEN_ITEM_ALREADY_OPENED(HttpStatus.BAD_REQUEST.value(), "이미 히든 아이템을 열었습니다.", HttpStatus.BAD_REQUEST);

	private final int bodyStatusCode;
	private final String defaultMessage;
	private final HttpStatus responseStatus;

	ApiErrorCode(
		int bodyStatusCode,
		String defaultMessage,
		HttpStatus responseStatus
	) {
		this.bodyStatusCode = bodyStatusCode;
		this.defaultMessage = defaultMessage;
		this.responseStatus = responseStatus;
	}

	public int getBodyStatusCode() {
		return bodyStatusCode;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}

	public HttpStatus getResponseStatus() {
		return responseStatus;
	}
}
