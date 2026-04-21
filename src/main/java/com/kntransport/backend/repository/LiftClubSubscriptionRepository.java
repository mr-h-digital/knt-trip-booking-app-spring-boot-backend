package com.kntransport.backend.repository;

import com.kntransport.backend.entity.LiftClub;
import com.kntransport.backend.entity.LiftClubSubscription;
import com.kntransport.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LiftClubSubscriptionRepository extends JpaRepository<LiftClubSubscription, UUID> {
    boolean existsByLiftClubAndUser(LiftClub liftClub, User user);
    long countByLiftClub(LiftClub liftClub);
}
