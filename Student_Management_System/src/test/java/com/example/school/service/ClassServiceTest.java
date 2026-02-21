package com.example.school.service;

import com.example.school.entity.*;
import com.example.school.repository.EnrollmentRepository;
import com.example.school.repository.SchoolClassRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClassService classService;

    private User teacher;
    private User student;
    private SchoolClass schoolClass;
    private SchoolUserDetails teacherDetails;
    private SchoolUserDetails studentDetails;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher1");
        teacher.setRole(Role.TEACHER);
        teacher.setName("Teacher");
        teacherDetails = new SchoolUserDetails(teacher);

        student = new User();
        student.setId(2L);
        student.setUsername("student1");
        student.setRole(Role.STUDENT);
        student.setName("Student");
        studentDetails = new SchoolUserDetails(student);

        schoolClass = new SchoolClass();
        schoolClass.setId(10L);
        schoolClass.setName("Math 101");
        schoolClass.setDescription("Algebra");
        schoolClass.setTeacher(teacher);
    }

    @Nested
    @DisplayName("findClassesForCurrentUser")
    class FindClassesForCurrentUser {
        @Test
        @DisplayName("teacher gets their classes")
        void teacherGetsTheirClasses() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
            when(schoolClassRepository.findByTeacherOrderByName(teacher)).thenReturn(List.of(schoolClass));
            List<SchoolClass> result = classService.findClassesForCurrentUser(teacherDetails);
            assertThat(result).containsExactly(schoolClass);
        }

        @Test
        @DisplayName("student gets all classes")
        void studentGetsAllClasses() {
            when(schoolClassRepository.findAllByOrderByName()).thenReturn(List.of(schoolClass));
            List<SchoolClass> result = classService.findClassesForCurrentUser(studentDetails);
            assertThat(result).containsExactly(schoolClass);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {
        @Test
        @DisplayName("returns class when found")
        void returnsClassWhenFound() {
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            assertThat(classService.findById(10L)).contains(schoolClass);
        }

        @Test
        @DisplayName("returns empty when not found")
        void returnsEmptyWhenNotFound() {
            when(schoolClassRepository.findById(999L)).thenReturn(Optional.empty());
            assertThat(classService.findById(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("createClass")
    class CreateClass {
        @Test
        @DisplayName("teacher can create class")
        void teacherCanCreateClass() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
            when(schoolClassRepository.save(any(SchoolClass.class))).thenReturn(schoolClass);
            Map<String, Object> body = Map.of("name", "Math 101", "description", "Algebra");
            Optional<SchoolClass> result = classService.createClass(body, teacherDetails);
            assertThat(result).isPresent();
            verify(schoolClassRepository).save(any(SchoolClass.class));
        }

        @Test
        @DisplayName("non-teacher cannot create class")
        void nonTeacherCannotCreateClass() {
            Optional<SchoolClass> result = classService.createClass(
                    Map.of("name", "Math", "description", "x"),
                    studentDetails
            );
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("blank name returns empty")
        void blankNameReturnsEmpty() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
            Optional<SchoolClass> result = classService.createClass(
                    Map.of("name", "   ", "description", "x"),
                    teacherDetails
            );
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateClass")
    class UpdateClass {
        @Test
        @DisplayName("teacher can update own class")
        void teacherCanUpdateOwnClass() {
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            when(schoolClassRepository.save(any(SchoolClass.class))).thenReturn(schoolClass);
            Map<String, Object> updates = Map.of("name", "Updated Math", "description", "New desc");
            Optional<SchoolClass> result = classService.updateClass(10L, updates, teacherDetails);
            assertThat(result).isPresent();
            assertThat(schoolClass.getName()).isEqualTo("Updated Math");
        }

        @Test
        @DisplayName("returns empty when class not owned by teacher")
        void returnsEmptyWhenNotOwned() {
            User otherTeacher = new User();
            otherTeacher.setId(99L);
            schoolClass.setTeacher(otherTeacher);
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            Optional<SchoolClass> result = classService.updateClass(10L, Map.of("name", "x"), teacherDetails);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteClass")
    class DeleteClass {
        @Test
        @DisplayName("teacher can delete own class")
        void teacherCanDeleteOwnClass() {
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            boolean result = classService.deleteClass(10L, teacherDetails);
            assertThat(result).isTrue();
            verify(schoolClassRepository).delete(schoolClass);
        }

        @Test
        @DisplayName("returns false when not owner")
        void returnsFalseWhenNotOwner() {
            User other = new User();
            other.setId(99L);
            schoolClass.setTeacher(other);
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            boolean result = classService.deleteClass(10L, teacherDetails);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("enroll")
    class Enroll {
        @Test
        @DisplayName("student can enroll")
        void studentCanEnroll() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            when(enrollmentRepository.existsByStudentAndSchoolClass(student, schoolClass)).thenReturn(false);
            when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));
            boolean result = classService.enroll(10L, studentDetails);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("teacher cannot enroll")
        void teacherCannotEnroll() {
            boolean result = classService.enroll(10L, teacherDetails);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("already enrolled returns true")
        void alreadyEnrolledReturnsTrue() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            when(enrollmentRepository.existsByStudentAndSchoolClass(student, schoolClass)).thenReturn(true);
            boolean result = classService.enroll(10L, studentDetails);
            assertThat(result).isTrue();
            verify(enrollmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unenroll")
    class Unenroll {
        @Test
        @DisplayName("student can unenroll")
        void studentCanUnenroll() {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setSchoolClass(schoolClass);
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            when(enrollmentRepository.findByStudentAndSchoolClass(student, schoolClass))
                    .thenReturn(Optional.of(enrollment));
            boolean result = classService.unenroll(10L, studentDetails);
            assertThat(result).isTrue();
            verify(enrollmentRepository).delete(enrollment);
        }

        @Test
        @DisplayName("teacher cannot unenroll")
        void teacherCannotUnenroll() {
            boolean result = classService.unenroll(10L, teacherDetails);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("enrolledClassIdsForStudent")
    class EnrolledClassIdsForStudent {
        @Test
        @DisplayName("returns enrolled class ids for student")
        void returnsEnrolledClassIds() {
            Enrollment e = new Enrollment();
            e.setSchoolClass(schoolClass);
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(enrollmentRepository.findByStudentOrderBySchoolClass_Name(student)).thenReturn(List.of(e));
            Set<Long> result = classService.enrolledClassIdsForStudent(studentDetails);
            assertThat(result).containsExactly(10L);
        }

        @Test
        @DisplayName("teacher gets empty set")
        void teacherGetsEmptySet() {
            Set<Long> result = classService.enrolledClassIdsForStudent(teacherDetails);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("enrollmentCountByClassId")
    class EnrollmentCountByClassId {
        @Test
        @DisplayName("returns count per class")
        void returnsCountPerClass() {
            when(enrollmentRepository.findBySchoolClass(schoolClass)).thenReturn(List.of(new Enrollment(), new Enrollment()));
            Map<Long, Long> result = classService.enrollmentCountByClassId(List.of(schoolClass));
            assertThat(result).containsEntry(10L, 2L);
        }
    }

    @Nested
    @DisplayName("findEnrolledStudentsByClassId")
    class FindEnrolledStudentsByClassId {
        @Test
        @DisplayName("teacher gets enrolled students")
        void teacherGetsEnrolledStudents() {
            Enrollment e = new Enrollment();
            e.setStudent(student);
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            when(enrollmentRepository.findBySchoolClassWithStudents(schoolClass)).thenReturn(List.of(e));
            List<User> result = classService.findEnrolledStudentsByClassId(10L, teacherDetails);
            assertThat(result).containsExactly(student);
        }

        @Test
        @DisplayName("non-teacher gets empty list")
        void nonTeacherGetsEmptyList() {
            List<User> result = classService.findEnrolledStudentsByClassId(10L, studentDetails);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeStudentFromClass")
    class RemoveStudentFromClass {
        @Test
        @DisplayName("teacher can remove student from class")
        void teacherCanRemoveStudent() {
            Enrollment enrollment = new Enrollment();
            when(schoolClassRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
            when(userRepository.findById(2L)).thenReturn(Optional.of(student));
            when(enrollmentRepository.findByStudentAndSchoolClass(student, schoolClass))
                    .thenReturn(Optional.of(enrollment));
            boolean result = classService.removeStudentFromClass(10L, 2L, teacherDetails);
            assertThat(result).isTrue();
            verify(enrollmentRepository).delete(enrollment);
        }

        @Test
        @DisplayName("non-teacher cannot remove")
        void nonTeacherCannotRemove() {
            boolean result = classService.removeStudentFromClass(10L, 2L, studentDetails);
            assertThat(result).isFalse();
        }
    }
}
