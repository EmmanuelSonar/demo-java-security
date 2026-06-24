package demo.security.servlet;

import demo.security.logging.ActivityEvent;
import demo.security.logging.ActivityEventType;
import demo.security.logging.AuditLogStore;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/logs")
public class LogServlet extends HttpServlet {

    private final AuditLogStore store = AuditLogStore.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String category = request.getParameter("category");
        String type = request.getParameter("type");
        String component = request.getParameter("component");
        String suspicious = request.getParameter("suspicious");
        String risk = request.getParameter("risk");
        int limit = parseLimit(request.getParameter("limit"), 50);

        List<ActivityEvent> events = resolveEvents(category, type, component, suspicious, risk, limit);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.print(toJson(events));
        out.flush();
    }

    private List<ActivityEvent> resolveEvents(String category, String type, String component,
                                               String suspicious, String risk, int limit) {
        if ("true".equalsIgnoreCase(suspicious)) {
            return store.getSuspicious(limit);
        }
        if (component != null && !component.isEmpty()) {
            return store.getByComponent(component, limit);
        }
        if (type != null && !type.isEmpty()) {
            try {
                ActivityEventType eventType = ActivityEventType.valueOf(type.toUpperCase());
                return store.getByType(eventType, limit);
            } catch (IllegalArgumentException ignored) {}
        }
        if (category != null && !category.isEmpty()) {
            try {
                ActivityEvent.Category cat = ActivityEvent.Category.valueOf(category.toUpperCase());
                return store.getByCategory(cat, limit);
            } catch (IllegalArgumentException ignored) {}
        }
        if (risk != null && !risk.isEmpty()) {
            try {
                ActivityEvent.RiskLevel level = ActivityEvent.RiskLevel.valueOf(risk.toUpperCase());
                return store.getByRiskLevel(level, limit);
            } catch (IllegalArgumentException ignored) {}
        }
        return store.getRecent(limit);
    }

    private int parseLimit(String param, int defaultValue) {
        if (param == null) return defaultValue;
        try {
            int v = Integer.parseInt(param);
            return Math.min(Math.max(v, 1), 500);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String toJson(List<ActivityEvent> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"total\":").append(store.getTotalCount());
        sb.append(",\"suspicious\":").append(store.getSuspiciousCount());
        sb.append(",\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(eventToJson(events.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    private String eventToJson(ActivityEvent e) {
        return "{" +
            "\"id\":\"" + escape(e.getEventId()) + "\"," +
            "\"timestamp\":\"" + e.getTimestamp() + "\"," +
            "\"type\":\"" + e.getEventType() + "\"," +
            "\"category\":\"" + e.getCategory() + "\"," +
            "\"component\":\"" + escape(e.getComponent()) + "\"," +
            "\"operation\":\"" + escape(e.getOperation()) + "\"," +
            "\"clientIp\":\"" + escape(e.getClientIp()) + "\"," +
            "\"riskLevel\":\"" + e.getRiskLevel() + "\"," +
            "\"suspicious\":" + e.isSuspicious() + "," +
            "\"requestUri\":\"" + escape(e.getRequestUri()) + "\"," +
            "\"httpMethod\":\"" + escape(e.getHttpMethod()) + "\"" +
            "}";
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
