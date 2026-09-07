package org.example.veportal.repository;

import java.util.Optional;
import org.example.veportal.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findByName(String name);

    Optional<AcademicYear> findByCurrentTrue();
}