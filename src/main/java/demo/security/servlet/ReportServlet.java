package demo.security.servlet;

import demo.security.util.DBUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@WebServlet("/reports")
public class ReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        try {
            String reportType = request.getParameter("type");
            String startDate = request.getParameter("startDate");
            String endDate = request.getParameter("endDate");
            String format = request.getParameter("format");
            String userId = request.getParameter("userId");

            if (reportType == null || reportType.isEmpty()) {
                out.println("<h1>Error: Report type is required</h1>");
                out.println("<p>Please provide a 'type' parameter: users, transactions, or activity</p>");
                return;
            }

            if (format == null || format.isEmpty()) {
                format = "html";
            }

            List<String> reportData = new ArrayList<>();

            switch (reportType) {
                case "users":
                    reportData = generateUserReport(userId);
                    break;
                case "transactions":
                    reportData = generateTransactionReport(startDate, endDate);
                    break;
                case "activity":
                    reportData = generateActivityReport(startDate, endDate, userId);
                    break;
                default:
                    out.println("<h1>Error: Unknown report type</h1>");
                    return;
            }

            renderReport(out, reportType, reportData, format);

        } catch (Exception e) {
            out.println("<h1>Error generating report</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
            e.printStackTrace(out);
        } finally {
            out.close();
        }
    }

    private List<String> generateUserReport(String userId) throws Exception {
        List<String> data = new ArrayList<>();

        Connection conn = DriverManager.getConnection(
                "mYJDBCUrl", "myJDBCUser", "myJDBCPass");

        String query;
        if (userId != null && !userId.isEmpty()) {
            query = "SELECT userid, username, email FROM users WHERE userid = " + userId;
        } else {
            query = "SELECT userid, username, email FROM users";
        }

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        data.add("User ID | Username | Email");
        data.add("------|----------|-------");

        while (rs.next()) {
            String line = rs.getString(1) + " | " +
                         rs.getString(2) + " | " +
                         rs.getString(3);
            data.add(line);
        }

        rs.close();
        stmt.close();
        conn.close();

        return data;
    }

    private List<String> generateTransactionReport(String startDate, String endDate) throws Exception {
        List<String> data = new ArrayList<>();

        Connection conn = DriverManager.getConnection(
                "mYJDBCUrl", "myJDBCUser", "myJDBCPass");

        StringBuilder query = new StringBuilder(
            "SELECT transaction_id, amount, status, timestamp FROM transactions WHERE 1=1");

        if (startDate != null && !startDate.isEmpty()) {
            query.append(" AND timestamp >= '").append(startDate).append("'");
        }

        if (endDate != null && !endDate.isEmpty()) {
            query.append(" AND timestamp <= '").append(endDate).append("'");
        }

        query.append(" ORDER BY timestamp DESC");

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query.toString());

        data.add("Transaction ID | Amount | Status | Timestamp");
        data.add("----------------|--------|--------|----------");

        while (rs.next()) {
            String line = rs.getString(1) + " | " +
                         rs.getBigDecimal(2) + " | " +
                         rs.getString(3) + " | " +
                         rs.getTimestamp(4);
            data.add(line);
        }

        rs.close();
        stmt.close();
        conn.close();

        return data;
    }

    private List<String> generateActivityReport(String startDate, String endDate, String userId)
            throws Exception {
        List<String> data = new ArrayList<>();

        Connection conn = DriverManager.getConnection(
                "mYJDBCUrl", "myJDBCUser", "myJDBCPass");

        StringBuilder query = new StringBuilder(
            "SELECT activity_id, action, performer, timestamp FROM activity_log WHERE 1=1");

        if (userId != null && !userId.isEmpty()) {
            query.append(" AND performer = '").append(userId).append("'");
        }

        if (startDate != null && !startDate.isEmpty()) {
            query.append(" AND timestamp >= '").append(startDate).append("'");
        }

        if (endDate != null && !endDate.isEmpty()) {
            query.append(" AND timestamp <= '").append(endDate).append("'");
        }

        query.append(" ORDER BY timestamp DESC LIMIT 500");

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query.toString());

        data.add("Activity ID | Action | Performer | Timestamp");
        data.add("------------|--------|-----------|----------");

        while (rs.next()) {
            String line = rs.getString(1) + " | " +
                         rs.getString(2) + " | " +
                         rs.getString(3) + " | " +
                         rs.getTimestamp(4);
            data.add(line);
        }

        rs.close();
        stmt.close();
        conn.close();

        return data;
    }

    private void renderReport(PrintWriter out, String reportType, List<String> data, String format) {
        out.println("<html>");
        out.println("<head>");
        out.println("<title>" + reportType.toUpperCase() + " Report</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 20px; }");
        out.println("table { border-collapse: collapse; width: 100%; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #4CAF50; color: white; }");
        out.println("tr:nth-child(even) { background-color: #f2f2f2; }");
        out.println(".timestamp { color: #666; font-size: 0.9em; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");

        out.println("<h1>" + reportType.toUpperCase() + " Report</h1>");
        out.println("<p class='timestamp'>Generated: " + new Date() + "</p>");

        if (data.isEmpty()) {
            out.println("<p><em>No data available for this report.</em></p>");
        } else {
            if ("csv".equalsIgnoreCase(format)) {
                out.println("<pre>");
                data.forEach(line -> out.println(line));
                out.println("</pre>");
            } else {
                out.println("<table>");
                out.println("<thead><tr>");
                String[] headers = data.get(0).split("\\|");
                for (String header : headers) {
                    out.println("<th>" + header.trim() + "</th>");
                }
                out.println("</tr></thead>");
                out.println("<tbody>");

                for (int i = 2; i < data.size(); i++) {
                    out.println("<tr>");
                    String[] cells = data.get(i).split("\\|");
                    for (String cell : cells) {
                        out.println("<td>" + cell.trim() + "</td>");
                    }
                    out.println("</tr>");
                }

                out.println("</tbody>");
                out.println("</table>");
            }
        }

        out.println("<hr>");
        out.println("<p><a href='/'>Back to Home</a></p>");
        out.println("</body>");
        out.println("</html>");
    }
}
