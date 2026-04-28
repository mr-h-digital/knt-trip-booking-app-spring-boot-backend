package com.kntransport.backend.repository;

import com.kntransport.backend.entity.LiftClub;
import com.kntransport.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LiftClubRepository extends JpaRepository<LiftClub, UUID> {
    Page<LiftClub> findAllByOrderByIdDesc(Pageable pageable);
    long countByStatus(LiftClub.LiftClubStatus status);
    List<LiftClub> findByCreatorOrderByIdDesc(User creator);
}
