package edu.northstar.campusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class CampusEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, length = 1200)
    private String description;

    @Column(nullable = false, length = 180)
    private String venue;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    protected CampusEvent() {}

    public CampusEvent(String title, String category, String description, String venue,
                       LocalDateTime startsAt, LocalDateTime endsAt, Integer capacity,
                       String imageUrl, Club club, User organizer) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.venue = venue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacity = capacity;
        this.imageUrl = imageUrl;
        this.club = club;
        this.organizer = organizer;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getVenue() { return venue; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public Integer getCapacity() { return capacity; }
    public String getImageUrl() { return imageUrl; }
    public Club getClub() { return club; }
    public User getOrganizer() { return organizer; }

    public void update(String title, String category, String description, String venue,
                       LocalDateTime startsAt, LocalDateTime endsAt, Integer capacity,
                       String imageUrl, Club club) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.venue = venue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacity = capacity;
        this.imageUrl = imageUrl;
        this.club = club;
    }
}
