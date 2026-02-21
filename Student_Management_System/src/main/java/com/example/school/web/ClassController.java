package com.example.school.web;

import com.example.school.entity.SchoolClass;
import com.example.school.entity.User;
import com.example.school.security.SchoolUserDetails;
import com.example.school.service.ClassService;
import com.example.school.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;
    private final UserService userService;

    public ClassController(ClassService classService, UserService userService) {
        this.classService = classService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> listClasses() {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<SchoolClass> classes = classService.findClassesForCurrentUser(current.get());
        boolean isTeacher = current.get().isTeacher();
        Set<Long> enrolledIds = isTeacher ? Set.of() : classService.enrolledClassIdsForStudent(current.get());
        Map<Long, Long> enrollmentCounts = isTeacher ? classService.enrollmentCountByClassId(classes) : Map.of();
        List<ClassResponse> list = classes.stream()
                .map(c -> ClassResponse.from(c, isTeacher ? enrollmentCounts.getOrDefault(c.getId(), 0L) : null, enrolledIds.contains(c.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassResponse> getClass(@PathVariable Long id) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<SchoolClass> c = classService.findById(id);
        if (c.isEmpty()) return ResponseEntity.notFound().build();
        SchoolClass sc = c.get();
        boolean isTeacher = current.get().isTeacher();
        boolean isEnrolled = !isTeacher && classService.enrolledClassIdsForStudent(current.get()).contains(id);
        Long count = isTeacher ? (long) classService.enrollmentCountByClassId(List.of(sc)).getOrDefault(sc.getId(), 0L) : null;
        return ResponseEntity.ok(ClassResponse.from(sc, count, isEnrolled));
    }

    @PostMapping
    public ResponseEntity<ClassResponse> createClass(@RequestBody Map<String, Object> body) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<SchoolClass> created = classService.createClass(body, current.get());
        if (created.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassResponse.from(created.get(), null, false));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClassResponse> updateClass(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<SchoolClass> updated = classService.updateClass(id, updates, current.get());
        if (updated.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ClassResponse.from(updated.get(), null, false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!classService.deleteClass(id, current.get())) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<Void> enroll(@PathVariable Long id) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!classService.enroll(id, current.get())) return ResponseEntity.badRequest().build();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/enroll")
    public ResponseEntity<Void> unenroll(@PathVariable Long id) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!classService.unenroll(id, current.get())) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/enrollments")
    public ResponseEntity<?> listEnrollments(@PathVariable Long id) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!current.get().isTeacher()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        List<User> students = classService.findEnrolledStudentsByClassId(id, current.get());
        List<EnrolledStudentResponse> list = students.stream()
                .map(EnrolledStudentResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}/enrollments/{studentId}")
    public ResponseEntity<Void> removeStudentFromClass(@PathVariable Long id, @PathVariable Long studentId) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!classService.removeStudentFromClass(id, studentId, current.get())) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    public record EnrolledStudentResponse(Long id, String username, String name, String email, String grade) {
        static EnrolledStudentResponse from(User u) {
            return new EnrolledStudentResponse(u.getId(), u.getUsername(), u.getName(), u.getEmail(), u.getGrade());
        }
    }

    public record ClassResponse(Long id, String name, String description, String teacherName, Long enrollmentCount, Boolean enrolled) {
        static ClassResponse from(SchoolClass c, Long enrollmentCount, boolean enrolled) {
            User t = c.getTeacher();
            return new ClassResponse(
                    c.getId(),
                    c.getName(),
                    c.getDescription(),
                    t != null ? t.getName() : null,
                    enrollmentCount,
                    enrolled
            );
        }
    }
}
