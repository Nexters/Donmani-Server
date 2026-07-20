package donmani.donmani_server.common.exception;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FailureStatusFilter extends OncePerRequestFilter {
	private final ExceptionWebhookService exceptionWebhookService;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper contentCachingRequest
			? contentCachingRequest
			: new ContentCachingRequestWrapper(request);
		try {
			filterChain.doFilter(wrappedRequest, response);
		} finally {
			exceptionWebhookService.notifyOnce(
				wrappedRequest,
				response.getStatus(),
				null,
				null
			);
		}
	}
}
