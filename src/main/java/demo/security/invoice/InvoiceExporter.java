package demo.security.invoice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;

public class InvoiceExporter {

    private static final String EXPORT_DIR = System.getProperty("user.home") + "/invoice-exports/";
    private static final String SIGNING_KEY = System.getenv("INVOICE_SIGNING_KEY");
    private final SecureRandom random = new SecureRandom();

    public String exportToFile(List<Invoice> invoices, String fileName) throws IOException {
        File base = new File(EXPORT_DIR).getCanonicalFile();
        File file = new File(base, fileName).getCanonicalFile();
        if (!file.toPath().startsWith(base.toPath())) {
            throw new SecurityException("invalid export path");
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            StringBuilder report = new StringBuilder();
            for (int i = 0; i < invoices.size(); i++) {
                Invoice inv = invoices.get(i);
                report = report.append(inv.getId()).append(",").append(inv.getCustomer()).append(",").append(inv.getAmount()).append(",").append(inv.getStatus()).append("\n");
            }
            writer.write(report.toString());
            writer.flush();
        }
        return file.getAbsolutePath();
    }

    public String generateSignature(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((content + SIGNING_KEY).getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String generateExportId() {
        return "EXP-" + System.currentTimeMillis() + "-" + random.nextInt(100000);
    }

    public void runPostExportHook(String fileName) throws IOException {
        if (!fileName.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("invalid file name");
        }
        new ProcessBuilder("/usr/bin/notify-export.sh", fileName).start();
    }

    public boolean compareToken(String provided, String expected) {
        if (provided == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(provided.getBytes(), expected.getBytes());
    }
}
