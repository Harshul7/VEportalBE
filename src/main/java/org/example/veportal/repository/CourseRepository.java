package org.example.veportal.repository;

import java.util.Optional;
import org.example.veportal.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);
}
