package donmani.donmani_server.common.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final ExceptionWebhookService exceptionWebhookService;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<HttpStatusDTO<Void>> handleApiException(
        ApiException ex,
        HttpServletRequest request
    ) {
        ApiErrorCode errorCode = ex.getErrorCode();
        String message = ex.getMessage();
        exceptionWebhookService.notifyOnce(request, errorCode.getBodyStatusCode(), ex, message);
        HttpStatusDTO<Void> body = HttpStatusDTO.response(
            errorCode.getBodyStatusCode(),
            message,
            null
        );
        return ResponseEntity.status(errorCode.getResponseStatus()).body(body);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(
        Exception ex,
        HttpServletRequest request
    ) {
        String message = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "잘못된 요청입니다.";
        exceptionWebhookService.notifyOnce(request, HttpStatus.BAD_REQUEST.value(), ex, message);
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, message);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
        ResponseStatusException ex,
        HttpServletRequest request
    ) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = StringUtils.hasText(ex.getReason()) ? ex.getReason() : status.getReasonPhrase();
        exceptionWebhookService.notifyOnce(request, status.value(), ex, message);
        Map<String, Object> body = buildBody(status, message);
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unhandled API exception.", ex);
        String message = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "서버 오류가 발생했습니다.";
        exceptionWebhookService.notifyOnce(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex, message);
        Map<String, Object> body = buildBody(HttpStatus.INTERNAL_SERVER_ERROR, message);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> buildBody(
        HttpStatus status,
        String message
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
