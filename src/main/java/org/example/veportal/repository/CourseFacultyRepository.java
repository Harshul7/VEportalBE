package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.CourseFaculty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseFacultyRepository extends JpaRepository<CourseFaculty, CourseFaculty.CourseFacultyId> {

    List<CourseFaculty> findByCourseId(Long courseId);

    List<CourseFaculty> findByFacultyId(Long facultyId);

    boolean existsByCourseIdAndFacultyId(Long courseId, Long facultyId);

    void deleteByCourseId(Long courseId);

    void deleteByCourseIdAndFacultyId(Long courseId, Long facultyId);
}