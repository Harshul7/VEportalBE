package org.example.veportal.repository;
import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.AcademicSemester;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AcademicSemesterRepository extends JpaRepository<AcademicSemester, Long> {
    List<AcademicSemester> findByAcademicYearIdAndStatusOrderByNameAsc(Long academicYearId, String status);
    Optional<AcademicSemester> findByAcademicYearIdAndNameIgnoreCase(Long academicYearId, String name);
}
