package org.example.veportal.repository;

import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.CourseGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseGroupRepository extends JpaRepository<CourseGroup, Long> {
    List<CourseGroup> findByCourseIdOrderByNameAsc(Long courseId);
    Optional<CourseGroup> findByCourseIdAndNameIgnoreCase(Long courseId, String name);
}
