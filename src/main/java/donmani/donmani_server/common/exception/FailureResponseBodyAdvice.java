package donmani.donmani_server.common.exception;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class FailureResponseBodyAdvice implements ResponseBodyAdvice<Object> {
	private final ExceptionWebhookService exceptionWebhookService;

	@Override
	public boolean supports(
		MethodParameter returnType,
		Class<? extends HttpMessageConverter<?>> converterType
	) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(
		Object body,
		MethodParameter returnType,
		MediaType selectedContentType,
		Class<? extends HttpMessageConverter<?>> selectedConverterType,
		ServerHttpRequest request,
		ServerHttpResponse response
	) {
		if (body instanceof HttpStatusDTO<?> httpStatusDTO && request instanceof ServletServerHttpRequest servletRequest) {
			int statusCode = httpStatusDTO.getStatusCode();
			if (statusCode >= 400 && statusCode < 600) {
				HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
				exceptionWebhookService.notifyOnce(
					httpServletRequest,
					statusCode,
					null,
					httpStatusDTO.getResponseMessage()
				);
			}
		}
		return body;
	}
}
