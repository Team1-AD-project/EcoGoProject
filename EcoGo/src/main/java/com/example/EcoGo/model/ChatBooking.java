package com.example.EcoGo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_bookings")
public class ChatBooking {

    @Id
    private String id;

    private String bookingId;
    private String tripId;       // linked Trip ID (from TripService)
    private String userId;
    private String fromName;
    private String toName;
    private String departAt;
    private int passengers;
    private String status; // pending | confirmed | cancelled
    private Instant createdAt;

    public ChatBooking() {
        // Empty constructor intentionally left blank.
        // Required by Spring Data MongoDB for deserialization of documents from the database.
        // Fields are populated via setters or reflection during document instantiation.
    }

    // --- Getters/Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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
