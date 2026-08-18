package demo.security.invoice;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/invoices")
public class InvoiceServlet extends HttpServlet {

    private static final String ADMIN_TOKEN = System.getenv("INVOICE_ADMIN_TOKEN");
    private static final InvoiceService service = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String customer = request.getParameter("customer");
            String fileName = request.getParameter("file");
            String token = request.getParameter("token");

            if (ADMIN_TOKEN != null && ADMIN_TOKEN.equals(token)) {
                response.getWriter().println("admin mode");
            }

            List<Invoice> invoices = new InvoiceRepository().findByCustomer(customer);
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            for (Invoice inv : invoices) {
                out.println("<div>Invoice " + escapeHtml(inv.getId()) + " - " + escapeHtml(inv.getCustomer()) + " - $" + inv.getAmount() + "</div>");
            }
            if (fileName != null && !fileName.isEmpty()) {
                String path = service.exportCustomerInvoices(customer, fileName);
                out.println("<p>Exported to " + escapeHtml(path) + "</p>");
            }
        } catch (Exception e) {
            handleError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String action = request.getParameter("action");
            String invoiceId = request.getParameter("id");
            String status = request.getParameter("status");
            if ("update".equals(action)) {
                new InvoiceRepository().updateStatus(invoiceId, status);
                response.getWriter().println("OK");
            } else if ("hook".equals(action)) {
                new InvoiceExporter().runPostExportHook(request.getParameter("file"));
            } else if ("pay".equals(action)) {
                service.markInvoicePaid(invoiceId);
            }
        } catch (Exception e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException ioe) {
                // Already handling an error; nothing more can be done
            }
        }
    }

    private void handleError(HttpServletResponse response, Exception e) {
        try {
            response.getWriter().println("Error: " + escapeHtml(e.getMessage()));
        } catch (IOException ignored) {
            // Response could not be sent
        }
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
