package com.example.school.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.school.entity.Role;
import com.example.school.entity.User;
import com.example.school.repository.UserRepository;

@Component
//@Profile("docker")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("teacher").isEmpty()) {
            User teacher = new User();
            teacher.setUsername("teacher");
            teacher.setPassword(passwordEncoder.encode("teacher"));
            teacher.setName("Sumaiya Akter");
            teacher.setRole(Role.TEACHER);
            userRepository.save(teacher);
        }
        if (userRepository.findByUsername("student").isEmpty()) {
            User student = new User();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("student"));
            student.setName("Default Student");
            student.setEmail("student@school.com");
            student.setGrade("A");
            student.setRole(Role.STUDENT);
            userRepository.save(student);
        }
    }
}
