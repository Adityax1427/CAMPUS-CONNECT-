package edu.northstar.campusconnect.controller;

import static edu.northstar.campusconnect.dto.ApiDtos.*;

import edu.northstar.campusconnect.model.Role;
import edu.northstar.campusconnect.model.User;
import edu.northstar.campusconnect.repository.UserRepository;
import edu.northstar.campusconnect.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        User user = userRepository.save(new User(request.name().trim(), email,
                passwordEncoder.encode(request.password()), Role.STUDENT));
        return new AuthResponse(jwtService.issue(user), view(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Email or password is incorrect."));
        return new AuthResponse(jwtService.issue(user), view(user));
    }

    @GetMapping("/me")
    public UserView me(@org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        return view(user);
    }

    private UserView view(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
