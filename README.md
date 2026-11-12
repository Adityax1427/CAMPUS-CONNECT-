# CampusConnect

A campus event and student community platform built with React, Spring Boot, Spring Security, and MySQL. Students can discover and register for events, join clubs, and read campus announcements. Organizers can publish and manage events, review rosters, and mark attendance. Admin-only endpoints provide system totals and role management.

## Run locally

### Prerequisites

- Node.js 20.19+ or 22.12+
- Java 21
- Maven 3.9+
- Maven 3.9+
- Docker Desktop only if you choose the MySQL setup

### 1. Start the API

**Quick local start (no Docker/MySQL required):** from `backend/`, run:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

The `local` profile uses a file-backed H2 database and starts the API on port `8080`. The first run seeds sample events, clubs, demo accounts, and announcements. Keep this backend terminal running while you use the app.

The app also includes a built-in Campus AI assistant in the frontend. It works out of the box using local recommendations from the campus data. To enable OpenAI responses instead, set `OPENAI_API_KEY` in the backend environment before starting the app.

**MySQL with Docker:** from the project root, run `docker compose up --build`. This starts MySQL on port `3307` and the API on port `8080`.

### 2. Start the React app

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

The Vite development server proxies `/api` requests to `http://localhost:8080`. Set `VITE_API_URL` if the API is hosted elsewhere.

Run the backend integration tests with `mvn test` from `backend/`. They use an isolated H2 database and do not require Docker or MySQL.

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Student | `student@northstar.edu` | `Campus123!` |
| Organizer | `organizer@northstar.edu` | `Campus123!` |
| Admin | `admin@northstar.edu` | `Campus123!` |

These are local-development credentials only. Change or remove the seed accounts before deploying.

## API overview

- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `GET /api/events`, `GET /api/events/mine`, event create/update/delete
- Register/unregister at `/api/events/{id}/registrations`
- Organizer roster and attendance routes at `/api/events/{id}/attendees` and `/api/events/{id}/attendance/{userId}`
- `GET /api/clubs`, `POST /api/clubs/{id}/join`
- `GET /api/announcements`, organizer announcement creation
- Admin-only `/api/admin/summary`, `/api/admin/users`, and role updates

New registrations are always assigned the `STUDENT` role. Organizers and admins are seeded only for development; use an authenticated admin workflow or a controlled database migration to grant elevated roles in production.

## Production setup

Set `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, a unique random `JWT_SECRET` of at least 32 bytes, and `CORS_ALLOWED_ORIGINS` for your deployed frontend. Replace `spring.jpa.hibernate.ddl-auto=update` with a managed migration strategy before a production deployment.
