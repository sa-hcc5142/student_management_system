package com.example.school.repository;

import com.example.school.entity.SchoolClass;
import com.example.school.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findByTeacherOrderByName(User teacher);

    List<SchoolClass> findAllByOrderByName();
}
