package com.example.EcoGo.dto.chatbot;

import java.time.Instant;

/**
 * DTO returned when querying a chat booking by bookingId.
 */
public class BookingDetailDto {

    private String bookingId;
    private String tripId;
    private String fromName;
    private String toName;
    private String departAt;
    private int passengers;
    private String status;
    private Instant createdAt;

    public BookingDetailDto() {}

    public BookingDetailDto(String bookingId, String tripId, String fromName, String toName,
                            String departAt, int passengers, String status, Instant createdAt) {
        this.bookingId = bookingId;
        this.tripId = tripId;
        this.fromName = fromName;
        this.toName = toName;
        this.departAt = departAt;
        this.passengers = passengers;
        this.status = status;
        this.createdAt = createdAt;
    }

    // --- Getters/Setters ---

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }

    public String getDepartAt() { return departAt; }
    public void setDepartAt(String departAt) { this.departAt = departAt; }

    public int getPassengers() { return passengers; }
    public void setPassengers(int passengers) { this.passengers = passengers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
