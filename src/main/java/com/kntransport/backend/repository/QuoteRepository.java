package com.kntransport.backend.repository;

import com.kntransport.backend.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    Optional<Quote> findByReferenceIdAndReferenceType(UUID referenceId, Quote.ReferenceType referenceType);

    /** All quotes with a date filter driven by the trip/liftclub date stored on the referencing entity. */
    @Query("SELECT q FROM Quote q WHERE q.accepted IS NOT NULL ORDER BY q.id DESC")
    List<Quote> findAllDecided();

    List<Quote> findByReferenceType(Quote.ReferenceType referenceType);
}
