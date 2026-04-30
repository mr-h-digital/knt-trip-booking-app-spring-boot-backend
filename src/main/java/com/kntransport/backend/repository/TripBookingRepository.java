package com.kntransport.backend.repository;

import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface TripBookingRepository extends JpaRepository<TripBooking, UUID> {
    Page<TripBooking> findByCommuterOrderByDateDescTimeDesc(User commuter, Pageable pageable);
    Page<TripBooking> findByStatusOrderByDateAscTimeAsc(TripBooking.TripStatus status, Pageable pageable);

    Page<TripBooking> findByStatusInOrderByDateAscTimeAsc(java.util.Collection<TripBooking.TripStatus> statuses, Pageable pageable);

    java.util.Optional<TripBooking> findByIdAndStatusIn(UUID id, java.util.Collection<TripBooking.TripStatus> statuses);

    List<TripBooking> findByStatusInAndDateBefore(java.util.Collection<TripBooking.TripStatus> statuses, java.time.LocalDate date);

    long countByStatus(TripBooking.TripStatus status);

    // ── Driver queries ────────────────────────────────────────────────────────
    Page<TripBooking> findByDriverOrderByDateDescTimeDesc(User driver, Pageable pageable);
    List<TripBooking> findByDriverAndStatus(User driver, TripBooking.TripStatus status);
    List<TripBooking> findByDriverAndDate(User driver, java.time.LocalDate date);

    @Query("SELECT COALESCE(SUM(t.quotedAmount), 0) FROM TripBooking t WHERE t.driver = :driver AND t.status = 'COMPLETED'")
    double sumCompletedEarningsByDriver(@Param("driver") User driver);

    long countByDriverAndStatus(User driver, TripBooking.TripStatus status);

    @Query("SELECT COALESCE(SUM(t.quotedAmount), 0) FROM TripBooking t WHERE t.status = 'COMPLETED'")
    double sumCompletedRevenue();

    @Query("SELECT COALESCE(SUM(t.quotedAmount), 0) FROM TripBooking t WHERE t.status IN ('QUOTE_ACCEPTED','CONFIRMED','IN_PROGRESS')")
    double sumOutstandingRevenue();

    @Query("SELECT FUNCTION('TO_CHAR', t.date, 'YYYY-MM') as month, COALESCE(SUM(t.quotedAmount), 0), COUNT(t) " +
           "FROM TripBooking t WHERE t.status = 'COMPLETED' GROUP BY FUNCTION('TO_CHAR', t.date, 'YYYY-MM') ORDER BY 1 DESC")
    List<Object[]> revenueByMonth(Pageable pageable);
}
