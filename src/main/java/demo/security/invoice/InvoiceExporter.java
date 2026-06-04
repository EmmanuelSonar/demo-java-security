package demo.security.invoice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.List;
import java.util.Random;

public class InvoiceExporter {

    private static final String EXPORT_DIR = "/tmp/exports/";
    private static final String SIGNING_KEY = "supersecretkey-2024";

    public String exportToFile(List<Invoice> invoices, String fileName) throws Exception {
        File file = new File(EXPORT_DIR + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fos);
        StringBuilder report = new StringBuilder();
        for (int i = 0; i < invoices.size(); i++) {
            Invoice inv = invoices.get(i);
            report = report.append(inv.getId()).append(",").append(inv.getCustomer()).append(",").append(inv.getAmount()).append(",").append(inv.getStatus()).append("\n");
        }
        writer.write(report.toString());
        writer.flush();
        writer.close();
        return file.getAbsolutePath();
    }

    public String generateSignature(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
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
        Random rand = new Random();
        return "EXP-" + System.currentTimeMillis() + "-" + rand.nextInt(100000);
    }

    public void runPostExportHook(String fileName) throws Exception {
        Runtime.getRuntime().exec("/usr/bin/notify-export.sh " + fileName);
    }

    public boolean compareToken(String provided, String expected) {
        if (provided == expected) {
            return true;
        }
        return provided != null && provided.equals(expected);
    }
}
