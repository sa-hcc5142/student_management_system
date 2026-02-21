# School Application – Complete Setup & Code Guide

This guide covers setup, architecture, and how each file and function works.

---

## Table of Contents

1. [Tech Stack & Overview](#1-tech-stack--overview)
2. [Setup](#2-setup)
3. [Project Structure](#3-project-structure)
4. [Configuration Files](#4-configuration-files)
5. [Entities (Data Model)](#5-entities-data-model)
6. [Repositories](#6-repositories)
7. [Security Layer](#7-security-layer)
8. [Services](#8-services)
9. [Controllers (REST API)](#9-controllers-rest-api)
10. [Static UI (HTML/JS)](#10-static-ui-htmljs)
11. [Request Flow Summary](#11-request-flow-summary)

---

## 1. Tech Stack & Overview

| Layer        | Technology                    |
|-------------|-------------------------------|
| Language    | Java 17                       |
| Framework   | Spring Boot 4.0.2             |
| Security    | Spring Security (form + HTTP Basic) |
| Data        | Spring Data JPA, PostgreSQL   |
| Build       | Maven                         |
| Frontend    | Vanilla HTML/CSS/JS (no framework) |

**Roles:**
- **Teacher**: Manage students (CRUD), create/edit/delete classes, view enrollments, remove students from classes, register new users.
- **Student**: View own profile, browse classes, enroll/unenroll in classes.

---

## 2. Setup

### Prerequisites

- **Java 17** (JDK)
- **Maven** (or use project’s `mvnw` / `mvnw.cmd`)
- **PostgreSQL** (for DB; or use Docker)

### Option A: Run locally (no Docker)

1. **Start PostgreSQL** (localhost, port 5432) with a database and user. Default from `application.yaml`:
   - Database: `school`
   - User: `school`
   - Password: `school`

2. **Run the app:**
   ```bash
   ./mvnw spring-boot:run
   ```
   (Windows: `mvnw.cmd spring-boot:run`)

3. Open: **http://localhost:8080**

### Option B: Run with Docker Compose

1. From project root:
   ```bash
   docker compose up --build
   ```
2. App: **http://localhost:8080**  
   PostgreSQL runs in a container; the app container connects to it.

### Default login accounts (created by DataInitializer when profile `docker` is active)

| Role    | Username | Password |
|---------|----------|----------|
| Teacher | teacher  | teacher  |
| Student | student  | student  |

---

## 3. Project Structure

```
school/
├── pom.xml                          # Maven dependencies & build
├── mvnw, mvnw.cmd, .mvn/            # Maven wrapper
├── Dockerfile                       # Multi-stage build for app image
├── docker-compose.yml               # Postgres + app services
├── GUIDE.md                         # This file
│
├── src/main/
│   ├── java/com/example/school/
│   │   ├── SchoolApplication.java   # Spring Boot entry point
│   │   ├── config/                  # Security, data init
│   │   ├── entity/                  # JPA entities
│   │   ├── repository/              # JPA repositories
│   │   ├── security/                # UserDetails, UserDetailsService
│   │   ├── service/                 # Business logic
│   │   └── web/                     # REST controllers
│   │
│   └── resources/
│       ├── application.yaml        # Main config (DB, JPA, server)
│       ├── application-docker.yaml # Overrides when running in Docker
│       └── static/                  # HTML/CSS/JS (login, welcome, students, classes, register)
│
└── src/test/                        # Tests
```

---

## 4. Configuration Files

### `pom.xml`

- **Parent**: `spring-boot-starter-parent` 4.0.2, Java 17.
- **Main dependencies**:
  - `spring-boot-starter-data-jpa` – JPA + Hibernate
  - `spring-boot-starter-security` – authentication/authorization
  - `spring-boot-starter-webmvc` – REST and static resources
  - `spring-boot-starter-validation` – bean validation
  - `spring-boot-starter-actuator` – health etc.
  - `postgresql` – JDBC driver

### `application.yaml`

- **Datasource**: URL, username, password from env vars with defaults (e.g. `localhost:5432/school`, user `school`).
- **JPA**: `ddl-auto: update` (schema created/updated from entities), PostgreSQL dialect, `open-in-view: false`.
- **Server**: port from `SERVER_PORT` (default 8080).

### `application-docker.yaml`

- Active when `SPRING_PROFILES_ACTIVE=docker`.
- Overrides datasource URL to `jdbc:postgresql://postgres:5432/school` for the Docker Compose service name.

### `SecurityConfig.java` (in config)

- **CSRF**: disabled (typical for REST + form login).
- **Access**:
  - Permit all: `/login.html`, `/login`, `/actuator/health`.
  - Authenticated: `/api/students/**`, `/api/classes/**`, and everything else (including static pages).
- **Form login**: page `/login.html`, POST `/login`, success redirect `/`, failure `/login.html?error=true`.
- **HTTP Basic**: enabled (e.g. for API calls).
- **Password encoding**: `BCryptPasswordEncoder` bean.

### `DataInitializer.java` (in config)

- **Profile**: `docker` only.
- **Runs**: after application context is up (`CommandLineRunner`).
- **Logic**: If no user `"teacher"` exists, creates one (password encoded, role TEACHER). Same for `"student"` (role STUDENT). So default accounts exist when running with Docker profile.

---

## 5. Entities (Data Model)

### `Role.java` (enum)

- `TEACHER`, `STUDENT`.
- Used in `User` and for authorization checks.

### `User.java`

- **Table**: `users`.
- **Fields**: `id`, `username` (unique), `password`, `role` (enum), `name`, `email`, `grade`.
- **Usage**: Both teachers and students are stored here; `role` distinguishes them.

### `SchoolClass.java`

- **Table**: `school_classes`.
- **Fields**: `id`, `name`, `description`, `teacher` (ManyToOne → `User`).
- **Usage**: A class is owned by one teacher; students link via enrollments.

### `Enrollment.java`

- **Table**: `enrollments`.
- **Unique constraint**: `(student_id, school_class_id)` – a student can enroll in a class only once.
- **Fields**: `id`, `student` (ManyToOne → `User`), `schoolClass` (ManyToOne → `SchoolClass`).
- **Usage**: Represents “student X is in class Y”.

---

## 6. Repositories

All extend `JpaRepository<Entity, Long>` and provide the data access used by services.

### `UserRepository`

- `findByUsername(String)` – for login and duplicate checks.
- `findByRole(Role)` – list all students or all teachers.

### `SchoolClassRepository`

- `findByTeacherOrderByName(User)` – classes of one teacher.
- `findAllByOrderByName()` – all classes (for students’ list).

### `EnrollmentRepository`

- `findByStudentOrderBySchoolClass_Name(User)` – enrollments of one student (for “my classes”).
- `findByStudentAndSchoolClass(User, SchoolClass)` – check one enrollment.
- `existsByStudentAndSchoolClass(User, SchoolClass)` – used before creating enrollment.
- `findBySchoolClass(SchoolClass)` – list enrollments for a class (teacher view).

---

## 7. Security Layer

### `SchoolUserDetails.java`

- Implements `UserDetails`.
- Wraps a `User` entity; exposes `getUsername()`, `getPassword()`, `getAuthorities()` (from role), and custom:
  - `getUserId()` – `User.getId()`
  - `isTeacher()` – `role == TEACHER`
  - `getName()` – `User.getName()`
- Used as the principal in the security context after login.

### `SchoolUserDetailsService.java`

- Implements `UserDetailsService`.
- `loadUserByUsername(String username)`: loads `User` from `UserRepository`, returns `SchoolUserDetails`.
- Called by Spring Security during form login to get the user and validate password.

---

## 8. Services

### `UserService`

- **getCurrentUserDetails()**: Reads `SecurityContextHolder.getAuthentication().getPrincipal()` and returns `Optional<SchoolUserDetails>` (empty if not logged in or wrong type).
- **findById(Long)**: Fetch user by id.
- **findStudentsForCurrentUser(SchoolUserDetails)**:
  - Teacher → all users with role STUDENT.
  - Student → only themselves (single-element list or empty).
- **createStudent(Map, SchoolUserDetails)**: Teacher only. Builds `User` from map (username, password, name, email, grade), encodes password, sets role STUDENT, saves.
- **updateStudentInfo(Long, Map, SchoolUserDetails)**: Teacher only. Updates name, email, grade; optionally password if provided in map.
- **deleteStudent(Long, SchoolUserDetails)**: Teacher only. Deletes the student user.

### `ClassService`

- **findClassesForCurrentUser(SchoolUserDetails)**:
  - Teacher → their classes (by teacher id), ordered by name.
  - Student → all classes, ordered by name.
- **findById(Long)**: Get one class by id.
- **createClass(Map, SchoolUserDetails)**: Teacher only. Creates `SchoolClass` with name, description, current user as teacher.
- **updateClass(Long, Map, SchoolUserDetails)**: Teacher only. Updates name/description of a class owned by current user.
- **deleteClass(Long, SchoolUserDetails)**: Teacher only. Deletes a class owned by current user (enrollments are removed by DB cascade or by design).
- **enroll(Long classId, SchoolUserDetails)**: Student only. Creates an `Enrollment` for current user and class (idempotent if already enrolled).
- **unenroll(Long classId, SchoolUserDetails)**: Student only. Deletes the enrollment for current user and class.
- **enrolledClassIdsForStudent(SchoolUserDetails)**: Returns set of class ids the current student is enrolled in (for UI “enrolled” flag).
- **enrollmentCountByClassId(List<SchoolClass>)**: Returns map classId → count of enrollments (for teacher UI).
- **findEnrolledStudentsByClassId(Long, SchoolUserDetails)**: Teacher only. Returns list of `User` (students) enrolled in that class (must be owned by current teacher). `@Transactional(readOnly = true)` so lazy `student` on `Enrollment` loads in same transaction.
- **removeStudentFromClass(Long classId, Long studentId, SchoolUserDetails)**: Teacher only. Deletes the enrollment for that student in that class (only if class is owned by current teacher).

---

## 9. Controllers (REST API)

### `HomeController`

- **GET /**  
  Redirects to `/welcome.html`.
- **GET /api**  
  Returns a small JSON with “message” and “docs” (map of endpoint names to descriptions). Used as a quick API overview.

### `AuthController` (`/api/auth`)

- **GET /api/auth/me**  
  Returns current user’s `username`, `name`, `role` (TEACHER/STUDENT). 401 if not authenticated.
- **POST /api/auth/register**  
  **Teacher only** (returns 401 if not logged in, 403 if not teacher). Body: username, password, name, role (optional, default STUDENT), email, grade. Creates a new `User` (password encoded). Returns id, username, role. Bad request if username already exists.

### `StudentController` (`/api/students`)

- **GET /api/students**  
  List students: teacher sees all students; student sees only themselves (single-element list). Returns array of `StudentResponse` (id, username, name, email, grade).
- **GET /api/students/me**  
  Current user’s own record (must be STUDENT). 404 if not student.
- **GET /api/students/{id}**  
  Teacher can get any student; student can get only own id. Returns `StudentResponse`.
- **POST /api/students**  
  Teacher only. Creates student (same shape as register). Returns `StudentResponse`, 201.
- **PATCH /api/students/{id}**  
  Teacher only. Partial update (name, email, grade, optional password). Returns `StudentResponse`.
- **DELETE /api/students/{id}**  
  Teacher only. Deletes student. Returns 204.

### `ClassController` (`/api/classes`)

- **GET /api/classes**  
  Teacher: their classes with `enrollmentCount`. Student: all classes with `enrolled` true/false per class. Returns list of `ClassResponse` (id, name, description, teacherName, enrollmentCount or enrolled).
- **GET /api/classes/{id}**  
  One class; for teacher includes enrollment count, for student includes enrolled flag.
- **POST /api/classes**  
  Teacher only. Body: name, description. Creates class. 201 + `ClassResponse`.
- **PATCH /api/classes/{id}**  
  Teacher only. Updates name/description. Returns `ClassResponse`.
- **DELETE /api/classes/{id}**  
  Teacher only. Deletes class. 204.
- **POST /api/classes/{id}/enroll**  
  Student only. Enrolls current user in class. 204.
- **DELETE /api/classes/{id}/enroll**  
  Student only. Unenrolls current user. 204.
- **GET /api/classes/{id}/enrollments**  
  Teacher only. List of students in that class. Returns list of `EnrolledStudentResponse` (id, username, name, email, grade).
- **DELETE /api/classes/{id}/enrollments/{studentId}**  
  Teacher only. Removes that student from the class. 204.

---

## 10. Static UI (HTML/JS)

All under `src/main/resources/static/`. Served at `/`; access requires authentication except login.

### `login.html`

- Form: username, password. POST to `/login`.
- On `?error=true` shows “Invalid username or password.”
- Hint text for default teacher/student credentials.

### `welcome.html`

- After load, calls **GET /api/auth/me** to get current user.
- Shows “Welcome, {name}” and a role badge (Teacher/Student).
- Links (shown only when logged in):
  - **Manage students** (teacher) or **My information** (student) → `/students.html`
  - **My classes** (teacher) or **Browse classes** (student) → `/classes.html`
  - **Register user** → `/register.html` (only if role is TEACHER)
  - **Log out** → POST `/logout`
- If `/api/auth/me` fails, shows “Please log in” with link to `/login.html`.

### `students.html`

- **Teacher**: Title “Students”, button “Create student”, table of all students with Edit/Delete. Create/Edit form: username, password, name, email, grade; Save/Cancel. Data from **GET /api/students**, create **POST /api/students**, update **PATCH /api/students/{id}**, delete **DELETE /api/students/{id}**.
- **Student**: Title “My information”, single profile card (no create/edit/delete). Data from **GET /api/students** (first element).
- Uses **GET /api/auth/me** to decide role. “Back” goes to `/welcome.html`.

### `classes.html`

- **Teacher**: Title “My classes”, “Create class” button, table: Name, Description, Enrolled count, Actions (View enrolled, Edit, Delete). Create/Edit form: name, description. “View enrolled” toggles a row that loads **GET /api/classes/{id}/enrollments** and shows a table of students with “Remove” (calls **DELETE /api/classes/{id}/enrollments/{studentId}**).
- **Student**: Title “Classes”, list of class cards with name, description, teacher; **Enroll** or **Unenroll** using **POST/DELETE /api/classes/{id}/enroll**. Enrolled classes styled differently.
- **GET /api/classes** and **GET /api/auth/me** drive role and data.

### `register.html`

- **GET /api/auth/me**: If not teacher, redirect to `/welcome.html`; if not logged in, redirect to `/login.html`.
- Form: username, password, name, role (Student/Teacher), email, grade. Submit → **POST /api/auth/register** (teacher-only). On success shows message and clears form; on error shows error from response body.

---

## 11. Request Flow Summary

1. **Login**  
   User submits form → POST `/login` → `SchoolUserDetailsService.loadUserByUsername` → password checked → session created → redirect `/` → `HomeController` redirects to `/welcome.html`.

2. **Welcome**  
   Browser loads `welcome.html` → JS calls **GET /api/auth/me** → response gives role → UI shows name, badge, and links (e.g. Register only for teachers).

3. **Students page**  
   **GET /api/auth/me** then **GET /api/students**. Teacher: full CRUD via API. Student: read-only profile from same list.

4. **Classes page**  
   **GET /api/auth/me** then **GET /api/classes**. Teacher: create/edit/delete classes, view enrollments, remove student. Student: enroll/unenroll via **POST/DELETE /api/classes/{id}/enroll**.

5. **Registration**  
   Only teachers see the link. **POST /api/auth/register** checks current user is teacher, then creates new user (username must be unique).

6. **Authorization**  
   - Controllers use `UserService.getCurrentUserDetails()` and `currentUser.isTeacher()` (or id checks) to enforce teacher-only or student-only actions.
   - Security config ensures only `/login`, `/login.html`, and `/actuator/health` are public; all other paths require authentication.

This is the full setup-to-code guideline: how to run the app, what each major file and function does, and how requests flow from login through the UI and API.
