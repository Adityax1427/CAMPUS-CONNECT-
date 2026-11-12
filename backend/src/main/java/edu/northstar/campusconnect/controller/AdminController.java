package edu.northstar.campusconnect.controller;

import static edu.northstar.campusconnect.dto.ApiDtos.*;

import edu.northstar.campusconnect.model.Role;
import edu.northstar.campusconnect.model.User;
import edu.northstar.campusconnect.repository.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final EventRegistrationRepository registrationRepository;

    public AdminController(UserRepository userRepository, EventRepository eventRepository,
                           ClubRepository clubRepository, EventRegistrationRepository registrationRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.clubRepository = clubRepository;
        this.registrationRepository = registrationRepository;
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public AdminSummary summary() {
        return new AdminSummary(userRepository.count(), eventRepository.count(), clubRepository.count(),
                registrationRepository.count(), registrationRepository.countByAttendedTrue());
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<UserView> users() {
        return userRepository.findAll().stream()
                .map(user -> new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole()))
                .toList();
    }

    @PutMapping("/users/{userId}/role")
    @Transactional
    public UserView setRole(@PathVariable Long userId, @RequestBody RoleUpdate request) {
        Role role;
        try {
            role = Role.valueOf(request.role());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be STUDENT, ORGANIZER, or ADMIN.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        user.setRole(role);
        userRepository.save(user);
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public record RoleUpdate(String role) {}
}
