package demo.security.servlet;

import demo.security.logging.SecurityAuditLogger;
import demo.security.util.Utils;

import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/scripts")
public class ScriptServlet extends HttpServlet {

    private final SecurityAuditLogger auditLogger = SecurityAuditLogger.getInstance();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String data = request.getParameter("data");
        String clientIp = request.getRemoteAddr();

        auditLogger.logUserInputParameter("ScriptServlet", "data", data, clientIp);
        auditLogger.logScriptExecution("ScriptServlet", data, clientIp);

        try {
            Utils.executeJs(data);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}
