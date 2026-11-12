package edu.northstar.campusconnect.controller;

import static edu.northstar.campusconnect.dto.ApiDtos.*;

import edu.northstar.campusconnect.model.Club;
import edu.northstar.campusconnect.model.ClubMembership;
import edu.northstar.campusconnect.model.User;
import edu.northstar.campusconnect.repository.ClubMembershipRepository;
import edu.northstar.campusconnect.repository.ClubRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {
    private final ClubRepository clubRepository;
    private final ClubMembershipRepository membershipRepository;

    public ClubController(ClubRepository clubRepository, ClubMembershipRepository membershipRepository) {
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ClubView> list(@AuthenticationPrincipal User user) {
        return clubRepository.findAll().stream().map(club -> view(club, user)).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ClubView create(@Valid @RequestBody ClubRequest request, @AuthenticationPrincipal User user) {
        if (clubRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A club with this name already exists.");
        }
        Club club = clubRepository.save(new Club(request.name().trim(), request.shortName().trim().toUpperCase(),
                request.description().trim(), user));
        membershipRepository.save(new ClubMembership(club, user));
        return view(club, user);
    }

    @PostMapping("/{clubId}/join")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ClubView join(@PathVariable Long clubId, @AuthenticationPrincipal User user) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found."));
        if (membershipRepository.existsByClubIdAndUserId(clubId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You’re already a member of this club.");
        }
        membershipRepository.save(new ClubMembership(club, user));
        return view(club, user);
    }

    private ClubView view(Club club, User user) {
        boolean joined = user != null && membershipRepository.existsByClubIdAndUserId(club.getId(), user.getId());
        return new ClubView(club.getId(), club.getName(), club.getShortName(), club.getDescription(),
                membershipRepository.countByClubId(club.getId()), joined);
    }
}
