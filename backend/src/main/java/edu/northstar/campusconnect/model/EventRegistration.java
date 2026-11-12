package edu.northstar.campusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
public class EventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CampusEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = false)
    private boolean attended;

    protected EventRegistration() {}

    public EventRegistration(CampusEvent event, User user) {
        this.event = event;
        this.user = user;
        this.registeredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public CampusEvent getEvent() { return event; }
    public User getUser() { return user; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public boolean isAttended() { return attended; }
    public void setAttended(boolean attended) { this.attended = attended; }
}
