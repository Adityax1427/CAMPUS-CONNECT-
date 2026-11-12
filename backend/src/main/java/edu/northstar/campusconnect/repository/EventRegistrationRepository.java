package edu.northstar.campusconnect.repository;

import edu.northstar.campusconnect.model.EventRegistration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    long countByEventId(Long eventId);
    List<EventRegistration> findByUserIdOrderByRegisteredAtDesc(Long userId);
    List<EventRegistration> findByEventId(Long eventId);
    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);
    long countByAttendedTrue();
    void deleteByEventId(Long eventId);
}
