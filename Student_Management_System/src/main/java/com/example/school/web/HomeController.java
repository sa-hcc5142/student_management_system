package com.example.school.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/welcome.html");
    }

    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        return ResponseEntity.ok(Map.of(
                "message", "School API is running",
                "docs", Map.ofEntries(
                        Map.entry("register", "POST /api/auth/register (teacher only)"),
                        Map.entry("listStudents", "GET /api/students (teacher: all; student: only self)"),
                        Map.entry("getMe", "GET /api/students/me (student: my info)"),
                        Map.entry("getStudent", "GET /api/students/{id} (teacher: any; student: own only)"),
                        Map.entry("createStudent", "POST /api/students (teacher only)"),
                        Map.entry("updateStudent", "PATCH /api/students/{id} (teacher only)"),
                        Map.entry("deleteStudent", "DELETE /api/students/{id} (teacher only)"),
                        Map.entry("listClasses", "GET /api/classes (teacher: my classes; student: all with enrolled)"),
                        Map.entry("getClass", "GET /api/classes/{id}"),
                        Map.entry("createClass", "POST /api/classes (teacher only)"),
                        Map.entry("updateClass", "PATCH /api/classes/{id} (teacher only)"),
                        Map.entry("deleteClass", "DELETE /api/classes/{id} (teacher only)"),
                        Map.entry("enroll", "POST /api/classes/{id}/enroll (student only)"),
                        Map.entry("unenroll", "DELETE /api/classes/{id}/enroll (student only)"),
                        Map.entry("health", "GET /actuator/health")
                )
        ));
    }
}
