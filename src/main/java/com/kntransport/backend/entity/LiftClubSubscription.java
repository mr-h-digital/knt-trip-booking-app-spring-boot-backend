package com.kntransport.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "lift_club_subscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"lift_club_id", "user_id"}))
public class LiftClubSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lift_club_id", nullable = false)
    private LiftClub liftClub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "quote_accepted")
    private Boolean quoteAccepted;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LiftClub getLiftClub() { return liftClub; }
    public void setLiftClub(LiftClub liftClub) { this.liftClub = liftClub; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Boolean getQuoteAccepted() { return quoteAccepted; }
    public void setQuoteAccepted(Boolean quoteAccepted) { this.quoteAccepted = quoteAccepted; }
}
