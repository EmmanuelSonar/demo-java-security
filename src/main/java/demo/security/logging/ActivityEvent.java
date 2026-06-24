package demo.security.logging;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActivityEvent {

    public enum Category {
        SOURCE, SINK, PROCESSING
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    private final String eventId;
    private final ActivityEventType eventType;
    private final Category category;
    private final String component;
    private final String operation;
    private final String data;
    private final String clientIp;
    private final String userId;
    private final Instant timestamp;
    private final Map<String, String> metadata;
    private final boolean suspicious;
    private final RiskLevel riskLevel;
    private final String threadName;
    private final String requestUri;
    private final String httpMethod;

    private ActivityEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.eventType = builder.eventType;
        this.category = builder.category;
        this.component = builder.component;
        this.operation = builder.operation;
        this.data = builder.data;
        this.clientIp = builder.clientIp;
        this.userId = builder.userId;
        this.timestamp = builder.timestamp;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
        this.suspicious = builder.suspicious;
        this.riskLevel = builder.riskLevel;
        this.threadName = Thread.currentThread().getName();
        this.requestUri = builder.requestUri;
        this.httpMethod = builder.httpMethod;
    }

    public String getEventId() { return eventId; }
    public ActivityEventType getEventType() { return eventType; }
    public Category getCategory() { return category; }
    public String getComponent() { return component; }
    public String getOperation() { return operation; }
    public String getData() { return data; }
    public String getClientIp() { return clientIp; }
    public String getUserId() { return userId; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, String> getMetadata() { return metadata; }
    public boolean isSuspicious() { return suspicious; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getThreadName() { return threadName; }
    public String getRequestUri() { return requestUri; }
    public String getHttpMethod() { return httpMethod; }

    @Override
    public String toString() {
        return String.format(
            "[%s] %s | category=%s | type=%s | component=%s | op=%s | ip=%s | user=%s | risk=%s | suspicious=%s | data=%s",
            timestamp, eventId, category, eventType, component, operation,
            clientIp, userId, riskLevel, suspicious, truncate(data, 200)
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "null";
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    public static Builder builder(ActivityEventType type, Category category) {
        return new Builder(type, category);
    }

    public static class Builder {
        private final String eventId = UUID.randomUUID().toString();
        private final ActivityEventType eventType;
        private final Category category;
        private final Instant timestamp = Instant.now();
        private final Map<String, String> metadata = new HashMap<>();
        private String component = "unknown";
        private String operation = "unknown";
        private String data;
        private String clientIp;
        private String userId;
        private boolean suspicious = false;
        private RiskLevel riskLevel = RiskLevel.LOW;
        private String requestUri;
        private String httpMethod;

        private Builder(ActivityEventType type, Category category) {
            this.eventType = type;
            this.category = category;
        }

        public Builder component(String component) {
            this.component = component;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder data(String data) {
            this.data = data;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder suspicious(boolean suspicious) {
            this.suspicious = suspicious;
            return this;
        }

        public Builder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder requestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder meta(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public ActivityEvent build() {
            return new ActivityEvent(this);
        }
    }
}
