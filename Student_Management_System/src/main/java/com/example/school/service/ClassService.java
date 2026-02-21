package com.example.school.service;

import com.example.school.entity.*;
import com.example.school.repository.EnrollmentRepository;
import com.example.school.repository.SchoolClassRepository;
import com.example.school.repository.UserRepository;
import com.example.school.security.SchoolUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public ClassService(SchoolClassRepository schoolClassRepository,
                        EnrollmentRepository enrollmentRepository,
                        UserRepository userRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    /** Teacher: their classes. Student: all classes (with enrolled flag). */
    public List<SchoolClass> findClassesForCurrentUser(SchoolUserDetails currentUser) {
        if (currentUser.isTeacher()) {
            return userRepository.findById(currentUser.getUserId())
                    .map(schoolClassRepository::findByTeacherOrderByName)
                    .orElse(List.of());
        }
        return schoolClassRepository.findAllByOrderByName();
    }

    public Optional<SchoolClass> findById(Long id) {
        return schoolClassRepository.findById(id);
    }

    /** Teacher only: create a class. */
    @Transactional
    public Optional<SchoolClass> createClass(Map<String, Object> body, SchoolUserDetails currentUser) {
        if (!currentUser.isTeacher()) return Optional.empty();
        Optional<User> teacher = userRepository.findById(currentUser.getUserId());
        if (teacher.isEmpty()) return Optional.empty();
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) return Optional.empty();
        SchoolClass c = new SchoolClass();
        c.setName(name.trim());
        c.setDescription((String) body.get("description"));
        c.setTeacher(teacher.get());
        return Optional.of(schoolClassRepository.save(c));
    }

    /** Teacher only: update a class they own. */
    @Transactional
    public Optional<SchoolClass> updateClass(Long id, Map<String, Object> updates, SchoolUserDetails currentUser) {
        if (!currentUser.isTeacher()) return Optional.empty();
        Optional<SchoolClass> opt = schoolClassRepository.findById(id);
        if (opt.isEmpty() || !opt.get().getTeacher().getId().equals(currentUser.getUserId())) return Optional.empty();
        SchoolClass c = opt.get();
        if (updates.containsKey("name")) {
            String name = (String) updates.get("name");
            if (name != null && !name.isBlank()) c.setName(name.trim());
        }
        if (updates.containsKey("description")) c.setDescription((String) updates.get("description"));
        return Optional.of(schoolClassRepository.save(c));
    }

    /** Teacher only: delete a class they own. */
    @Transactional
    public boolean deleteClass(Long id, SchoolUserDetails currentUser) {
        if (!currentUser.isTeacher()) return false;
        Optional<SchoolClass> opt = schoolClassRepository.findById(id);
        if (opt.isEmpty() || !opt.get().getTeacher().getId().equals(currentUser.getUserId())) return false;
        schoolClassRepository.delete(opt.get());
        return true;
    }

    /** Student only: enroll in a class. */
    @Transactional
    public boolean enroll(Long classId, SchoolUserDetails currentUser) {
        if (currentUser.isTeacher()) return false;
        Optional<User> student = userRepository.findById(currentUser.getUserId());
        if (student.isEmpty() || student.get().getRole() != Role.STUDENT) return false;
        Optional<SchoolClass> schoolClass = schoolClassRepository.findById(classId);
        if (schoolClass.isEmpty()) return false;
        if (enrollmentRepository.existsByStudentAndSchoolClass(student.get(), schoolClass.get())) return true; // already enrolled
        Enrollment e = new Enrollment();
        e.setStudent(student.get());
        e.setSchoolClass(schoolClass.get());
        enrollmentRepository.save(e);
        return true;
    }

    /** Student only: unenroll from a class. */
    @Transactional
    public boolean unenroll(Long classId, SchoolUserDetails currentUser) {
        if (currentUser.isTeacher()) return false;
        Optional<User> student = userRepository.findById(currentUser.getUserId());
        if (student.isEmpty()) return false;
        Optional<SchoolClass> schoolClass = schoolClassRepository.findById(classId);
        if (schoolClass.isEmpty()) return false;
        return enrollmentRepository.findByStudentAndSchoolClass(student.get(), schoolClass.get())
                .map(enrollment -> {
                    enrollmentRepository.delete(enrollment);
                    return true;
                })
                .orElse(false);
    }

    /** For student: set of class IDs they are enrolled in. */
    public Set<Long> enrolledClassIdsForStudent(SchoolUserDetails currentUser) {
        if (currentUser.isTeacher()) return Set.of();
        return userRepository.findById(currentUser.getUserId())
                .map(enrollmentRepository::findByStudentOrderBySchoolClass_Name)
                .orElse(List.of())
                .stream()
                .map(e -> e.getSchoolClass().getId())
                .collect(Collectors.toSet());
    }

    /** For teacher: count of enrollments per class (by class id). */
    public Map<Long, Long> enrollmentCountByClassId(List<SchoolClass> classes) {
        return classes.stream()
                .collect(Collectors.toMap(SchoolClass::getId, c -> (long) enrollmentRepository.findBySchoolClass(c).size()));
    }

    /** Teacher only: list students enrolled in a class they own. */
    @Transactional(readOnly = true)
    public List<User> findEnrolledStudentsByClassId(Long classId, SchoolUserDetails currentUser) {
        if (!currentUser.isTeacher()) return List.of();
        Optional<SchoolClass> c = schoolClassRepository.findById(classId);
        if (c.isEmpty() || !c.get().getTeacher().getId().equals(currentUser.getUserId())) return List.of();
        return enrollmentRepository.findBySchoolClassWithStudents(c.get()).stream()
                .map(Enrollment::getStudent)
                .collect(Collectors.toList());
    }

    /** Teacher only: remove a student from a class they own. */
    @Transactional
    public boolean removeStudentFromClass(Long classId, Long studentId, SchoolUserDetails currentUser) {
        if (!currentUser.isTeacher()) return false;
        Optional<SchoolClass> schoolClass = schoolClassRepository.findById(classId);
        if (schoolClass.isEmpty() || !schoolClass.get().getTeacher().getId().equals(currentUser.getUserId())) return false;
        Optional<User> student = userRepository.findById(studentId);
        if (student.isEmpty() || student.get().getRole() != Role.STUDENT) return false;
        return enrollmentRepository.findByStudentAndSchoolClass(student.get(), schoolClass.get())
                .map(enrollment -> {
                    enrollmentRepository.delete(enrollment);
                    return true;
                })
                .orElse(false);
    }
}
