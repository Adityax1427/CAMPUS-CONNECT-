package edu.northstar.campusconnect;

import static org.assertj.core.api.Assertions.assertThat;

import edu.northstar.campusconnect.dto.ApiDtos.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CampusConnectApiTest {
    @Autowired
    private TestRestTemplate client;

    @Test
    void studentCanRegisterAndOrganizerCanManageAttendance() {
        AuthResponse student = login("student@northstar.edu");
        AuthResponse organizer = login("organizer@northstar.edu");

        EventView[] events = client.getForObject("/api/events", EventView[].class);
        assertThat(events).isNotEmpty();
        EventView event = events[0];

        var registrationResponse = client.exchange("/api/events/" + event.id() + "/registrations",
                HttpMethod.POST, authenticated(student.token(), null),
                new ParameterizedTypeReference<RegistrationView>() {});
        assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var mine = client.exchange("/api/events/mine", HttpMethod.GET, authenticated(student.token(), null),
                new ParameterizedTypeReference<java.util.List<RegistrationView>>() {});
        assertThat(mine.getBody()).extracting(RegistrationView::eventId).contains(event.id());

        var studentRosterResponse = client.exchange("/api/events/" + event.id() + "/attendees",
                HttpMethod.GET, authenticated(student.token(), null),
                new ParameterizedTypeReference<String>() {});
        assertThat(studentRosterResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var rosterResponse = client.exchange("/api/events/" + event.id() + "/attendees",
                HttpMethod.GET, authenticated(organizer.token(), null),
                new ParameterizedTypeReference<java.util.List<AttendeeView>>() {});
        assertThat(rosterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rosterResponse.getBody()).hasSize(1);

        var attendanceResponse = client.exchange(
                "/api/events/" + event.id() + "/attendance/" + student.user().id(),
                HttpMethod.POST, authenticated(organizer.token(), null),
                new ParameterizedTypeReference<AttendeeView>() {});
        assertThat(attendanceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(attendanceResponse.getBody().attended()).isTrue();
    }

    @Test
    void registrationRejectsAnonymousRequestsAndSignUpCannotChoosePrivilegedRole() {
        EventView[] events = client.getForObject("/api/events", EventView[].class);
        var registrationResponse = client.exchange("/api/events/" + events[0].id() + "/registrations",
                HttpMethod.POST, HttpEntity.EMPTY,
                new ParameterizedTypeReference<RegistrationView>() {});
        assertThat(registrationResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var signUp = new RegisterRequest("New Student", "new-student@northstar.edu", "Passphrase123!");
        AuthResponse account = client.postForObject("/api/auth/register", signUp, AuthResponse.class);
        assertThat(account).isNotNull();
        assertThat(account.user().role().name()).isEqualTo("STUDENT");
    }

    @Test
    void organizerCanCreateEditAndDeleteEventsWithoutLeavingRegistrationsBehind() {
        AuthResponse organizer = login("organizer@northstar.edu");
        AuthResponse student = login("student@northstar.edu");
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        EventRequest create = new EventRequest("API test event", "Workshop", "A test event",
                "Test Hall", start, start.plusHours(1), 10, null, null);

        var created = client.exchange("/api/events", HttpMethod.POST,
                authenticated(organizer.token(), create), EventView.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long eventId = created.getBody().id();

        var signup = client.exchange("/api/events/" + eventId + "/registrations",
                HttpMethod.POST, authenticated(student.token(), null),
                new ParameterizedTypeReference<RegistrationView>() {});
        assertThat(signup.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        EventRequest update = new EventRequest("Updated API event", "Workshop", "Updated details",
                "New Hall", start, start.plusHours(2), 10, null, null);
        var updated = client.exchange("/api/events/" + eventId, HttpMethod.PUT,
                authenticated(organizer.token(), update), EventView.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().title()).isEqualTo("Updated API event");

        var deleted = client.exchange("/api/events/" + eventId, HttpMethod.DELETE,
                authenticated(organizer.token(), null), String.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var registrations = client.exchange("/api/events/mine", HttpMethod.GET,
                authenticated(student.token(), null),
                new ParameterizedTypeReference<java.util.List<RegistrationView>>() {});
        assertThat(registrations.getBody()).extracting(RegistrationView::eventId).doesNotContain(eventId);
    }

    private AuthResponse login(String email) {
        return client.postForObject("/api/auth/login",
                new LoginRequest(email, "Campus123!"), AuthResponse.class);
    }

    private <T> HttpEntity<T> authenticated(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
