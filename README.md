# Digital Library

A Spring Boot REST API service that searches authors and their works using a local MySQL database with fallback to the [OpenLibrary API](https://openlibrary.org). Results fetched from OpenLibrary are cached locally for faster subsequent lookups.

## Tech Stack

- **Java 17** / **Spring Boot 3.3.5**
- **Spring Data JPA** (Hibernate)
- **MySQL 8**
- **Liquibase** – XML-based database migrations
- **Resilience4j** – Circuit breaker & retry for OpenLibrary calls
- **Lombok**
- **Maven**
- **Docker** & **Docker Compose**

---

## Getting Started

`application.yaml` is pre-configured to run with Docker Compose out of the box (connecting to the `mysql` service on port `3306`), and supports a `local` profile (port `3307`) for rapid local development and testing.

### Option 1: Run Everything with Docker Compose (Recommended)

The easiest way to run the entire project — no JDK, Maven, or MySQL installation required.

**Prerequisites:** [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) installed.

```bash
# Clone the repository
git clone https://github.com/developerKhusanjon/digital-library.git
cd digital-library

# Build and start all services
docker compose up --build
```

This will:
- Start a **MySQL 8** container (`digital-library-db`, port `3307` mapped to host)
- Build the Spring Boot container (`digital-library-app`, port `8080` mapped to host)
- Run Liquibase migrations automatically on startup

The API will be available at **http://localhost:8080**.

**Useful commands:**

```bash
# Run in detached mode (background)
docker compose up --build -d

# View application logs
docker compose logs -f app

# Stop all services
docker compose down

# Stop and remove database volume (full reset)
docker compose down -v
```

---

### Option 2: Run DB in Docker & App Locally (Fastest for Dev & Testing)

If you are developing or testing code changes quickly:

1. **Start only the MySQL database container:**
```bash
docker compose up -d mysql
```
*(MySQL is available on host port `3307`)*

2. **Run the Spring Boot application using the `local` profile:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
*(The `local` profile in `application.yaml` automatically connects to `localhost:3307`)*

---

### Option 3: Run Locally (Standalone MySQL)

If you have a local standalone MySQL server running on port `3306`:

1. Create the database:
```sql
CREATE DATABASE digital_library;
```

2. Run the application:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3306/digital_library?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```
Or set environment variables:
```bash
export DB_HOST=localhost
export DB_PORT=3306
mvn spring-boot:run
```

---

## Configuration (`application.yaml`)

| Environment Variable | Default Value | Description |
|----------------------|---------------|-------------|
| `PORT` | `8080` | HTTP server port |
| `DB_HOST` | `mysql` (`localhost` with `--spring.profiles.active=local`) | Database host |
| `DB_PORT` | `3306` (`3307` with `--spring.profiles.active=local`) | Database port |
| `DB_NAME` | `digital_library` | Database name |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Database password |
| `SPRING_DATASOURCE_URL` | Computed from host/port/name | Full JDBC URL override |
| `OPENLIBRARY_BASE_URL` | `https://openlibrary.org` | OpenLibrary API URL |

---

## API Endpoints

### 1. Search Authors

Search authors by name. Checks the local database first, then falls back to the OpenLibrary API.

```
GET /api/v1/authors?name={name}
```

**Query Parameters:**

| Parameter | Type   | Required | Description            |
|-----------|--------|----------|------------------------|
| `name`    | String | Yes      | Author name to search  |

**Example Request:**

```bash
curl "http://localhost:8080/api/v1/authors?name=Tolkien"
```

**Example Response** (`200 OK`):

```json
[
  {
    "id": "OL26320A",
    "name": "J.R.R. Tolkien"
  },
  {
    "id": "OL12627750A",
    "name": "Simon Tolkien"
  }
]
```

**Error Responses:**

| Status | Condition                        |
|--------|----------------------------------|
| `400`  | `name` parameter is blank/missing |

---

### 2. Get Author Works

Get all works by an author using their OpenLibrary author key. Checks the local database first, then falls back to the OpenLibrary API.

```
GET /api/v1/authors/{authorId}/works
```

**Path Parameters:**

| Parameter  | Type   | Required | Description                               |
|------------|--------|----------|-------------------------------------------|
| `authorId` | String | Yes      | OpenLibrary author key (e.g. `OL26320A`)  |

**Example Request:**

```bash
curl "http://localhost:8080/api/v1/authors/OL26320A/works"
```

**Example Response** (`200 OK`):

```json
[
  {
    "key": "/works/OL27516W",
    "title": "The Hobbit"
  },
  {
    "key": "/works/OL27479W",
    "title": "The Lord of the Rings"
  }
]
```

**Error Responses:**

| Status | Condition                                      |
|--------|------------------------------------------------|
| `404`  | Author not found in DB or OpenLibrary          |
| `503`  | OpenLibrary API is unavailable (circuit open)  |

---

## Resilience Configuration

The OpenLibrary integration is protected with Resilience4j:

- **Circuit Breaker** – Opens after 50% failure rate in a sliding window of 10 calls; waits 30s before half-open.
- **Retry** – Up to 3 attempts with exponential backoff (starting at 500ms, multiplier 2×).

---

## Project Structure

```
digital-library/
├── Dockerfile                # Multi-stage Docker build
├── docker-compose.yml        # MySQL + App orchestration
├── pom.xml
└── src/main/
    ├── java/com/digitallibrary/
    │   ├── client/           # OpenLibrary API client
    │   ├── config/           # App configuration
    │   ├── controller/       # REST controllers
    │   ├── dto/              # Request/Response DTOs
    │   ├── entity/           # JPA entities
    │   ├── exception/        # Exception handling
    │   ├── repository/       # Spring Data repositories
    │   └── service/          # Business logic
    └── resources/
        ├── application.yaml  # App configuration (Docker Compose ready)
        └── db/changelog/     # Liquibase migrations (XML)
            ├── db.changelog-master.xml
            └── changes/
                ├── 001-create-author-table.xml
                └── 002-create-work-table.xml
```
