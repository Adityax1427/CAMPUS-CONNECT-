package edu.northstar.campusconnect.repository;

import edu.northstar.campusconnect.model.ClubMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubMembershipRepository extends JpaRepository<ClubMembership, Long> {
    long countByClubId(Long clubId);
    boolean existsByClubIdAndUserId(Long clubId, Long userId);
}
