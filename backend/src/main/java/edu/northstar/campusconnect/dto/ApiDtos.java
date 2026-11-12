package edu.northstar.campusconnect.dto;

import edu.northstar.campusconnect.model.Role;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class ApiDtos {
    private ApiDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserView(Long id, String name, String email, Role role) {}
    public record AuthResponse(String token, UserView user) {}

    public record EventRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Pattern(regexp = "Workshop|Social|Competition|Talk") String category,
            @NotBlank @Size(max = 1200) String description,
            @NotBlank @Size(max = 180) String venue,
            @NotNull LocalDateTime startsAt,
            @NotNull LocalDateTime endsAt,
            @NotNull @Min(1) @Max(2000) Integer capacity,
            @Size(max = 500) String imageUrl,
            Long clubId) {}

    public record EventView(
            Long id, String title, String category, String description, String venue,
            LocalDateTime startsAt, LocalDateTime endsAt, Integer capacity, long registeredCount,
            String imageUrl, Long clubId, String clubName, Long organizerId) {}

    public record RegistrationView(Long eventId, String title, LocalDateTime startsAt,
                                   LocalDateTime registeredAt, boolean attended) {}

    public record AttendeeView(Long id, String name, String email, boolean attended,
                               LocalDateTime registeredAt) {}

    public record ClubRequest(@NotBlank @Size(max = 120) String name,
                              @NotBlank @Size(max = 8) String shortName,
                              @NotBlank @Size(max = 500) String description) {}

    public record ClubView(Long id, String name, String shortName, String description,
                           long memberCount, boolean joined) {}

    public record AnnouncementRequest(@NotBlank @Size(max = 160) String title,
                                      @NotBlank @Size(max = 1200) String body, Long clubId) {}

    public record AnnouncementView(Long id, String title, String body,
                                   LocalDateTime createdAt, String clubName, String authorName) {}

    public record AiChatRequest(@NotBlank @Size(max = 1000) String prompt) {}
    public record AiChatResponse(String answer, java.util.List<String> suggestions) {}

    public record AdminSummary(long users, long events, long clubs, long registrations, long attended) {}
}
