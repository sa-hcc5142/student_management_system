package com.example.school.repository;

import com.example.school.entity.Enrollment;
import com.example.school.entity.SchoolClass;
import com.example.school.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentOrderBySchoolClass_Name(User student);

    Optional<Enrollment> findByStudentAndSchoolClass(User student, SchoolClass schoolClass);

    boolean existsByStudentAndSchoolClass(User student, SchoolClass schoolClass);

    List<Enrollment> findBySchoolClass(SchoolClass schoolClass);

    /** Load enrollments with students in one query (avoids lazy load after transaction). */
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student WHERE e.schoolClass = :schoolClass")
    List<Enrollment> findBySchoolClassWithStudents(@Param("schoolClass") SchoolClass schoolClass);
}
