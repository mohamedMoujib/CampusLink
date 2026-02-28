package org.example.campusLink.entities;

import org.example.campusLink.enumeration.Method;

public class Payments {
    private int id;
    private int reservationId;
    private float amount;
    private Method method;
    private Double meetingLat;
    private Double meetingLng;
    private String meetingAddress;

public Payments(){}


    public Payments(int id, int reservationId, float amount, Method method, Double meetingLat, Double meetingLng, String meetingAddress) {
        this.id = id;
        this.reservationId = reservationId;
        this.amount = amount;
        this.method = method;
        this.meetingLat = meetingLat;
        this.meetingLng = meetingLng;
        this.meetingAddress = meetingAddress;
    }

    public Double getMeetingLat() {
        return meetingLat;
    }

    public void setMeetingLat(Double meetingLat) {
        this.meetingLat = meetingLat;
    }

    public Double getMeetingLng() {
        return meetingLng;
    }

    public void setMeetingLng(Double meetingLng) {
        this.meetingLng = meetingLng;
    }

    public String getMeetingAddress() {
        return meetingAddress;
    }

    public void setMeetingAddress(String meetingAddress) {
        this.meetingAddress = meetingAddress;
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


    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setMethod(Method method) {
        this.method = method;
    }


    @Override
    public String toString() {
        return "Payments{" +
                "id=" + id +
                ", reservationId=" + reservationId +
                ", amount=" + amount +
                ", method=" + method +
                ", meetingLat=" + meetingLat +
                ", meetingLng=" + meetingLng +
                ", meetingAddress='" + meetingAddress + '\'' +
                '}';
    }

    public int getId() {
    return id;
    }

    public void setId(int id) {
    this.id = id;
    }
}
