package com.kntransport.backend.repository;

import com.kntransport.backend.entity.LiftClub;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LiftClubRepository extends JpaRepository<LiftClub, UUID> {
    Page<LiftClub> findAllByOrderByIdDesc(Pageable pageable);
}
