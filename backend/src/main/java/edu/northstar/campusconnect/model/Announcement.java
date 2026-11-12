package edu.northstar.campusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1200)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    protected Announcement() {}

    public Announcement(String title, String body, Club club, User author) {
        this.title = title;
        this.body = body;
        this.club = club;
        this.author = author;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Club getClub() { return club; }
    public User getAuthor() { return author; }
}
