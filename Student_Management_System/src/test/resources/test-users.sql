-- Test users for @WithUserDetails (run before each test so they exist before security listener).
-- BCrypt hash for "password" (strength 10).
DELETE FROM enrollments;
DELETE FROM users;
INSERT INTO users (username, password, name, role) VALUES
('auth_test_teacher', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Auth Test Teacher', 'TEACHER'),
('auth_test_student', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Auth Test Student', 'STUDENT'),
('test_teacher', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Test Teacher', 'TEACHER'),
('test_student', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Test Student', 'STUDENT');
