package com.kntransport.backend.repository;

import com.kntransport.backend.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    Optional<Quote> findByReferenceIdAndReferenceType(UUID referenceId, Quote.ReferenceType referenceType);
}
