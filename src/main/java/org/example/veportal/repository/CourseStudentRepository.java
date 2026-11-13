package org.example.veportal.repository;

import java.util.List;
import java.util.Collection;
import org.example.veportal.entity.CourseStudent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseStudentRepository extends JpaRepository<CourseStudent, CourseStudent.CourseStudentId> {

    List<CourseStudent> findByCourseId(Long courseId);
    List<CourseStudent> findByGroupIdIn(Collection<Long> groupIds);

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    void deleteByCourseId(Long courseId);

    void deleteByCourseIdAndStudentId(Long courseId, Long studentId);
}
