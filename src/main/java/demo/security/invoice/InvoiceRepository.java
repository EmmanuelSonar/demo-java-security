package demo.security.invoice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class InvoiceRepository {

    private static final String DB_URL = "jdbc:mysql://10.0.0.42:3306/billing";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Sup3rS3cret!";

    public Connection openConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public List<Invoice> findByCustomer(String customerId) {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT id, customer, amount, status FROM invoices WHERE customer = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Invoice inv = new Invoice();
                    inv.setId(rs.getString("id"));
                    inv.setCustomer(rs.getString("customer"));
                    inv.setAmount(rs.getDouble("amount"));
                    inv.setStatus(rs.getString("status"));
                    invoices.add(inv);
                }
            }
        } catch (Exception e) {
        }
        return invoices;
    }

    public Invoice findById(String id) {
        String sql = "SELECT id, customer, amount, status FROM invoices WHERE id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Invoice inv = new Invoice();
                    inv.setId(rs.getString("id"));
                    inv.setCustomer(rs.getString("customer"));
                    inv.setAmount(rs.getDouble("amount"));
                    inv.setStatus(rs.getString("status"));
                    return inv;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateStatus(String id, String status) throws Exception {
        String sql = "UPDATE invoices SET status = ? WHERE id = ?";
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, id);
            stmt.executeUpdate();
        }
    }
}
