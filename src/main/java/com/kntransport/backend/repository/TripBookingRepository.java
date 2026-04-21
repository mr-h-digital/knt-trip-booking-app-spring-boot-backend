package com.kntransport.backend.repository;

import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TripBookingRepository extends JpaRepository<TripBooking, UUID> {
    Page<TripBooking> findByCommuterOrderByDateDescTimeDesc(User commuter, Pageable pageable);
}
