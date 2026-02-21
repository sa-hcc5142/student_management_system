# Authentication and Feature Flow (Student & Teacher)

This document describes the flow from login through every feature, and which functions are used for each.

---

## 1. Login Flow (Same for Student and Teacher)

| Step | Where | What Happens |
|------|--------|--------------|
| 1 | User opens `/login.html` | **SecurityConfig**: `permitAll()` for `/login.html` → static page served. |
| 2 | User submits form (POST to `/login`) | **SecurityConfig**: `loginProcessingUrl("/login")` → Spring Security handles the request. |
| 3 | Spring Security | Reads `username` and `password` from the form, then calls **UserDetailsService** to load the user. |
| 4 | **SchoolUserDetailsService.loadUserByUsername(username)** | `UserRepository.findByUsername(username)` → wraps result in **SchoolUserDetails** (or throws if not found). |
| 5 | **SchoolUserDetails** | Provides `getPassword()`, `getUsername()`, `getAuthorities()` (e.g. `ROLE_TEACHER` / `ROLE_STUDENT`). Spring compares form password with stored (BCrypt) password. |
| 6 | Success | **SecurityConfig**: `defaultSuccessUrl("/", true)` → redirect to `/` → **HomeController.home()** → redirect to `/welcome.html`. Session now has authenticated principal = **SchoolUserDetails**. |
| 7 | Failure | **SecurityConfig**: `failureUrl("/login.html?error=true")` → user sees login again with error. |

**Summary:** Login uses **SecurityConfig** (form + URLs), **SchoolUserDetailsService.loadUserByUsername**, **SchoolUserDetails**, and **UserRepository.findByUsername**. No controller method handles the POST `/login`; it’s all Spring Security.

---

## 2. After Login – “Who Am I?” (Both Roles)

| Feature | HTTP | Controller | Service / Security |
|--------|------|------------|--------------------|
| Current user info | `GET /api/auth/me` | **AuthController.me()** | **UserService.getCurrentUserDetails()** (from `SecurityContextHolder`) |

Used by the frontend to know if the user is teacher/student and to show name/username.

---

## 3. Teacher-Only Features and Functions

### 3.1 Register New User (Teacher or Student)

| Step | HTTP | Controller | Service / Repo |
|------|------|------------|----------------|
| Register | `POST /api/auth/register` | **AuthController.register()** | **UserService.getCurrentUserDetails()** (must be teacher); **UserRepository.findByUsername()**; **UserRepository.save()** (and **PasswordEncoder** in controller). |

**AuthController** does the role check and creation; **UserService** only supplies current user.

### 3.2 Students (CRUD)

| Feature | HTTP | Controller Method | UserService Method |
|--------|------|-------------------|--------------------|
| List students | `GET /api/students` | **StudentController.listStudents()** | **UserService.getCurrentUserDetails()**, **UserService.findStudentsForCurrentUser()** (teacher → all students) |
| Get one student | `GET /api/students/{id}` | **StudentController.getStudent()** | **UserService.getCurrentUserDetails()**, **UserService.findById()** |
| Create student | `POST /api/students` | **StudentController.createStudent()** | **UserService.getCurrentUserDetails()**, **UserService.createStudent()** |
| Update student | `PATCH /api/students/{id}` | **StudentController.updateStudent()** | **UserService.getCurrentUserDetails()**, **UserService.updateStudentInfo()** |
| Delete student | `DELETE /api/students/{id}` | **StudentController.deleteStudent()** | **UserService.getCurrentUserDetails()**, **UserService.deleteStudent()** |

**UserService** uses: **UserRepository.findById()**, **UserRepository.findByRole()**, **UserRepository.findByUsername()**, **UserRepository.save()**, **UserRepository.delete()**, **PasswordEncoder**.

### 3.3 Classes (Teacher’s Own Classes)

| Feature | HTTP | Controller Method | ClassService Method |
|--------|------|-------------------|---------------------|
| List my classes | `GET /api/classes` | **ClassController.listClasses()** | **UserService.getCurrentUserDetails()**, **ClassService.findClassesForCurrentUser()** (teacher path), **ClassService.enrollmentCountByClassId()** |
| Get one class | `GET /api/classes/{id}` | **ClassController.getClass()** | **UserService.getCurrentUserDetails()**, **ClassService.findById()**, **ClassService.enrollmentCountByClassId()** |
| Create class | `POST /api/classes` | **ClassController.createClass()** | **UserService.getCurrentUserDetails()**, **ClassService.createClass()** |
| Update class | `PATCH /api/classes/{id}` | **ClassController.updateClass()** | **UserService.getCurrentUserDetails()**, **ClassService.updateClass()** |
| Delete class | `DELETE /api/classes/{id}` | **ClassController.deleteClass()** | **UserService.getCurrentUserDetails()**, **ClassService.deleteClass()** |

