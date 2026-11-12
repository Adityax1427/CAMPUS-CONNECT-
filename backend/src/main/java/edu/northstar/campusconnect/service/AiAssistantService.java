package edu.northstar.campusconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.northstar.campusconnect.dto.ApiDtos.AiChatResponse;
import edu.northstar.campusconnect.model.Announcement;
import edu.northstar.campusconnect.model.Club;
import edu.northstar.campusconnect.model.CampusEvent;
import edu.northstar.campusconnect.model.User;
import edu.northstar.campusconnect.repository.AnnouncementRepository;
import edu.northstar.campusconnect.repository.ClubRepository;
import edu.northstar.campusconnect.repository.EventRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiAssistantService {
    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final AnnouncementRepository announcementRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String openAiApiKey;
    private final String openAiModel;

    public AiAssistantService(EventRepository eventRepository,
                             ClubRepository clubRepository,
                             AnnouncementRepository announcementRepository,
                             @Value("${app.ai.openai.api-key:}") String openAiApiKey,
                             @Value("${app.ai.openai.model:gpt-4o-mini}") String openAiModel) {
        this.eventRepository = eventRepository;
        this.clubRepository = clubRepository;
        this.announcementRepository = announcementRepository;
        this.openAiApiKey = openAiApiKey == null ? "" : openAiApiKey.trim();
        this.openAiModel = openAiModel == null || openAiModel.isBlank() ? "gpt-4o-mini" : openAiModel.trim();
    }

    public AiChatResponse respond(User user, String prompt) {
        String trimmedPrompt = prompt == null ? "" : prompt.trim();
        if (trimmedPrompt.isBlank()) {
            return new AiChatResponse(
                    "Ask me about campus events, clubs, or what to do next this week and I’ll help you plan it.",
                    List.of("Best events this week", "Club ideas for me", "What’s happening on campus")
            );
        }

        String openAiAnswer = callOpenAI(user, trimmedPrompt);
        if (openAiAnswer != null && !openAiAnswer.isBlank()) {
            return new AiChatResponse(openAiAnswer, suggestedPrompts());
        }

        return new AiChatResponse(buildFallbackResponse(user, trimmedPrompt), suggestedPrompts());
    }

    private String callOpenAI(User user, String prompt) {
        if (openAiApiKey.isBlank()) {
            return null;
        }

        try {
            String context = buildCampusContext();
            String requestBody = """
                    {
                      "model": "%s",
                      "temperature": 0.7,
                      "messages": [
                        {"role": "system", "content": "You are a helpful campus assistant for Northstar University. Follow the provided context exactly and keep answers concise, warm, and specific."},
                        {"role": "user", "content": "User: %s\nContext:\n%s\nQuestion: %s"}
                      ]
                    }
                    """.formatted(openAiModel, escapeJson(user == null ? "student" : user.getName()), escapeJson(context), escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isTextual() ? content.asText().trim() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildFallbackResponse(User user, String prompt) {
        String lowerPrompt = prompt.toLowerCase(Locale.ROOT);
        List<CampusEvent> upcoming = eventRepository.findAllByOrderByStartsAtAsc().stream()
                .filter(event -> !event.getStartsAt().isBefore(LocalDateTime.now()))
                .limit(3)
                .toList();
        List<Club> clubs = clubRepository.findAll().stream().limit(3).toList();
        List<Announcement> announcements = announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(2)
                .toList();

        if (lowerPrompt.contains("club") || lowerPrompt.contains("community") || lowerPrompt.contains("people")) {
            StringBuilder builder = new StringBuilder();
            builder.append("A few great ways to get plugged in: ");
            for (Club club : clubs) {
                builder.append(club.getName()).append(" (").append(club.getDescription()).append("), ");
            }
            if (!clubs.isEmpty()) {
                builder.setLength(Math.max(builder.length() - 2, 0));
            }
            builder.append(". Pick the one that best matches your interests and join the community.");
            return builder.toString();
        }

        if (lowerPrompt.contains("announcement") || lowerPrompt.contains("news") || lowerPrompt.contains("notice")) {
            if (announcements.isEmpty()) {
                return "There isn’t much campus news right now, but there are plenty of upcoming events worth checking out.";
            }
            StringBuilder builder = new StringBuilder("Here’s what’s going on: ");
            for (Announcement announcement : announcements) {
                builder.append("\n• ").append(announcement.getTitle()).append(" — ").append(announcement.getBody());
            }
            return builder.toString();
        }

        if (upcoming.isEmpty()) {
            return "There aren’t any current events on the calendar, but you could explore clubs and announcements to find your next community connection.";
        }

        StringBuilder builder = new StringBuilder("Here are the best options right now: ");
        for (CampusEvent event : upcoming) {
            builder.append("\n• ").append(event.getTitle()).append(" (").append(event.getCategory()).append(") on ")
                    .append(event.getStartsAt().toLocalDate()).append(" at ").append(event.getVenue()).append(".");
        }
        builder.append("\nIf you want a more tailored recommendation, tell me whether you’re in the mood for a workshop, a social event, or a competition.");
        return builder.toString();
    }

    private List<String> suggestedPrompts() {
        List<String> prompts = new ArrayList<>();
        prompts.add("Best events this week");
        prompts.add("What clubs should I join?");
        prompts.add("What’s happening on campus?");
        return prompts;
    }

    private String buildCampusContext() {
        List<CampusEvent> events = eventRepository.findAllByOrderByStartsAtAsc().stream()
                .filter(event -> !event.getStartsAt().isBefore(LocalDateTime.now()))
                .sorted(Comparator.comparing(CampusEvent::getStartsAt))
                .limit(5)
                .toList();
        List<Club> clubs = clubRepository.findAll().stream().limit(5).toList();
        List<Announcement> announcements = announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(3)
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("Upcoming events:\n");
        for (CampusEvent event : events) {
            builder.append("- ").append(event.getTitle()).append(" | ").append(event.getCategory())
                    .append(" | ").append(event.getStartsAt()).append(" | ").append(event.getVenue()).append("\n");
        }
        if (clubs.isEmpty()) {
            builder.append("No clubs available.\n");
        } else {
            builder.append("Popular clubs:\n");
            for (Club club : clubs) {
                builder.append("- ").append(club.getName()).append(" | ").append(club.getDescription()).append("\n");
            }
        }
        if (announcements.isEmpty()) {
            builder.append("No recent announcements.\n");
        } else {
            builder.append("Recent announcements:\n");
            for (Announcement announcement : announcements) {
                builder.append("- ").append(announcement.getTitle()).append(" | ").append(announcement.getBody()).append("\n");
            }
        }
        return builder.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value).replace("\n", " ");
        } catch (Exception ignored) {
            return value.replace("\n", " ");
        }
    }
}
