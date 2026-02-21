# School API – Teacher & Student Roles

Spring Boot API with **teacher** and **student** roles, PostgreSQL, and Docker.

## Roles

- **Teacher**: Can view and edit any student's info (name, email, grade).
- **Student**: Can only edit their **own name** (no email/grade).

## Run with Docker

**Before running:** Start **Docker Desktop** and wait until it is fully running. On Windows, if you see a "docker client must be run with elevated privileges" or "cannot find the file specified" error, **run your terminal as Administrator** (right‑click PowerShell or Command Prompt → "Run as administrator"), then run:

From the project root:

```bash
docker compose up --build
```

- **API**: http://localhost:8080  
- **PostgreSQL**: localhost:5432 (user: `school`, password: `school`, database: `school`)

On first run (with profile `docker`), default users are created:

| Username | Password | Role    |
|----------|----------|--------|
| teacher  | teacher  | TEACHER |
| student  | student  | STUDENT |

## API (HTTP Basic Auth)

- **Register** (no auth):  
  `POST /api/auth/register`  
  Body: `{"username":"...","password":"...","name":"...","role":"STUDENT"|"TEACHER","email":"...","grade":"..."}`

- **List students**:  
  `GET /api/students` (auth required)

- **Get one student**:  
  `GET /api/students/{id}` (auth required)

- **Update student** (teacher: any field; student: only self, only `name`):  
  `PATCH /api/students/{id}`  
  Body: `{"name":"..."}` or `{"name":"...","email":"...","grade":"..."}`

Example (student updates own name):

```bash
curl -u student:student -X PATCH http://localhost:8080/api/students/2 -H "Content-Type: application/json" -d "{\"name\":\"New Name\"}"
```

Example (teacher updates a student):

```bash
curl -u teacher:teacher -X PATCH http://localhost:8080/api/students/2 -H "Content-Type: application/json" -d "{\"name\":\"Updated\",\"email\":\"a@b.com\",\"grade\":\"B\"}"
```

## Run locally (no Docker)

1. Start PostgreSQL (e.g. local install or another Docker Postgres) with database `school`, user `school`, password `school`.
2. Set env or `application.yaml`:  
   `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` (defaults in `application.yaml` use `localhost:5432` and `school`/`school`).
3. Run: `./mvnw spring-boot:run`

## Troubleshooting Docker on Windows

- **"cannot find the file specified" / "docker_engine"**  
  Docker Desktop is not running or the client cannot connect. Do this:
  1. Start **Docker Desktop** from the Start menu and wait until the whale icon in the system tray is steady (not animating).
  2. If it still fails, close your terminal, **right‑click** PowerShell or Command Prompt → **Run as administrator**, then `cd` to the project and run `docker compose up --build` again.

- **Build fails with "bad interpreter" or mvnw errors**  
  The Dockerfile already normalizes line endings for `mvnw`; ensure you have the latest Dockerfile and rebuild with `docker compose build --no-cache`.
