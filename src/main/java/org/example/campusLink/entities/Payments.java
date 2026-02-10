package org.example.campusLink.entities;

import org.example.campusLink.enumeration.Method;
import org.example.campusLink.enumeration.Status;

public class Payments {
    private int id;
    private int reservationId;
    private float amount;
    private Method method;
    private Status status;
public Payments(){}

public Payments(int reservationId, float amount, Method method, Status status){
    this.reservationId = reservationId;
    this.amount = amount;
    this.method = method;
    this.status = status;
}

    public Payments(int payId, int resId, Float amount, String method, String status) {
    this.id = payId;
        this.reservationId = resId;
        this.amount = amount;
        this.method = Method.valueOf(method.toUpperCase());
        this.status = Status.valueOf(status);
    }

    public int getReservationId() {
        return reservationId;
    }

    public float getAmount() {
        return amount;
    }

    public Method getMethod() {
        return method;
    }

    public Status getStatus() {
        return status;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Payments{" +
                "reservationId=" + reservationId +
                ", amount=" + amount +
                ", method=" + method +
                ", status=" + status +
                '}';
    }
}
