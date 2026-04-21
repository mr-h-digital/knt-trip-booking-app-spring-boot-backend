<div align="center">

<img src="docs/assets/knt-logo.png" alt="K&T Transport" width="260"/>

# K&T Transport — Trip Booking Backend

**Production-ready Spring Boot REST API powering the K&T Transport Android app.**  
JWT-secured · PostgreSQL · Deployed on Railway

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Railway](https://img.shields.io/badge/Deployed%20on-Railway-0B0D0E?style=flat-square&logo=railway&logoColor=white)](https://railway.app)
[![License](https://img.shields.io/badge/License-MIT-1A8FE3?style=flat-square)](LICENSE)

</div>

---

## Overview

K&T Transport is a community transport booking platform serving commuters in Cape Town's Mitchell's Plain area. This backend provides the full REST API consumed by the [Android app](https://github.com/your-org/knt-trip-booking-app-android-frontend), handling everything from user authentication to trip bookings, lift clubs, real-time quotes, and push notifications.

---

## Features

| Domain | Capabilities |
|--------|-------------|
| **Authentication** | JWT-based register & login, BCrypt password hashing, role-aware tokens (COMMUTER / DRIVER / ADMIN) |
| **User Profiles** | Get & update profile, avatar upload (multipart) with static file serving |
| **Trip Bookings** | Create trips, paginated history, per-trip detail with status lifecycle |
| **Lift Clubs** | Browse, create, and subscribe to recurring shared commutes; automatic quota tracking |
| **Quotes** | Driver-issued quotes tied to trips or lift clubs; commuter accept/decline flow |
| **Notifications** | Paginated in-app notifications, mark-as-read (single & bulk) |
| **Security** | Stateless JWT filter chain, CORS configured for Android emulator + production domain |
| **Observability** | `/actuator/health` for Railway health checks |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security + JJWT 0.12.6 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 (Railway managed) |
| Build | Maven 3.x |
| Deployment | Railway (Nixpacks) |

---

## API Reference

All protected endpoints require `Authorization: Bearer <token>`.

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/register` | Public | Register a new commuter account |
| `POST` | `/api/auth/login` | Public | Authenticate and receive JWT |

**Login request**
```json
{ "email": "user@example.com", "password": "secret123" }
```
**Auth response**
```json
{
  "token": "eyJ...",
  "role": "COMMUTER",
  "user": { "id": "uuid", "name": "Tayla Hendricks", "email": "...", "phone": "...", "role": "COMMUTER", "avatarUrl": null }
}
```

---

### Users

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/users/me` | Bearer | Get current user profile |
| `PUT` | `/api/users/me` | Bearer | Update name, email, phone |
| `POST` | `/api/users/me/avatar` | Bearer | Upload profile avatar (multipart `avatar` field) |

---

### Trips

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/trips` | Bearer | List my trips (paginated: `?page=0&size=20`) |
| `GET` | `/api/trips/{id}` | Bearer | Get single trip detail |
| `POST` | `/api/trips` | Bearer | Create a new trip booking |

**Trip statuses:** `PENDING_QUOTE` → `QUOTE_SENT` → `QUOTE_ACCEPTED` → `CONFIRMED` → `IN_PROGRESS` → `COMPLETED` / `CANCELLED`

**Create trip request**
```json
{
  "pickupAddress": "14 Sunrise Ave, Beacon Valley",
  "dropAddress": "Cape Town CBD, Adderley St",
  "date": "2025-05-12",
  "time": "07:30",
  "passengers": 2,
  "notes": "Please be on time"
}
```

---

### Lift Clubs

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/lift-clubs` | Public | Browse all lift clubs (paginated) |
| `GET` | `/api/lift-clubs/{id}` | Public | Get lift club detail |
| `POST` | `/api/lift-clubs` | Bearer | Create a new lift club |
| `POST` | `/api/lift-clubs/{id}/subscribe` | Bearer | Subscribe to a lift club |

**Lift club statuses:** `OPEN` → `QUOTA_MET` → `QUOTE_SENT` → `ACTIVE` → `COMPLETED` / `CANCELLED`

---

### Quotes

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/quotes/{id}` | Bearer | Get a quote |
| `POST` | `/api/quotes/{id}/respond` | Bearer | Accept or decline a quote |

**Respond to quote**
```json
{ "accepted": true, "paymentCycle": "MONTHLY" }
```

---

### Notifications

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/notifications` | Bearer | Get notifications (paginated: `?page=0&size=30`) |
| `POST` | `/api/notifications/{id}/read` | Bearer | Mark single notification as read |
| `POST` | `/api/notifications/read-all` | Bearer | Mark all notifications as read |

---

### Paginated Response Shape

All list endpoints return:
```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "last": false
}
```

---

## Project Structure

```
src/
└── main/
    ├── java/com/kntransport/backend/
    │   ├── BackendApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java      # JWT filter chain, CORS
    │   │   └── WebConfig.java           # Static file serving for avatars
    │   ├── controller/                  # REST controllers (one per domain)
    │   ├── dto/                         # Request/response records
    │   ├── entity/                      # JPA entities
    │   ├── exception/                   # GlobalExceptionHandler + typed exceptions
    │   ├── repository/                  # Spring Data JPA interfaces
    │   ├── security/                    # JwtService, JwtAuthFilter, UserDetailsServiceImpl
    │   └── service/                     # Business logic layer
    └── resources/
        └── application.properties
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 14+ (or use Railway for managed hosting)

### Local Development

**1. Clone the repository**
```bash
git clone https://github.com/your-org/knt-trip-booking-app-spring-boot-backend.git
cd knt-trip-booking-app-spring-boot-backend
```

**2. Configure environment**

Create a `.env` file or export the following:
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/knt_db
export JWT_SECRET=your-super-secret-jwt-key-at-least-32-chars
```

Or edit `src/main/resources/application.properties` directly for local dev.

**3. Build and run**
```bash
mvn clean package -DskipTests
java -jar target/backend-1.0.0.jar
```

Or with the Maven wrapper:
```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

### Running Tests
```bash
mvn test
```

Tests use an in-memory H2 database — no PostgreSQL required for the test suite.

---

## Railway Deployment

This project is pre-configured for one-click Railway deployment.

**1. Push to GitHub, then in Railway:**
- **New Project → Deploy from GitHub repo** → select this repo

**2. Add a PostgreSQL plugin**
Railway automatically injects `DATABASE_URL` into your service.

**3. Set environment variables**

| Variable | Description | Required |
|----------|-------------|----------|
| `PGHOST` | Auto-injected by Railway PostgreSQL plugin | Auto |
| `PGPORT` | Auto-injected by Railway PostgreSQL plugin | Auto |
| `PGDATABASE` | Auto-injected by Railway PostgreSQL plugin | Auto |
| `PGUSER` | Auto-injected by Railway PostgreSQL plugin | Auto |
| `PGPASSWORD` | Auto-injected by Railway PostgreSQL plugin | Auto |
| `JWT_SECRET` | Random string, 32+ characters | **Yes** |
| `JWT_EXPIRATION_MS` | Token lifetime in ms (default: `86400000` = 24h) | Optional |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | Optional |
| `PORT` | Auto-injected by Railway | Auto |

**4. Deploy**
Railway uses `railway.toml` to build (`mvn clean package -DskipTests`) and start the jar. Health checks hit `/actuator/health`.

**Production base URL (Android app):** `https://api.ktransport.co.za/`

---

## Android App Integration

The base URL is configured in the Android app's `build.gradle.kts`:

```kotlin
// Debug (local emulator)
buildConfigField("String", "ACTIVE_API_URL", "\"http://10.0.2.2:8080/\"")

// Release (Railway deployment)
buildConfigField("String", "ACTIVE_API_URL", "\"https://api.ktransport.co.za/\"")
```

The app uses Retrofit with a JWT `AuthInterceptor` — no changes needed once the Railway URL is live.

---

## Database Schema

Hibernate auto-generates the schema on startup (`spring.jpa.hibernate.ddl-auto=update`).

| Table | Description |
|-------|-------------|
| `users` | Commuter, driver, and admin accounts |
| `trip_bookings` | Individual trip booking records |
| `lift_clubs` | Recurring shared commute groups |
| `lift_club_days` | Days-of-week per lift club (element collection) |
| `lift_club_subscriptions` | Commuter ↔ lift club membership |
| `quotes` | Driver quotes for trips or lift clubs |
| `notifications` | In-app notification records per user |

---

## Roadmap

- [ ] FCM push notification delivery (`POST /api/devices/token`)
- [ ] Driver mobile portal (driver-side trip management endpoints)
- [ ] Admin dashboard API (CRUD management, reporting)
- [ ] Payment integration (PayFast / Ozow)
- [ ] S3/Cloudflare R2 avatar storage
- [ ] Rate-limiting and request throttling
- [ ] WebSocket real-time trip status updates

---

<br/>

<div align="center">

<br/>

*Designed and developed by*

<br/>

<img src="docs/assets/mrh-digital-logo.png" alt="Mr H Digital" width="200"/>

<br/>

**Mr H Digital**

*Digital Solutions for Growing Businesses*

<br/>

[![Website](https://img.shields.io/badge/Website-mrhdigital.co.za-84CC16?style=flat-square)](https://mrhdigital.co.za)
[![Email](https://img.shields.io/badge/Email-info%40mrhdigital.co.za-84CC16?style=flat-square)](mailto:info@mrhdigital.co.za)

<br/>

*© 2026 Mr H Digital. All rights reserved.*

</div>
