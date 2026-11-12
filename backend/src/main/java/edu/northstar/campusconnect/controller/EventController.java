package edu.northstar.campusconnect.controller;

import static edu.northstar.campusconnect.dto.ApiDtos.*;

import edu.northstar.campusconnect.model.*;
import edu.northstar.campusconnect.repository.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final ClubRepository clubRepository;

    public EventController(EventRepository eventRepository, EventRegistrationRepository registrationRepository,
                           ClubRepository clubRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.clubRepository = clubRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EventView> list() {
        return eventRepository.findAllByOrderByStartsAtAsc().stream().map(this::view).toList();
    }

    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public List<RegistrationView> mine(@AuthenticationPrincipal User user) {
        return registrationRepository.findByUserIdOrderByRegisteredAtDesc(user.getId()).stream()
                .map(item -> new RegistrationView(item.getEvent().getId(), item.getEvent().getTitle(),
                        item.getEvent().getStartsAt(), item.getRegisteredAt(), item.isAttended()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EventView create(@Valid @RequestBody EventRequest request, @AuthenticationPrincipal User user) {
        validateDates(request);
        Club club = request.clubId() == null ? null : findClub(request.clubId());
        CampusEvent event = new CampusEvent(request.title().trim(), request.category(), request.description().trim(),
                request.venue().trim(), request.startsAt(), request.endsAt(), request.capacity(),
                cleanImageUrl(request.imageUrl()), club, user);
        return view(eventRepository.save(event));
    }

    @PutMapping("/{eventId}")
    @Transactional
    public EventView update(@PathVariable Long eventId, @Valid @RequestBody EventRequest request,
                            @AuthenticationPrincipal User user) {
        CampusEvent event = findEvent(eventId);
        requireOwnerOrAdmin(event, user);
        validateDates(request);
        if (request.capacity() < registrationRepository.countByEventId(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Capacity cannot be lower than the number of existing registrations.");
        }
        Club club = request.clubId() == null ? null : findClub(request.clubId());
        event.update(request.title().trim(), request.category(), request.description().trim(),
                request.venue().trim(), request.startsAt(), request.endsAt(), request.capacity(),
                cleanImageUrl(request.imageUrl()), club);
        return view(eventRepository.save(event));
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long eventId, @AuthenticationPrincipal User user) {
        CampusEvent event = findEvent(eventId);
        requireOwnerOrAdmin(event, user);
        registrationRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    @PostMapping("/{eventId}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RegistrationView register(@PathVariable Long eventId, @AuthenticationPrincipal User user) {
        CampusEvent event = findEvent(eventId);
        if (!event.getStartsAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is closed for this event.");
        }
        if (registrationRepository.existsByEventIdAndUserId(eventId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You’re already registered for this event.");
        }
        if (registrationRepository.countByEventId(eventId) >= event.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This event is full.");
        }
        EventRegistration registration = registrationRepository.save(new EventRegistration(event, user));
        return new RegistrationView(event.getId(), event.getTitle(), event.getStartsAt(),
                registration.getRegisteredAt(), false);
    }

    @DeleteMapping("/{eventId}/registrations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void unregister(@PathVariable Long eventId, @AuthenticationPrincipal User user) {
        EventRegistration registration = registrationRepository.findByEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not registered for this event."));
        registrationRepository.delete(registration);
    }

    @GetMapping("/{eventId}/attendees")
    @Transactional(readOnly = true)
    public List<AttendeeView> attendees(@PathVariable Long eventId, @AuthenticationPrincipal User user) {
        CampusEvent event = findEvent(eventId);
        requireOwnerOrAdmin(event, user);
        return registrationRepository.findByEventId(eventId).stream()
                .map(item -> new AttendeeView(item.getUser().getId(), item.getUser().getName(),
                        item.getUser().getEmail(), item.isAttended(), item.getRegisteredAt()))
                .toList();
    }

    @PostMapping("/{eventId}/attendance/{userId}")
    @Transactional
    public AttendeeView markAttendance(@PathVariable Long eventId, @PathVariable Long userId,
                                       @AuthenticationPrincipal User user) {
        CampusEvent event = findEvent(eventId);
        requireOwnerOrAdmin(event, user);
        EventRegistration registration = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The student is not registered for this event."));
        registration.setAttended(true);
        registrationRepository.save(registration);
        return new AttendeeView(userId, registration.getUser().getName(), registration.getUser().getEmail(),
                true, registration.getRegisteredAt());
    }

    private EventView view(CampusEvent event) {
        return new EventView(event.getId(), event.getTitle(), event.getCategory(), event.getDescription(),
                event.getVenue(), event.getStartsAt(), event.getEndsAt(), event.getCapacity(),
                registrationRepository.countByEventId(event.getId()), event.getImageUrl(),
                event.getClub() == null ? null : event.getClub().getId(),
                event.getClub() == null ? null : event.getClub().getName(), event.getOrganizer().getId());
    }

    private CampusEvent findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));
    }

    private Club findClub(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found."));
    }

    private void validateDates(EventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The end time must be after the start time.");
        }
        if (request.startsAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Events must be scheduled in the future.");
        }
    }

    private String cleanImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        if (!imageUrl.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event images must use a secure HTTPS URL.");
        }
        return imageUrl;
    }

    private void requireOwnerOrAdmin(CampusEvent event, User user) {
        if (user.getRole() != Role.ADMIN && !event.getOrganizer().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the event organizer or an admin can manage this event.");
        }
    }
}
