package com.kntransport.backend.service;

import com.kntransport.backend.dto.CreateLiftClubRequest;
import com.kntransport.backend.dto.LiftClubDto;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.entity.LiftClub;
import com.kntransport.backend.entity.LiftClubSubscription;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.LiftClubRepository;
import com.kntransport.backend.repository.LiftClubSubscriptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

@Service
public class LiftClubService {

    private final LiftClubRepository liftClubRepository;
    private final LiftClubSubscriptionRepository subscriptionRepository;
    private final UserService userService;

    public LiftClubService(LiftClubRepository liftClubRepository,
                           LiftClubSubscriptionRepository subscriptionRepository,
                           UserService userService) {
        this.liftClubRepository = liftClubRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userService = userService;
    }

    public PagedResponse<LiftClubDto> getLiftClubs(int page, int size) {
        var pageResult = liftClubRepository.findAllByOrderByIdDesc(PageRequest.of(page, size));
        return PagedResponse.from(pageResult, lc ->
                LiftClubDto.from(lc, subscriptionRepository.countByLiftClub(lc)));
    }

    public LiftClubDto getLiftClub(String id) {
        LiftClub lc = findLiftClub(id);
        return LiftClubDto.from(lc, subscriptionRepository.countByLiftClub(lc));
    }

    public LiftClubDto createLiftClub(String email, CreateLiftClubRequest req) {
        User creator = userService.getByEmail(email);

        LiftClub lc = new LiftClub();
        lc.setCreator(creator);
        lc.setTitle(req.title());
        lc.setPickupArea(req.pickupArea());
        lc.setDropArea(req.dropArea());
        lc.setDepartureTime(LocalTime.parse(req.departureTime()));
        if (req.returnTime() != null && !req.returnTime().isBlank()) {
            lc.setReturnTime(LocalTime.parse(req.returnTime()));
        }
        lc.setDaysOfWeek(req.daysOfWeek());
        lc.setMaxPassengers(req.maxPassengers());
        lc.setDescription(req.description() != null ? req.description() : "");
        lc.setStatus(LiftClub.LiftClubStatus.OPEN);

        return LiftClubDto.from(liftClubRepository.save(lc), 0);
    }

    @Transactional
    public void subscribe(String email, String liftClubId) {
        User user = userService.getByEmail(email);
        LiftClub lc = findLiftClub(liftClubId);

        if (subscriptionRepository.existsByLiftClubAndUser(lc, user)) {
            throw new BadRequestException("Already subscribed to this lift club");
        }

        long currentCount = subscriptionRepository.countByLiftClub(lc);
        if (currentCount >= lc.getMaxPassengers()) {
            throw new BadRequestException("This lift club is full");
        }

        LiftClubSubscription sub = new LiftClubSubscription();
        sub.setLiftClub(lc);
        sub.setUser(user);
        subscriptionRepository.save(sub);

        long newCount = currentCount + 1;
        if (newCount >= lc.getMaxPassengers() && lc.getStatus() == LiftClub.LiftClubStatus.OPEN) {
            lc.setStatus(LiftClub.LiftClubStatus.QUOTA_MET);
            liftClubRepository.save(lc);
        }
    }

    private LiftClub findLiftClub(String id) {
        return liftClubRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Lift club not found: " + id));
    }
}
