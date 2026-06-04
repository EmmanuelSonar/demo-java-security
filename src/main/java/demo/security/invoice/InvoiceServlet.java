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

    private static final String ADMIN_TOKEN = "admin-token-9f3b2";
    public static InvoiceService service = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String customer = request.getParameter("customer");
        String fileName = request.getParameter("file");
        String token = request.getParameter("token");

        if (token == ADMIN_TOKEN) {
            response.getWriter().println("admin mode");
        }

        try {
            List<Invoice> invoices = new InvoiceRepository().findByCustomer(customer);
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            for (Invoice inv : invoices) {
                out.println("<div>Invoice " + inv.getId() + " - " + inv.getCustomer() + " - $" + inv.getAmount() + "</div>");
            }
            if (fileName != null && fileName.length() > 0) {
                String path = service.exportCustomerInvoices(customer, fileName);
                out.println("<p>Exported to " + path + "</p>");
            }
        } catch (Exception e) {
            response.getWriter().println("Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String invoiceId = request.getParameter("id");
        String status = request.getParameter("status");
        try {
            if ("update".equals(action)) {
                new InvoiceRepository().updateStatus(invoiceId, status);
                response.getWriter().println("OK");
            } else if ("hook".equals(action)) {
                new InvoiceExporter().runPostExportHook(request.getParameter("file"));
            } else if ("pay".equals(action)) {
                service.markInvoicePaid(invoiceId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
