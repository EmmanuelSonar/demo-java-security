package demo.security.logging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class AuditLogStore {

    private static final int MAX_EVENTS = 10_000;
    private static final AuditLogStore INSTANCE = new AuditLogStore();

    private final CopyOnWriteArrayList<ActivityEvent> events = new CopyOnWriteArrayList<>();
    private final Map<ActivityEventType, AtomicLong> countsByType = new ConcurrentHashMap<>();
    private final Map<ActivityEvent.RiskLevel, AtomicLong> countsByRisk = new ConcurrentHashMap<>();
    private final AtomicLong totalEvents = new AtomicLong(0);
    private final AtomicLong suspiciousEvents = new AtomicLong(0);

    private AuditLogStore() {
        for (ActivityEventType type : ActivityEventType.values()) {
            countsByType.put(type, new AtomicLong(0));
        }
        for (ActivityEvent.RiskLevel level : ActivityEvent.RiskLevel.values()) {
            countsByRisk.put(level, new AtomicLong(0));
        }
    }

    public static AuditLogStore getInstance() {
        return INSTANCE;
    }

    public void store(ActivityEvent event) {
        if (events.size() >= MAX_EVENTS) {
            events.remove(0);
        }
        events.add(event);
        totalEvents.incrementAndGet();
        countsByType.get(event.getEventType()).incrementAndGet();
        countsByRisk.get(event.getRiskLevel()).incrementAndGet();
        if (event.isSuspicious()) {
            suspiciousEvents.incrementAndGet();
        }
    }

    public List<ActivityEvent> getRecent(int limit) {
        List<ActivityEvent> all = new ArrayList<>(events);
        int from = Math.max(0, all.size() - limit);
        List<ActivityEvent> recent = all.subList(from, all.size());
        List<ActivityEvent> result = new ArrayList<>(recent);
        Collections.reverse(result);
        return result;
    }

    public List<ActivityEvent> getByCategory(ActivityEvent.Category category, int limit) {
        return events.stream()
            .filter(e -> e.getCategory() == category)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ActivityEvent> getByType(ActivityEventType type, int limit) {
        return events.stream()
            .filter(e -> e.getEventType() == type)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ActivityEvent> getByComponent(String component, int limit) {
        return events.stream()
            .filter(e -> component.equalsIgnoreCase(e.getComponent()))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ActivityEvent> getSuspicious(int limit) {
        return events.stream()
            .filter(ActivityEvent::isSuspicious)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ActivityEvent> getByRiskLevel(ActivityEvent.RiskLevel level, int limit) {
        return events.stream()
            .filter(e -> e.getRiskLevel() == level)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ActivityEvent> getInTimeRange(Instant from, Instant to, int limit) {
        return events.stream()
            .filter(e -> !e.getTimestamp().isBefore(from) && !e.getTimestamp().isAfter(to))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public long getTotalCount() { return totalEvents.get(); }
    public long getSuspiciousCount() { return suspiciousEvents.get(); }

    public long getCountByType(ActivityEventType type) {
        return countsByType.getOrDefault(type, new AtomicLong(0)).get();
    }

    public long getCountByRiskLevel(ActivityEvent.RiskLevel level) {
        return countsByRisk.getOrDefault(level, new AtomicLong(0)).get();
    }

    public Map<ActivityEventType, Long> getTypeStatistics() {
        Map<ActivityEventType, Long> stats = new ConcurrentHashMap<>();
        countsByType.forEach((type, count) -> stats.put(type, count.get()));
        return stats;
    }

    public void clear() {
        events.clear();
        totalEvents.set(0);
        suspiciousEvents.set(0);
        countsByType.values().forEach(c -> c.set(0));
        countsByRisk.values().forEach(c -> c.set(0));
    }
}
