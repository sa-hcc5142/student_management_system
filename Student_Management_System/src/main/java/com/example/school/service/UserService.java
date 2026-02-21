package com.example.school.service;

import com.example.school.entity.Role;
import com.example.school.entity.User;
import com.example.school.repository.UserRepository;
import com.example.school.security.SchoolUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAllStudents() {
        return userRepository.findByRole(Role.STUDENT);
    }

    /** Teacher sees all students; student sees only themselves. */
    public List<User> findStudentsForCurrentUser(SchoolUserDetails currentUser) {
        if (currentUser.isTeacher()) {
            return userRepository.findByRole(Role.STUDENT);
        }
        return userRepository.findById(currentUser.getUserId())
                .filter(u -> u.getRole() == Role.STUDENT)
                .map(List::of)
                .orElse(List.of());
    }

    /** Teacher only: create a new student. */
    @Transactional
    public Optional<User> createStudent(Map<String, Object> body, SchoolUserDetails currentUser) {
        if (!currentUser.isTeacher()) return Optional.empty();
        String username = (String) body.get("username");
        if (username == null || username.isBlank() || userRepository.findByUsername(username).isPresent()) {
            return Optional.empty();
        }
        User student = new User();
        student.setUsername(username.trim());
        student.setPassword(passwordEncoder.encode((String) body.getOrDefault("password", "changeme")));
        student.setName((String) body.getOrDefault("name", username));
        student.setEmail((String) body.get("email"));
        student.setGrade((String) body.get("grade"));
        student.setRole(Role.STUDENT);
        return Optional.of(userRepository.save(student));
    }

    /** Teacher only: update any student's info. */
    @Transactional
    public Optional<User> updateStudentInfo(Long studentId, Map<String, Object> updates, SchoolUserDetails currentUser) {
        if (!currentUser.isTeacher()) return Optional.empty();
        Optional<User> opt = userRepository.findById(studentId);
        if (opt.isEmpty()) return Optional.empty();
        User student = opt.get();
        if (student.getRole() != Role.STUDENT) return Optional.empty();
        if (updates.containsKey("name")) student.setName((String) updates.get("name"));
        if (updates.containsKey("email")) student.setEmail((String) updates.get("email"));
        if (updates.containsKey("grade")) student.setGrade((String) updates.get("grade"));
        String pwd = (String) updates.get("password");
        if (pwd != null && !pwd.isBlank())
            student.setPassword(passwordEncoder.encode(pwd));
        return Optional.of(userRepository.save(student));
    }

    /** Teacher only: delete a student. */
    @Transactional
    public boolean deleteStudent(Long studentId, SchoolUserDetails currentUser) {
        if (!currentUser.isTeacher()) return false;
        Optional<User> opt = userRepository.findById(studentId);
        if (opt.isEmpty() || opt.get().getRole() != Role.STUDENT) return false;
        userRepository.delete(opt.get());
        return true;
    }

    public Optional<SchoolUserDetails> getCurrentUserDetails() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof SchoolUserDetails)) {
            return Optional.empty();
        }
        return Optional.of((SchoolUserDetails) auth.getPrincipal());
    }
}
