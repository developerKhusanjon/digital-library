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

---

## Prerequisites

| Tool  | Version |
|-------|---------|
| JDK   | 17+     |
| Maven  | 3.8+    |
| MySQL  | 8.0+    |

---

## Database Setup

1. Start your MySQL server.
2. Create the database:

```sql
CREATE DATABASE digital_library;
```

3. The default connection settings in `application.yml` are:

```yaml
url: jdbc:mysql://localhost:3306/digital_library
username: root
password: root
```

> Adjust these values in [`src/main/resources/application.yml`](src/main/resources/application.yml) if your MySQL credentials differ.

Liquibase will automatically create the `author` and `work` tables on startup.

---

## Running the Application

```bash
# Clone the repository
git clone https://github.com/developerKhusanjon/digital-library.git
cd digital-library

# Build the project
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run
```

The server starts on **http://localhost:8080**.

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
src/main/
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
    ├── application.yml   # App configuration
    └── db/changelog/     # Liquibase migrations (XML)
        ├── db.changelog-master.xml
        └── changes/
            ├── 001-create-author-table.xml
            └── 002-create-work-table.xml
```
