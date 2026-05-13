package demo.security.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

@WebFilter("/*")
public class LoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private final SecurityAuditLogger auditLogger = SecurityAuditLogger.getInstance();

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("SecurityAuditLogger filter initialized");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        long startTime = System.currentTimeMillis();
        StatusCapturingResponseWrapper responseWrapper = new StatusCapturingResponseWrapper(response);

        auditLogger.logHttpRequest(request);

        try {
            chain.doFilter(request, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = responseWrapper.getStatus();
            logResponseCompleted(request, status, duration);
        }
    }

    @Override
    public void destroy() {
        log.info("SecurityAuditLogger filter destroyed");
    }

    private void logResponseCompleted(HttpServletRequest request, int status, long durationMs) {
        ActivityEvent.RiskLevel risk = status >= 500 ? ActivityEvent.RiskLevel.HIGH
            : status >= 400 ? ActivityEvent.RiskLevel.MEDIUM
            : ActivityEvent.RiskLevel.LOW;

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.HTTP_RESPONSE_WRITTEN, ActivityEvent.Category.SINK)
            .component("LoggingFilter")
            .operation("sendResponse")
            .data(String.valueOf(status))
            .requestUri(request.getRequestURI())
            .httpMethod(request.getMethod())
            .riskLevel(risk)
            .meta("statusCode", String.valueOf(status))
            .meta("durationMs", String.valueOf(durationMs))
            .build();

        AuditLogStore.getInstance().store(event);
        log.info("HTTP_RESPONSE: method={} uri={} status={} duration={}ms",
            request.getMethod(), request.getRequestURI(), status, durationMs);
    }

    private static class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {

        private int status = 200;

        public StatusCapturingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        public int getStatus() {
            return status;
        }
    }
}
