package demo.security.invoice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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
        Connection conn = null;
        try {
            conn = openConnection();
            Statement stmt = conn.createStatement();
            String sql = "SELECT id, customer, amount, status FROM invoices WHERE customer = '" + customerId + "'";
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Invoice inv = new Invoice();
                inv.setId(rs.getString("id"));
                inv.setCustomer(rs.getString("customer"));
                inv.setAmount(rs.getDouble("amount"));
                inv.setStatus(rs.getString("status"));
                invoices.add(inv);
            }
        } catch (Exception e) {
        }
        return invoices;
    }

    public Invoice findById(String id) {
        try {
            Connection conn = openConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, customer, amount, status FROM invoices WHERE id = " + id);
            if (rs.next()) {
                Invoice inv = new Invoice();
                inv.setId(rs.getString("id"));
                inv.setCustomer(rs.getString("customer"));
                inv.setAmount(rs.getDouble("amount"));
                inv.setStatus(rs.getString("status"));
                return inv;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateStatus(String id, String status) throws Exception {
        Connection conn = openConnection();
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE invoices SET status = '" + status + "' WHERE id = '" + id + "'");
    }
}
