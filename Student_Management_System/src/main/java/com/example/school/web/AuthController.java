package com.example.school.web;

import com.example.school.entity.Role;
import com.example.school.entity.User;
import com.example.school.repository.UserRepository;
import com.example.school.security.SchoolUserDetails;
import com.example.school.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        return userService.getCurrentUserDetails()
                .map(d -> ResponseEntity.<Map<String, Object>>ok(Map.of(
                        "username", d.getUsername(),
                        "name", d.getName() != null ? d.getName() : d.getUsername(),
                        "role", d.isTeacher() ? "TEACHER" : "STUDENT"
                )))
                .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        if (!current.get().isTeacher()) return ResponseEntity.status(403).body(Map.of("error", "Only teachers can register new users"));
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already exists"));
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setRole(request.role() != null ? request.role() : Role.STUDENT);
        user.setEmail(request.email());
        user.setGrade(request.grade());
        user = userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole().name()
        ));
    }

    public record RegisterRequest(String username, String password, String name, Role role, String email, String grade) {}
}
