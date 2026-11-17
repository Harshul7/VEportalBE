package org.example.veportal.repository;
import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.StudentCourseResult;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentCourseResultRepository extends JpaRepository<StudentCourseResult, Long> {
    Optional<StudentCourseResult> findByStudentIdAndCourseId(Long studentId, Long courseId);
    List<StudentCourseResult> findByStudentId(Long studentId);
}
