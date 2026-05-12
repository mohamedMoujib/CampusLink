package org.example.campusLink.entities;

import java.sql.Timestamp;

public class Invoices {
    private int id;
    private int paymentId;
    private Timestamp invoiceDate;
    private String details;

    public Invoices() {
    }

    public Invoices(int paymentId, Timestamp invoiceDate, String details) {
        this.paymentId = paymentId;
        this.invoiceDate = invoiceDate;
        this.details = details;
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

    public void setDate(Timestamp timestamp) {
    }
}
