package donmani.donmani_server.common.exception;

import org.springframework.util.StringUtils;

public class ApiException extends RuntimeException {
	private final ApiErrorCode errorCode;
	private final String messageOverride;

	public ApiException(ApiErrorCode errorCode) {
		this(errorCode, null, null);
	}

	public ApiException(
		ApiErrorCode errorCode,
		Throwable cause
	) {
		this(errorCode, null, cause);
	}

	public ApiException(
		ApiErrorCode errorCode,
		String messageOverride
	) {
		this(errorCode, messageOverride, null);
	}

	public ApiException(
		ApiErrorCode errorCode,
		String messageOverride,
		Throwable cause
	) {
		super(resolveMessage(errorCode, messageOverride), cause);
		this.errorCode = errorCode;
		this.messageOverride = messageOverride;
	}

	public static ApiException of(ApiErrorCode errorCode) {
		return new ApiException(errorCode);
	}

	public static ApiException of(
		ApiErrorCode errorCode,
		String messageOverride
	) {
		return new ApiException(errorCode, messageOverride);
	}

	public static ApiException of(
		ApiErrorCode errorCode,
		Throwable cause
	) {
		return new ApiException(errorCode, cause);
	}

	public static ApiException of(
		ApiErrorCode errorCode,
		String messageOverride,
		Throwable cause
	) {
		return new ApiException(errorCode, messageOverride, cause);
	}

	public ApiErrorCode getErrorCode() {
		return errorCode;
	}

	@Override
	public String getMessage() {
		return resolveMessage(errorCode, messageOverride);
	}

	private static String resolveMessage(
		ApiErrorCode errorCode,
		String messageOverride
	) {
		if (StringUtils.hasText(messageOverride)) {
			return messageOverride;
		}
		return errorCode.getDefaultMessage();
	}
}