**ClassService** uses: **SchoolClassRepository**, **UserRepository**, **EnrollmentRepository** (e.g. for counts).

### 3.4 Class Enrollments (Teacher Manages Roster)

| Feature | HTTP | Controller Method | ClassService Method |
|--------|------|-------------------|---------------------|
| List enrolled students | `GET /api/classes/{id}/enrollments` | **ClassController.listEnrollments()** | **UserService.getCurrentUserDetails()**, **ClassService.findEnrolledStudentsByClassId()** (teacher only) |
| Remove student from class | `DELETE /api/classes/{id}/enrollments/{studentId}` | **ClassController.removeStudentFromClass()** | **UserService.getCurrentUserDetails()**, **ClassService.removeStudentFromClass()** (teacher only) |

For teacher: every feature goes through **UserService.getCurrentUserDetails()** first, then the corresponding **UserService** or **ClassService** method that enforces “teacher only” or “teacher’s own resource”.

---

## 4. Student-Only / Student-Relevant Features and Functions

### 4.1 “My” Student Info

| Feature | HTTP | Controller Method | UserService Method |
|--------|------|-------------------|--------------------|
| My profile | `GET /api/students/me` | **StudentController.getMe()** | **UserService.getCurrentUserDetails()**, **UserService.findById()** (current user’s id) |
| List “students” (only self) | `GET /api/students` | **StudentController.listStudents()** | **UserService.getCurrentUserDetails()**, **UserService.findStudentsForCurrentUser()** (student path: only self); controller also filters so student sees only their own in the list. |
| Get one student | `GET /api/students/{id}` | **StudentController.getStudent()** | **UserService.getCurrentUserDetails()**, **UserService.findById()**; controller returns 403 if student asks for another user’s id. |

Create/Update/Delete student: controller allows the call, but **UserService.createStudent** / **updateStudentInfo** / **deleteStudent** return empty/false for non-teachers, so students cannot actually change student data.

### 4.2 Classes (Browse and Enroll)

| Feature | HTTP | Controller Method | ClassService Method |
|--------|------|-------------------|---------------------|
| List all classes (+ enrolled) | `GET /api/classes` | **ClassController.listClasses()** | **UserService.getCurrentUserDetails()**, **ClassService.findClassesForCurrentUser()** (student path: all classes), **ClassService.enrolledClassIdsForStudent()** |
| Get one class | `GET /api/classes/{id}` | **ClassController.getClass()** | **UserService.getCurrentUserDetails()**, **ClassService.findById()**, **ClassService.enrolledClassIdsForStudent()** (to set `enrolled`) |
| Enroll in class | `POST /api/classes/{id}/enroll` | **ClassController.enroll()** | **UserService.getCurrentUserDetails()**, **ClassService.enroll()** (student only) |
| Unenroll | `DELETE /api/classes/{id}/enroll` | **ClassController.unenroll()** | **UserService.getCurrentUserDetails()**, **ClassService.unenroll()** (student only) |

Students cannot create/update/delete classes or manage enrollments; **ClassService** returns false or empty for those when the user is not a teacher.

---

## 5. Function Usage Summary

### Used on (Almost) Every Authenticated Request

- **UserService.getCurrentUserDetails()** – reads **SecurityContextHolder.getContext().getAuthentication().getPrincipal()** as **SchoolUserDetails** (used by **AuthController**, **StudentController**, **ClassController**).

### Authentication (Login Only)

- **SchoolUserDetailsService.loadUserByUsername()**
- **SchoolUserDetails** (principal + password/authorities)
- **UserRepository.findByUsername()**
- **SecurityConfig** (form login, success/failure URLs)

### User/Student Features

- **UserService**: `findById`, `findStudentsForCurrentUser`, `createStudent`, `updateStudentInfo`, `deleteStudent`
- **UserRepository**: `findById`, `findByUsername`, `findByRole`, `save`, `delete`

### Class and Enrollment Features

- **ClassService**: `findClassesForCurrentUser`, `findById`, `createClass`, `updateClass`, `deleteClass`, `enroll`, `unenroll`, `enrolledClassIdsForStudent`, `enrollmentCountByClassId`, `findEnrolledStudentsByClassId`, `removeStudentFromClass`
- **SchoolClassRepository**, **EnrollmentRepository**, **UserRepository** (as used inside **ClassService**)

### Auth (After Login)

- **AuthController.me()** → **UserService.getCurrentUserDetails()**
- **AuthController.register()** → **UserService.getCurrentUserDetails()**, **UserRepository** + **PasswordEncoder**

---

## Summary

- **Login** is handled by Spring Security + **SchoolUserDetailsService** + **SchoolUserDetails**.
- **Everything after login** goes through **UserService.getCurrentUserDetails()** and then the appropriate **UserService** or **ClassService** methods, which implement the actual “teacher vs student” and “own resource” rules.
