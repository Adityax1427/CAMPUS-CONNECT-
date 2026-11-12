package edu.northstar.campusconnect.repository;

import edu.northstar.campusconnect.model.CampusEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<CampusEvent, Long> {
    List<CampusEvent> findAllByOrderByStartsAtAsc();
    List<CampusEvent> findByOrganizerIdOrderByStartsAtAsc(Long organizerId);
}
