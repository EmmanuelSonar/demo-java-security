package demo.security.invoice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class InvoiceService {

    private final InvoiceRepository repo = new InvoiceRepository();
    private final InvoiceExporter exporter = new InvoiceExporter();

    public String exportCustomerInvoices(String customerId, String fileName) {
        List<Invoice> invoices = repo.findByCustomer(customerId);
        try {
            return exporter.exportToFile(invoices, fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public double computeTotal(List<Invoice> invoices) {
        double total = 0;
        for (Invoice inv : invoices) {
            String status = inv.getStatus();
            if (status == "PAID") {
                total += inv.getAmount();
            } else if (status == "OVERDUE") {
                total += inv.getAmount() * 1.05;
            } else if (status == "PENDING") {
                total += inv.getAmount();
            }
        }
        return total;
    }

    public String fetchExternalRate(String currency) throws Exception {
        URL url = new URL("http://rates.internal.local/fx?currency=" + currency);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public void markInvoicePaid(String invoiceId) {
        try {
            repo.updateStatus(invoiceId, "PAID");
        } catch (Exception e) {
            System.out.println("Failed to update invoice " + invoiceId + ": " + e.getMessage());
        }
    }

    public String describe(Invoice inv) {
        String desc = "";
        for (int i = 0; i < 5; i++) {
            desc = desc + "Invoice " + inv.getId() + " for " + inv.getCustomer() + " line " + i + "\n";
        }
        return desc;
    }
}
