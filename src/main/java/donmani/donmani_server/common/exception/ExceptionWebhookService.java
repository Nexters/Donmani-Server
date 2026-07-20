package donmani.donmani_server.common.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.HandlerMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExceptionWebhookService {
	static final String WEBHOOK_SENT_ATTRIBUTE = ExceptionWebhookService.class.getName() + ".SENT";

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int CONTENT_LIMIT = 1900;
	private static final int BODY_LIMIT = 1200;
	private static final String STACK_TRACE_FILE_NAME = "exception-stacktrace.log";
	private static final Set<String> STATIC_EXTENSIONS = Set.of(
		".css",
		".js",
		".png",
		".jpg",
		".jpeg",
		".gif",
		".svg",
		".ico",
		".woff",
		".woff2",
		".ttf",
		".map"
	);

	private final WebClient webClient;
	private final Environment environment;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${discord.exception.webhook.url:}")
	private String webhookUrl;

	public void notifyOnce(
		HttpServletRequest request,
		int statusCode,
		Throwable exception,
		String responseMessage
	) {
		if (!shouldNotify(request, statusCode)) {
			return;
		}
		if (Boolean.TRUE.equals(request.getAttribute(WEBHOOK_SENT_ATTRIBUTE))) {
			return;
		}

		request.setAttribute(WEBHOOK_SENT_ATTRIBUTE, true);
		if (!StringUtils.hasText(webhookUrl) || "default-value".equals(webhookUrl)) {
			return;
		}

		Map<String, Object> payload = buildPayload(request, statusCode, exception, responseMessage);
		try {
			String stackTrace = stackTrace(exception);
			if (StringUtils.hasText(stackTrace)) {
				postMultipart(payload, stackTrace);
				return;
			}
			postJson(payload);
		} catch (Exception e) {
			log.warn("Exception webhook delivery setup failed.", e);
		}
	}

	boolean shouldNotify(
		HttpServletRequest request,
		int statusCode
	) {
		if (statusCode < 400 || statusCode >= 600) {
			return false;
		}
		if (statusCode == HttpStatus.NOT_FOUND.value()) {
			return false;
		}
		String path = request.getRequestURI();
		return !isExcludedPath(path);
	}

	Map<String, Object> buildPayload(
		HttpServletRequest request,
		int statusCode,
		Throwable exception,
		String responseMessage
	) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("content", truncate(buildContent(request, statusCode, exception, responseMessage), CONTENT_LIMIT));
		return payload;
	}

	private boolean isExcludedPath(String path) {
		if (!StringUtils.hasText(path)) {
			return false;
		}
		String lowerPath = path.toLowerCase();
		return lowerPath.startsWith("/actuator")
			|| lowerPath.startsWith("/swagger-ui")
			|| lowerPath.startsWith("/v3/api-docs")
			|| lowerPath.startsWith("/donmani/swagger-ui")
			|| lowerPath.startsWith("/donmani/v1/api-docs")
			|| STATIC_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
	}

	private String buildContent(
		HttpServletRequest request,
		int statusCode,
		Throwable exception,
		String responseMessage
	) {
		return """
			*🚨 Donmani API EXCEPTION 🚨*

			**STATUS**
			%s

			**PROFILE**
			%s

			**REQUEST**
			%s %s%s

			**MESSAGE**
			%s

			**EXCEPTION**
			%s
			
			**BODY**
			```json
			%s
			```
			**LOG**
			""".formatted(
			statusCode,
			activeProfiles(),
			request.getMethod(),
			routePattern(request),
			redactedQuery(request),
			defaultText(responseMessage),
			exceptionName(exception),
			requestBody(request)
		).strip();
	}

	private void postJson(Map<String, Object> payload) {
		webClient.post()
			.uri(webhookUrl)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(payload)
			.retrieve()
			.toBodilessEntity()
			.subscribe(
				response -> log.info("Exception webhook delivered. status={}", response.getStatusCode().value()),
				error -> log.warn("Exception webhook delivery failed.", error)
			);
	}

	private void postMultipart(
		Map<String, Object> payload,
		String stackTrace
	) throws JsonProcessingException {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("payload_json", objectMapper.writeValueAsString(payload))
			.contentType(MediaType.APPLICATION_JSON);
		builder.part("files[0]", stackTraceResource(stackTrace))
			.filename(STACK_TRACE_FILE_NAME)
			.contentType(MediaType.TEXT_PLAIN);

		webClient.post()
			.uri(webhookUrl)
			.body(BodyInserters.fromMultipartData(builder.build()))
			.retrieve()
			.toBodilessEntity()
			.subscribe(
				response -> log.info("Exception webhook delivered. status={}", response.getStatusCode().value()),
				error -> log.warn("Exception webhook delivery failed.", error)
			);
	}

	private ByteArrayResource stackTraceResource(String stackTrace) {
		byte[] bytes = stackTrace.getBytes(StandardCharsets.UTF_8);
		return new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return STACK_TRACE_FILE_NAME;
			}
		};
	}

	private String activeProfiles() {
		String[] activeProfiles = environment.getActiveProfiles();
		if (activeProfiles.length == 0) {
			return "default";
		}
		return Arrays.stream(activeProfiles).collect(Collectors.joining(","));
	}

	private String routePattern(HttpServletRequest request) {
		Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (pattern != null) {
			return pattern.toString();
		}
		return request.getRequestURI();
	}

	private String redactedQuery(HttpServletRequest request) {
		String queryString = request.getQueryString();
		if (!StringUtils.hasText(queryString)) {
			return "";
		}
		String redacted = Arrays.stream(queryString.split("&"))
			.map(this::redactQueryParam)
			.collect(Collectors.joining("&"));
		return "?" + redacted;
	}

	private String redactQueryParam(String param) {
		int separatorIndex = param.indexOf('=');
		String key = separatorIndex < 0 ? param : param.substring(0, separatorIndex);
		if (isSensitiveKey(key)) {
			return key + "=***";
		}
		return param;
	}

	private boolean isSensitiveKey(String key) {
		String lowerKey = key.toLowerCase();
		if ("userkey".equals(lowerKey) || "user_key".equals(lowerKey)) {
			return false;
		}
		return lowerKey.contains("token")
			|| lowerKey.contains("key")
			|| lowerKey.contains("password")
			|| lowerKey.contains("secret")
			|| lowerKey.contains("authorization");
	}

	private String requestBody(HttpServletRequest request) {
		if (isBinaryBody(request.getContentType())) {
			return "[binary body omitted]";
		}
		if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
			return "-";
		}
		byte[] body = wrapper.getContentAsByteArray();
		if (body.length == 0) {
			return "-";
		}
		String charset = request.getCharacterEncoding();
		String value = new String(body, StringUtils.hasText(charset)
			? java.nio.charset.Charset.forName(charset)
			: StandardCharsets.UTF_8);
		return truncate(redactBody(value), BODY_LIMIT);
	}

	private boolean isBinaryBody(String contentType) {
		if (!StringUtils.hasText(contentType)) {
			return false;
		}
		String lowerContentType = contentType.toLowerCase();
		return lowerContentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)
			|| lowerContentType.startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE)
			|| lowerContentType.startsWith("image/")
			|| lowerContentType.startsWith("video/")
			|| lowerContentType.startsWith("audio/");
	}

	private String redactBody(String body) {
		if (!StringUtils.hasText(body)) {
			return "-";
		}
		try {
			JsonNode jsonNode = objectMapper.readTree(body);
			JsonNode redacted = redactJson(jsonNode);
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(redacted);
		} catch (Exception e) {
			return body;
		}
	}

	private JsonNode redactJson(JsonNode jsonNode) {
		if (jsonNode instanceof ObjectNode objectNode) {
			objectNode.fieldNames().forEachRemaining(fieldName -> {
				JsonNode value = objectNode.get(fieldName);
				if (isSensitiveKey(fieldName)) {
					objectNode.put(fieldName, "***");
					return;
				}
				objectNode.set(fieldName, redactJson(value));
			});
			return objectNode;
		}
		if (jsonNode instanceof ArrayNode arrayNode) {
			for (int i = 0; i < arrayNode.size(); i++) {
				arrayNode.set(i, redactJson(arrayNode.get(i)));
			}
		}
		return jsonNode;
	}

	private String defaultText(String value) {
		return StringUtils.hasText(value) ? value : "-";
	}

	private String exceptionName(Throwable exception) {
		if (exception == null) {
			return "-";
		}
		return exception.getClass().getName() + ": " + defaultText(exception.getMessage());
	}

	private String stackTrace(Throwable exception) {
		if (exception == null) {
			return null;
		}
		StringWriter writer = new StringWriter();
		exception.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	private String truncate(
		String value,
		int maxLength
	) {
		if (!StringUtils.hasText(value) || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength - 14) + "...(truncated)";
	}
}
