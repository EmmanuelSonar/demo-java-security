package demo.security.password;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/password")
public class PasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final int MIN_GENERATE_LENGTH = 12;
    private static final int MAX_GENERATE_LENGTH = 64;
    private static final int DEFAULT_GENERATE_LENGTH = 16;

    private final PasswordPolicy policy = PasswordPolicy.defaultPolicy();
    private final PasswordStrengthAnalyzer analyzer = new PasswordStrengthAnalyzer(policy);
    private final SecurePasswordGenerator generator = new SecurePasswordGenerator(policy);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int length = parseLength(request.getParameter("length"));
        char[] generated = null;
        try {
            generated = generator.generate(length);
            writeGenerateResponse(response, generated);
        } catch (Exception e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while generating the password");
            } catch (IOException ioe) {
                // Unable to send error response; nothing more can be done
            }
        } finally {
            scrub(generated);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String submitted = request.getParameter("password");
            if (submitted == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing password parameter");
                return;
            }
            char[] chars = submitted.toCharArray();
            try {
                Set<String> contextTerms = collectContextTerms(request);
                PasswordStrengthAnalyzer.Result result = analyzer.analyze(chars, contextTerms);
                writeAnalyzeResponse(response, result);
            } finally {
                scrub(chars);
            }
        } catch (Exception e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
            } catch (IOException ioe) {
                // Unable to send error response; nothing more can be done
            }
        }
    }

    /**
     * Gathers context-specific terms a password should not contain: the
     * authenticated user, and any submitted username/email. For emails the
     * local-part (before '@') is added so that "alice@corp.com" also blocks
     * passwords containing "alice".
     */
    private Set<String> collectContextTerms(HttpServletRequest request) {
        Set<String> terms = new LinkedHashSet<>();
        addContextTerm(terms, request.getRemoteUser());
        addContextTerm(terms, request.getParameter("username"));
        String email = request.getParameter("email");
        addContextTerm(terms, email);
        if (email != null) {
            int at = email.indexOf('@');
            if (at > 0) {
                addContextTerm(terms, email.substring(0, at));
            }
        }
        return terms;
    }

    private void addContextTerm(Set<String> terms, String value) {
        if (value != null && !value.trim().isEmpty()) {
            terms.add(value.trim());
        }
    }

    private int parseLength(String raw) {
        if (raw == null || raw.isEmpty()) {
            return DEFAULT_GENERATE_LENGTH;
        }
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < MIN_GENERATE_LENGTH) {
                return MIN_GENERATE_LENGTH;
            }
            if (parsed > MAX_GENERATE_LENGTH) {
                return MAX_GENERATE_LENGTH;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return DEFAULT_GENERATE_LENGTH;
        }
    }

    private void writeGenerateResponse(HttpServletResponse response, char[] generated)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write("{\"length\":");
            out.write(Integer.toString(generated.length));
            out.write(",\"password\":\"");
            for (char c : generated) {
                appendJsonEscaped(out, c);
            }
            out.write("\"}");
        }
    }

    private void writeAnalyzeResponse(HttpServletResponse response, PasswordStrengthAnalyzer.Result result)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write("{\"strength\":\"");
            out.write(result.getStrength().name());
            out.write("\",\"entropyBits\":");
            out.write(String.format(java.util.Locale.ROOT, "%.2f", result.getEntropyBits()));
            out.write(",\"compliant\":");
            out.write(Boolean.toString(result.isCompliant()));
            out.write(",\"violations\":[");
            List<String> violations = result.getViolations();
            for (int i = 0; i < violations.size(); i++) {
                if (i > 0) {
                    out.write(',');
                }
                out.write('"');
                for (char c : violations.get(i).toCharArray()) {
                    appendJsonEscaped(out, c);
                }
                out.write('"');
            }
            out.write("]}");
        }
    }

    private void appendJsonEscaped(PrintWriter out, char c) {
        switch (c) {
            case '"': out.write("\\\""); break;
            case '\\': out.write("\\\\"); break;
            case '\b': out.write("\\b"); break;
            case '\f': out.write("\\f"); break;
            case '\n': out.write("\\n"); break;
            case '\r': out.write("\\r"); break;
            case '\t': out.write("\\t"); break;
            default:
                if (c < 0x20) {
                    out.write(String.format("\\u%04x", (int) c));
                } else {
                    out.write(c);
                }
        }
    }

    private void scrub(char[] data) {
        if (data == null) {
            return;
        }
        for (int i = 0; i < data.length; i++) {
            data[i] = '\0';
        }
    }
}
