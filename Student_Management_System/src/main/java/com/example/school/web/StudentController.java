package com.example.school.web;

import com.example.school.entity.User;
import com.example.school.security.SchoolUserDetails;
import com.example.school.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.school.entity.Role;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final UserService userService;

    public StudentController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> listStudents() {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<StudentResponse> list = userService.findStudentsForCurrentUser(current.get()).stream()
                .map(StudentResponse::from)
                .collect(Collectors.toList());
        // Student: only ever return their own record (never others)
        if (!current.get().isTeacher() && !list.isEmpty()) {
            list = list.stream()
                    .filter(s -> s.id().equals(current.get().getUserId()))
                    .limit(1)
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/me")
    public ResponseEntity<StudentResponse> getMe() {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<User> user = userService.findById(current.get().getUserId());
        if (user.isEmpty() || user.get().getRole() != Role.STUDENT) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(StudentResponse.from(user.get()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudent(@PathVariable Long id) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!current.get().isTeacher() && !current.get().getUserId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<User> user = userService.findById(id);
        if (user.isEmpty() || user.get().getRole() != Role.STUDENT) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(StudentResponse.from(user.get()));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@RequestBody Map<String, Object> body) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<User> created = userService.createStudent(body, current.get());
        if (created.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(StudentResponse.from(created.get()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<User> updated = userService.updateStudentInfo(id, updates, current.get());
        if (updated.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(StudentResponse.from(updated.get()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!userService.deleteStudent(id, current.get())) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    public record StudentResponse(Long id, String username, String name, String email, String grade) {
        static StudentResponse from(User u) {
            return new StudentResponse(u.getId(), u.getUsername(), u.getName(), u.getEmail(), u.getGrade());
        }
    }
}
