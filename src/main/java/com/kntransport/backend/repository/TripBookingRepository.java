package com.kntransport.backend.repository;

import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface TripBookingRepository extends JpaRepository<TripBooking, UUID> {
    Page<TripBooking> findByCommuterOrderByDateDescTimeDesc(User commuter, Pageable pageable);

    long countByStatus(TripBooking.TripStatus status);

    @Query("SELECT COALESCE(SUM(t.quotedAmount), 0) FROM TripBooking t WHERE t.status = 'COMPLETED'")
    double sumCompletedRevenue();

    @Query("SELECT COALESCE(SUM(t.quotedAmount), 0) FROM TripBooking t WHERE t.status IN ('QUOTE_ACCEPTED','CONFIRMED','IN_PROGRESS')")
    double sumOutstandingRevenue();

    @Query("SELECT FUNCTION('TO_CHAR', t.date, 'YYYY-MM') as month, COALESCE(SUM(t.quotedAmount), 0), COUNT(t) " +
           "FROM TripBooking t WHERE t.status = 'COMPLETED' GROUP BY FUNCTION('TO_CHAR', t.date, 'YYYY-MM') ORDER BY 1 DESC")
    List<Object[]> revenueByMonth(Pageable pageable);
}
