package edu.northstar.campusconnect.model;

import jakarta.persistence.*;

@Entity
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 8)
    private String shortName;

    @Column(nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private User lead;

    protected Club() {}

    public Club(String name, String shortName, String description, User lead) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.lead = lead;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getDescription() { return description; }
    public User getLead() { return lead; }
}
