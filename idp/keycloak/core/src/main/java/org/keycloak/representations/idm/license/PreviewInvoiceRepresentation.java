package org.keycloak.representations.idm.license;


import java.util.Map;

public class PreviewInvoiceRepresentation {
    private String account_name;
    private String customer_email;
    private String currency;
    private String due_date;
    private String total;
    private String subtotal;
    private Map<String,Object> invoiceItems;

    public String getAccount_name() {
        return account_name;
    }

    public void setAccount_name(String account_name) {
        this.account_name = account_name;
    }

    public String getCustomer_email() {
        return customer_email;
    }

    public void setCustomer_email(String customer_email) {
        this.customer_email = customer_email;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDue_date() {
        return due_date;
    }

    public void setDue_date(String due_date) {
        this.due_date = due_date;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(String subtotal) {
        this.subtotal = subtotal;
    }

    public Map<String, Object> getInvoiceItems() {
        return invoiceItems;
    }

    public void setInvoiceItems(Map<String, Object> invoiceItems) {
        this.invoiceItems = invoiceItems;
    }
}