package com.example.EcoGo.repository;

import com.example.EcoGo.model.ChatBooking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatBookingRepository extends MongoRepository<ChatBooking, String> {

    Optional<ChatBooking> findByBookingId(String bookingId);

    Optional<ChatBooking> findByTripId(String tripId);

    List<ChatBooking> findByUserIdOrderByCreatedAtDesc(String userId);
}
