package demo.security.invoice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class InvoiceService {

    private static final Logger logger = Logger.getLogger(InvoiceService.class.getName());

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
            if ("PAID".equals(status)) {
                total += inv.getAmount();
            } else if ("OVERDUE".equals(status)) {
                total += inv.getAmount() * 1.05;
            } else if ("PENDING".equals(status)) {
                total += inv.getAmount();
            }
        }
        return total;
    }

    private static final List<String> ALLOWED_CURRENCIES = Arrays.asList("USD", "EUR", "GBP", "JPY");

    public String fetchExternalRate(String currency) throws IOException, URISyntaxException {
        if (!ALLOWED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("unsupported currency: " + currency);
        }
        URL url = new URI("https://rates.internal.local/fx?currency=" + currency).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public void markInvoicePaid(String invoiceId) {
        try {
            repo.updateStatus(invoiceId, "PAID");
        } catch (Exception e) {
            logger.warning("Failed to update invoice " + invoiceId + ": " + e.getMessage());
        }
    }

    public String describe(Invoice inv) {
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            desc.append("Invoice ").append(inv.getId()).append(" for ").append(inv.getCustomer()).append(" line ").append(i).append("\n");
        }
        return desc.toString();
    }
}
