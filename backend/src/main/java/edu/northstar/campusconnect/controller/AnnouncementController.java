package edu.northstar.campusconnect.controller;

import static edu.northstar.campusconnect.dto.ApiDtos.*;

import edu.northstar.campusconnect.model.Announcement;
import edu.northstar.campusconnect.model.Club;
import edu.northstar.campusconnect.model.User;
import edu.northstar.campusconnect.repository.AnnouncementRepository;
import edu.northstar.campusconnect.repository.ClubRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementRepository announcementRepository;
    private final ClubRepository clubRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository, ClubRepository clubRepository) {
        this.announcementRepository = announcementRepository;
        this.clubRepository = clubRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AnnouncementView> list() {
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AnnouncementView create(@Valid @RequestBody AnnouncementRequest request,
                                   @AuthenticationPrincipal User user) {
        Club club = request.clubId() == null ? null : clubRepository.findById(request.clubId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found."));
        return view(announcementRepository.save(new Announcement(request.title().trim(),
                request.body().trim(), club, user)));
    }

    private AnnouncementView view(Announcement announcement) {
        return new AnnouncementView(announcement.getId(), announcement.getTitle(), announcement.getBody(),
                announcement.getCreatedAt(), announcement.getClub() == null ? null : announcement.getClub().getName(),
                announcement.getAuthor().getName());
    }
}
