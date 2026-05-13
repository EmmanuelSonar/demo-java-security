package demo.security.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class SecurityAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditLogger.class);
    private static final SecurityAuditLogger INSTANCE = new SecurityAuditLogger();
    private final AuditLogStore store = AuditLogStore.getInstance();

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union\\s+select|drop\\s+table|insert\\s+into|delete\\s+from|'\\s*or\\s*'|" +
        "--\\s*$|/\\*.*\\*/|xp_cmdshell|exec\\s*\\(|;\\s*drop|'\\s*;)"
    );
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|%2e%2e%2f|%2e%2e/|\\.\\./|\\.%2f|%2f\\.\\.)"
    );
    private static final Pattern SCRIPT_INJECTION_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|onerror=|onload=|eval\\s*\\(|document\\.cookie|window\\.location)"
    );
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(;\\s*(ls|cat|rm|wget|curl|bash|sh|cmd)|\\|\\s*(ls|cat|rm|wget)|`[^`]+`)"
    );

    private SecurityAuditLogger() {}

    public static SecurityAuditLogger getInstance() {
        return INSTANCE;
    }

    public void logHttpRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String remoteIp = getClientIp(request);
        String queryString = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");

        boolean suspicious = detectSuspiciousRequest(request);
        ActivityEvent.RiskLevel risk = suspicious ? ActivityEvent.RiskLevel.HIGH : ActivityEvent.RiskLevel.LOW;

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.HTTP_REQUEST_RECEIVED, ActivityEvent.Category.SOURCE)
            .component("LoggingFilter")
            .operation(method + " " + uri)
            .data(queryString)
            .clientIp(remoteIp)
            .requestUri(uri)
            .httpMethod(method)
            .suspicious(suspicious)
            .riskLevel(risk)
            .meta("userAgent", userAgent != null ? userAgent : "unknown")
            .meta("contentType", request.getContentType() != null ? request.getContentType() : "none")
            .build();

        store.store(event);

        if (suspicious) {
            log.warn("SUSPICIOUS HTTP REQUEST: method={} uri={} ip={} query={}", method, uri, remoteIp, queryString);
        } else {
            log.info("HTTP_REQUEST: method={} uri={} ip={}", method, uri, remoteIp);
        }
    }

    public void logUserInputParameter(String component, String paramName, String paramValue, String clientIp) {
        boolean suspicious = isSuspiciousInput(paramValue);
        ActivityEvent.RiskLevel risk = assessInputRisk(paramValue);

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.USER_INPUT_PARAMETER, ActivityEvent.Category.SOURCE)
            .component(component)
            .operation("readParameter")
            .data(paramValue)
            .clientIp(clientIp)
            .suspicious(suspicious)
            .riskLevel(risk)
            .meta("paramName", paramName)
            .build();

        store.store(event);
        log.info("SOURCE[PARAM]: component={} param={} suspicious={} risk={}", component, paramName, suspicious, risk);
    }

    public void logHeaderExtracted(String component, String headerName, String headerValue, String clientIp) {
        boolean suspicious = isSuspiciousInput(headerValue);
        ActivityEvent.RiskLevel risk = assessInputRisk(headerValue);

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.USER_INPUT_HEADER, ActivityEvent.Category.SOURCE)
            .component(component)
            .operation("readHeader")
            .data(headerValue)
            .clientIp(clientIp)
            .suspicious(suspicious)
            .riskLevel(risk)
            .meta("headerName", headerName)
            .build();

        store.store(event);
        log.info("SOURCE[HEADER]: component={} header={} suspicious={}", component, headerName, suspicious);
    }

    public void logSqlQuery(String component, String query, String clientIp) {
        boolean suspicious = SQL_INJECTION_PATTERN.matcher(query).find();
        ActivityEvent.RiskLevel risk = suspicious ? ActivityEvent.RiskLevel.CRITICAL : ActivityEvent.RiskLevel.MEDIUM;

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.SQL_QUERY_EXECUTED, ActivityEvent.Category.SINK)
            .component(component)
            .operation("executeQuery")
            .data(query)
            .clientIp(clientIp)
            .suspicious(suspicious)
            .riskLevel(risk)
            .build();

        store.store(event);

        if (suspicious) {
            log.error("POTENTIAL SQL INJECTION DETECTED: component={} query={}", component, query);
        } else {
            log.info("SINK[SQL]: component={} query={}", component, query);
        }
    }

    public void logFileOperation(String component, String filePath, String operation, String clientIp) {
        boolean suspicious = PATH_TRAVERSAL_PATTERN.matcher(filePath).find();
        ActivityEvent.RiskLevel risk = suspicious ? ActivityEvent.RiskLevel.CRITICAL : ActivityEvent.RiskLevel.MEDIUM;

        ActivityEventType eventType = resolveFileEventType(operation);

        ActivityEvent event = ActivityEvent
            .builder(eventType, ActivityEvent.Category.SINK)
            .component(component)
            .operation(operation)
            .data(filePath)
            .clientIp(clientIp)
            .suspicious(suspicious)
            .riskLevel(risk)
            .meta("operation", operation)
            .build();

        store.store(event);

        if (suspicious) {
            log.error("POTENTIAL PATH TRAVERSAL DETECTED: component={} path={} op={}", component, filePath, operation);
        } else {
            log.info("SINK[FILE]: component={} path={} op={}", component, filePath, operation);
        }
    }

    public void logScriptExecution(String component, String script, String clientIp) {
        boolean suspicious = SCRIPT_INJECTION_PATTERN.matcher(script).find()
            || COMMAND_INJECTION_PATTERN.matcher(script).find();
        ActivityEvent.RiskLevel risk = ActivityEvent.RiskLevel.CRITICAL;

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.SCRIPT_EXECUTED, ActivityEvent.Category.SINK)
            .component(component)
            .operation("eval")
            .data(script)
            .clientIp(clientIp)
            .suspicious(suspicious)
            .riskLevel(risk)
            .meta("scriptLength", String.valueOf(script != null ? script.length() : 0))
            .build();

        store.store(event);
        log.warn("SINK[SCRIPT_EXEC]: component={} suspicious={} scriptPreview={}", component, suspicious,
            script != null && script.length() > 50 ? script.substring(0, 50) + "..." : script);
    }

    public void logDeserialization(String component, String dataSource, String clientIp) {
        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.DESERIALIZATION_PERFORMED, ActivityEvent.Category.SINK)
            .component(component)
            .operation("deserialize")
            .data(dataSource)
            .clientIp(clientIp)
            .suspicious(true)
            .riskLevel(ActivityEvent.RiskLevel.CRITICAL)
            .meta("source", dataSource)
            .build();

        store.store(event);
        log.warn("SINK[DESERIALIZATION]: component={} source={} ip={}", component, dataSource, clientIp);
    }

    public void logNetworkOperation(String component, String host, int port) {
        boolean suspicious = isExternalHost(host);
        ActivityEvent.RiskLevel risk = suspicious ? ActivityEvent.RiskLevel.HIGH : ActivityEvent.RiskLevel.MEDIUM;

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.NETWORK_CONNECTION_OPENED, ActivityEvent.Category.SINK)
            .component(component)
            .operation("openSocket")
            .data(host + ":" + port)
            .suspicious(suspicious)
            .riskLevel(risk)
            .meta("host", host)
            .meta("port", String.valueOf(port))
            .build();

        store.store(event);
        log.warn("SINK[NETWORK]: component={} host={} port={} suspicious={}", component, host, port, suspicious);
    }

    public void logEncryptionOperation(String component, String algorithm, String operation) {
        ActivityEvent.RiskLevel risk = isWeakAlgorithm(algorithm) ? ActivityEvent.RiskLevel.HIGH : ActivityEvent.RiskLevel.LOW;

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.ENCRYPTION_PERFORMED, ActivityEvent.Category.PROCESSING)
            .component(component)
            .operation(operation)
            .data(algorithm)
            .suspicious(isWeakAlgorithm(algorithm))
            .riskLevel(risk)
            .meta("algorithm", algorithm)
            .build();

        store.store(event);
        log.info("PROCESSING[CRYPTO]: component={} algo={} op={} weakAlgo={}", component, algorithm, operation,
            isWeakAlgorithm(algorithm));
    }

    public void logAuthAttempt(String component, String username, boolean success, String clientIp) {
        ActivityEventType type = success
            ? ActivityEventType.AUTHENTICATION_SUCCEEDED
            : ActivityEventType.AUTHENTICATION_FAILED;

        ActivityEvent.RiskLevel risk = success ? ActivityEvent.RiskLevel.LOW : ActivityEvent.RiskLevel.MEDIUM;

        ActivityEvent event = ActivityEvent
            .builder(type, ActivityEvent.Category.PROCESSING)
            .component(component)
            .operation("authenticate")
            .data(username)
            .clientIp(clientIp)
            .userId(username)
            .suspicious(!success)
            .riskLevel(risk)
            .build();

        store.store(event);

        if (success) {
            log.info("AUTH_SUCCESS: component={} user={} ip={}", component, username, clientIp);
        } else {
            log.warn("AUTH_FAILURE: component={} user={} ip={}", component, username, clientIp);
        }
    }

    public void logKeyGeneration(String component, String algorithm, int keySize) {
        boolean weakKey = isWeakKeySize(algorithm, keySize);
        ActivityEvent.RiskLevel risk = weakKey ? ActivityEvent.RiskLevel.HIGH : ActivityEvent.RiskLevel.LOW;

        ActivityEvent event = ActivityEvent
            .builder(ActivityEventType.KEY_GENERATED, ActivityEvent.Category.PROCESSING)
            .component(component)
            .operation("generateKey")
            .data(algorithm + "/" + keySize + "bits")
            .suspicious(weakKey)
            .riskLevel(risk)
            .meta("algorithm", algorithm)
            .meta("keySize", String.valueOf(keySize))
            .build();

        store.store(event);

        if (weakKey) {
            log.warn("WEAK_KEY_GENERATED: component={} algo={} size={}", component, algorithm, keySize);
        } else {
            log.info("PROCESSING[KEY_GEN]: component={} algo={} size={}", component, algorithm, keySize);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    private boolean detectSuspiciousRequest(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString != null && isSuspiciousInput(queryString)) return true;

        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String value = request.getParameter(paramNames.nextElement());
            if (isSuspiciousInput(value)) return true;
        }
        return false;
    }

    private boolean isSuspiciousInput(String input) {
        if (input == null) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find()
            || PATH_TRAVERSAL_PATTERN.matcher(input).find()
            || SCRIPT_INJECTION_PATTERN.matcher(input).find()
            || COMMAND_INJECTION_PATTERN.matcher(input).find();
    }

    private ActivityEvent.RiskLevel assessInputRisk(String input) {
        if (input == null) return ActivityEvent.RiskLevel.LOW;
        if (SQL_INJECTION_PATTERN.matcher(input).find()) return ActivityEvent.RiskLevel.CRITICAL;
        if (COMMAND_INJECTION_PATTERN.matcher(input).find()) return ActivityEvent.RiskLevel.CRITICAL;
        if (SCRIPT_INJECTION_PATTERN.matcher(input).find()) return ActivityEvent.RiskLevel.HIGH;
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) return ActivityEvent.RiskLevel.HIGH;
        return ActivityEvent.RiskLevel.LOW;
    }

    private ActivityEventType resolveFileEventType(String operation) {
        if (operation == null) return ActivityEventType.FILE_WRITE;
        switch (operation.toLowerCase()) {
            case "delete": return ActivityEventType.FILE_DELETE;
            case "read": return ActivityEventType.FILE_READ;
            default: return ActivityEventType.FILE_WRITE;
        }
    }

    private boolean isExternalHost(String host) {
        if (host == null) return false;
        return !host.equals("localhost") && !host.equals("127.0.0.1") && !host.startsWith("192.168.")
            && !host.startsWith("10.") && !host.startsWith("172.");
    }

    private boolean isWeakAlgorithm(String algorithm) {
        if (algorithm == null) return false;
        String alg = algorithm.toUpperCase();
        return alg.contains("MD5") || alg.contains("SHA1") || alg.contains("SHA-1")
            || alg.contains("DES") || alg.contains("RC4") || alg.equals("RSA");
    }

    private boolean isWeakKeySize(String algorithm, int keySize) {
        if ("RSA".equalsIgnoreCase(algorithm)) return keySize < 2048;
        if ("AES".equalsIgnoreCase(algorithm)) return keySize < 128;
        return false;
    }
}
