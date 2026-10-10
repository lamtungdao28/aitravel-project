# TravelHub

Travel discovery, reviews, favorites, itinerary planning and reservations built with Java 21, Spring Boot 3, PostgreSQL and Redis.

## Run With Docker

```bash
docker compose up --build
```

API: http://localhost:8080/api/v1

Swagger: http://localhost:8080/swagger-ui.html

Health: http://localhost:8080/actuator/health

```bash
docker compose up -d --build
docker compose logs -f backend
docker compose down
docker compose down -v
```

The last command removes the local database volume and its data. Compose uses development credentials by default; configure environment variables for other environments. Do not commit `.env`.

## Tests

```bash
mvn test
```

Docker must be running. Tests use real PostgreSQL 17 and Redis 7 containers. API provider responses are mocked; no paid AI calls are made.

See [implementation verification](docs/implementation-log.md) for completed phase checks.
