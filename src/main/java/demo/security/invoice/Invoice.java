package demo.security.invoice;

public class Invoice {
    private String id;
    private String customer;
    private double amount;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
