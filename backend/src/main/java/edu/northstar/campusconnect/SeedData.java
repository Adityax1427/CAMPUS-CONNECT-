package edu.northstar.campusconnect;

import edu.northstar.campusconnect.model.*;
import edu.northstar.campusconnect.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"local", "test"})
public class SeedData {
    @Bean
    CommandLineRunner seedCampus(UserRepository users, ClubRepository clubs, EventRepository events,
                                 ClubMembershipRepository memberships, AnnouncementRepository announcements,
                                 PasswordEncoder passwords) {
        return args -> {
            User organizer = getOrCreateUser(users, passwords, "Jamie Parker",
                    "organizer@northstar.edu", Role.ORGANIZER);
            User student = getOrCreateUser(users, passwords, "Alex Morgan",
                    "student@northstar.edu", Role.STUDENT);
            getOrCreateUser(users, passwords, "Northstar Admin", "admin@northstar.edu", Role.ADMIN);

            Club makers = getOrCreateClub(clubs, "Makers Society", "MS",
                    "For the curious minds who love building, tinkering, and making ideas real.", organizer);
            Club creative = getOrCreateClub(clubs, "Creative Collective", "CC",
                    "A home for artists, designers, photographers, and creative beginners.", organizer);
            Club outdoors = getOrCreateClub(clubs, "Outdoors Club", "OC",
                    "Trade your screen for a trail. All paces, all experience levels welcome.", organizer);
            Club founders = getOrCreateClub(clubs, "Founders Circle", "FC",
                    "Big ideas start with a conversation. Meet the next generation of builders.", organizer);

            for (Club club : new Club[]{makers, creative, outdoors, founders}) {
                if (!memberships.existsByClubIdAndUserId(club.getId(), student.getId())) {
                    memberships.save(new ClubMembership(club, student));
                }
            }

            LocalDate today = LocalDate.now();
            if (events.count() == 0) {
                events.save(new CampusEvent("Autumn Makers Night", "Social",
                        "A night for big ideas and even better company. Meet local makers, make something of your own, and leave with a few new friends.",
                        "The Commons · Studio A", today.plusDays(2).atTime(17, 30), today.plusDays(2).atTime(20, 0),
                        120, "https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1200&q=85",
                        makers, organizer));
                events.save(new CampusEvent("Build Your First AI App", "Workshop",
                        "A hands-on introduction to turning your idea into a working app. Bring a laptop; curiosity is all the experience you need.",
                        "Innovation Lab · Room 204", today.plusDays(4).atTime(16, 0), today.plusDays(4).atTime(18, 30),
                        45, "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=960&q=85",
                        makers, organizer));
                events.save(new CampusEvent("Open Mic: The Autumn Edit", "Talk",
                        "Poems, stories, songs, unfinished thoughts. A welcoming night on the mic, hosted by students and made for everyone.",
                        "The Lantern Café", today.plusDays(6).atTime(19, 0), today.plusDays(6).atTime(21, 0),
                        80, "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?auto=format&fit=crop&w=960&q=85",
                        creative, organizer));
                events.save(new CampusEvent("The Great Campus Cook-Off", "Competition",
                        "Teams of three, mystery ingredients, and one very friendly judges’ table. Sign up to cook or come hungry.",
                        "Student Union · Courtyard", today.plusDays(9).atTime(12, 0), today.plusDays(9).atTime(15, 0),
                        60, "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=960&q=85",
                        creative, organizer));
                events.save(new CampusEvent("Sunrise Trail & Coffee", "Social",
                        "An easy morning loop at Pine Ridge followed by coffee on us. Meet at the south gate — we’ll take it at everyone’s pace.",
                        "South Gate · Campus Loop", today.plusDays(12).atTime(7, 30), today.plusDays(12).atTime(10, 0),
                        35, "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=960&q=85",
                        outdoors, organizer));
            }

            if (announcements.count() == 0) {
                announcements.save(new Announcement("Fall Fest is almost here",
                        "Save a spot in your calendar for campus Fall Fest later this month. Club stalls, live music, food trucks, and a few surprises are on the way.",
                        null, organizer));
                announcements.save(new Announcement("Club fair sign-ups are open",
                        "Want to share what your club is all about? Reserve a table this week. New clubs are welcome too.",
                        null, organizer));
                announcements.save(new Announcement("Extended library hours this week",
                        "The north reading room will stay open until midnight through Friday. Bring your student ID for entry after 9 pm.",
                        null, organizer));
            }
        };
    }

    private User getOrCreateUser(UserRepository users, PasswordEncoder passwords, String name,
                                 String email, Role role) {
        return users.findByEmailIgnoreCase(email)
                .orElseGet(() -> users.save(new User(name, email, passwords.encode("Campus123!"), role)));
    }

    private Club getOrCreateClub(ClubRepository clubs, String name, String shortName,
                                 String description, User organizer) {
        return clubs.findByNameIgnoreCase(name)
                .orElseGet(() -> clubs.save(new Club(name, shortName, description, organizer)));
    }
}
