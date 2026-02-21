package com.example.school.service;

import com.example.school.entity.Role;
import com.example.school.entity.User;
import com.example.school.repository.UserRepository;
import com.example.school.security.SchoolUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User teacher;
    private User student;
    private SchoolUserDetails teacherDetails;
    private SchoolUserDetails studentDetails;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher1");
        teacher.setRole(Role.TEACHER);
        teacher.setName("Teacher One");
        teacherDetails = new SchoolUserDetails(teacher);

        student = new User();
        student.setId(2L);
        student.setUsername("student1");
        student.setRole(Role.STUDENT);
        student.setName("Student One");
        studentDetails = new SchoolUserDetails(student);
    }

    @Nested
    @DisplayName("findById")
    class FindById {
        @Test
        @DisplayName("returns user when found")
        void returnsUserWhenFound() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            assertThat(userService.findById(2L)).contains(student);
        }

        @Test
        @DisplayName("returns empty when not found")
        void returnsEmptyWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            assertThat(userService.findById(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllStudents")
    class FindAllStudents {
        @Test
        @DisplayName("returns all students from repository")
        void returnsAllStudents() {
            when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(student));
            assertThat(userService.findAllStudents()).containsExactly(student);
        }
    }

    @Nested
    @DisplayName("findStudentsForCurrentUser")
    class FindStudentsForCurrentUser {
        @Test
        @DisplayName("teacher sees all students")
        void teacherSeesAllStudents() {
            User s2 = new User();
            s2.setId(3L);
            s2.setRole(Role.STUDENT);
            when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(student, s2));
            List<User> result = userService.findStudentsForCurrentUser(teacherDetails);
            assertThat(result).hasSize(2);
            verify(userRepository).findByRole(Role.STUDENT);
        }

        @Test
        @DisplayName("student sees only themselves")
        void studentSeesOnlySelf() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            List<User> result = userService.findStudentsForCurrentUser(studentDetails);
            assertThat(result).containsExactly(student);
        }

        @Test
        @DisplayName("student not in DB sees empty list")
        void studentNotInDbSeesEmpty() {
            when(userRepository.findById(2L)).thenReturn(Optional.empty());
            List<User> result = userService.findStudentsForCurrentUser(studentDetails);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createStudent")
    class CreateStudent {
        @Test
        @DisplayName("teacher can create student")
        void teacherCanCreateStudent() {
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            User saved = new User();
            saved.setId(10L);
            saved.setUsername("newuser");
            saved.setRole(Role.STUDENT);
            when(userRepository.save(any(User.class))).thenReturn(saved);

            Map<String, Object> body = Map.of(
                    "username", "newuser",
                    "password", "secret",
                    "name", "New User"
            );
            Optional<User> result = userService.createStudent(body, teacherDetails);

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("newuser");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("non-teacher cannot create student")
        void nonTeacherCannotCreateStudent() {
            Optional<User> result = userService.createStudent(
                    Map.of("username", "x", "password", "y"),
                    studentDetails
            );
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("duplicate username returns empty")
        void duplicateUsernameReturnsEmpty() {
            when(userRepository.findByUsername("existing")).thenReturn(Optional.of(student));
            Optional<User> result = userService.createStudent(
                    Map.of("username", "existing", "password", "p"),
                    teacherDetails
            );
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("blank username returns empty")
        void blankUsernameReturnsEmpty() {
            Optional<User> result = userService.createStudent(
                    Map.of("username", "   ", "password", "p"),
                    teacherDetails
            );
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateStudentInfo")
    class UpdateStudentInfo {
        @Test
        @DisplayName("teacher can update student name")
        void teacherCanUpdateStudent() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(userRepository.save(any(User.class))).thenReturn(student);
            Map<String, Object> updates = Map.of("name", "Updated Name");
            Optional<User> result = userService.updateStudentInfo(2L, updates, teacherDetails);
            assertThat(result).isPresent();
            assertThat(student.getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("non-teacher cannot update")
        void nonTeacherCannotUpdate() {
            Optional<User> result = userService.updateStudentInfo(2L, Map.of("name", "X"), studentDetails);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when student not found")
        void returnsEmptyWhenStudentNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            Optional<User> result = userService.updateStudentInfo(999L, Map.of("name", "X"), teacherDetails);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteStudent")
    class DeleteStudent {
        @Test
        @DisplayName("teacher can delete student")
        void teacherCanDeleteStudent() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            boolean result = userService.deleteStudent(2L, teacherDetails);
            assertThat(result).isTrue();
            verify(userRepository).delete(student);
        }

        @Test
        @DisplayName("student cannot delete")
        void studentCannotDelete() {
            boolean result = userService.deleteStudent(2L, studentDetails);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns false when student not found")
        void returnsFalseWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());
            boolean result = userService.deleteStudent(999L, teacherDetails);
            assertThat(result).isFalse();
        }
    }
}
