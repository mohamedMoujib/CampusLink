package org.example.campusLink.entities;

import java.sql.Timestamp;

public class Invoices {
    private int id;
    private int paymentId;
    private Timestamp invoiceDate;
    private String details;
    private Integer userId; // ADD T


    public Invoices() {
    }

    public Invoices(int paymentId, Timestamp invoiceDate, String details) {
        this.paymentId = paymentId;
        this.invoiceDate = invoiceDate;
        this.details = details;
    }
    // New constructor with userId
    public Invoices(int paymentId, Timestamp invoiceDate, String details, Integer userId) {
        this.paymentId = paymentId;
        this.invoiceDate = invoiceDate;
        this.details = details;
        this.userId = userId;
    }

    // Full constructor for reading from DB
    public Invoices(int invId, int payId, Timestamp invDate, String details, Integer userId) {
        this.id = invId;
        this.paymentId = payId;
        this.invoiceDate = invDate;
        this.details = details;
        this.userId = userId;
    }
    public Invoices(int invId, int payId, Timestamp invDate, String details) {
        this.id = invId;
        this.paymentId = payId;
        this.invoiceDate = invDate;
        this.details = details;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public Timestamp getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Timestamp invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "Invoices{" +
                "paymentId=" + paymentId +
                ", invoiceDate=" + invoiceDate +
                ", details='" + details + '\'' +
                '}';
    }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public void setDate(Timestamp timestamp) {
    }
}
