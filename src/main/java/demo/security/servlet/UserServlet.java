package demo.security.servlet;

import demo.security.logging.SecurityAuditLogger;
import demo.security.util.DBUtils;
import demo.security.util.SessionHeader;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/users")
public class UserServlet extends HttpServlet {

    private final SecurityAuditLogger auditLogger = SecurityAuditLogger.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user = request.getParameter("username");
        String clientIp = request.getRemoteAddr();

        auditLogger.logUserInputParameter("UserServlet", "username", user, clientIp);

        try {
            DBUtils db = new DBUtils();
            List<String> users = db.findUsers(user);
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            users.forEach((result) -> {
                        out.print("<h2>User "+result+ "</h2>");
            });
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SessionHeader getSessionHeader(HttpServletRequest request) {
        String sessionAuth = request.getHeader("Session-Auth");
        String clientIp = request.getRemoteAddr();

        if (sessionAuth != null) {
            auditLogger.logHeaderExtracted("UserServlet", "Session-Auth", sessionAuth, clientIp);
            try {
                byte[] decoded = Base64.decodeBase64(sessionAuth);
                auditLogger.logDeserialization("UserServlet", "Session-Auth header", clientIp);
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(decoded));
                return (SessionHeader) in.readObject();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        SessionHeader sessionHeader = getSessionHeader(request);
        if (sessionHeader == null) return;
        String user = sessionHeader.getUsername();
        String clientIp = request.getRemoteAddr();

        auditLogger.logUserInputParameter("UserServlet", "sessionHeader.username", user, clientIp);

        try {
            DBUtils db = new DBUtils();
            List<String> users = db.findUsers(user);
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            users.forEach((result) -> {
                out.print("<h2>User "+result+ "</h2>");
            });
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
