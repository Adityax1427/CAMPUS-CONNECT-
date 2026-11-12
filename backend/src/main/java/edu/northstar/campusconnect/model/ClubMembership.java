package edu.northstar.campusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "club_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "user_id"}))
public class ClubMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    protected ClubMembership() {}

    public ClubMembership(Club club, User user) {
        this.club = club;
        this.user = user;
    }

    public Long getId() { return id; }
}
