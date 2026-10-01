package org.example.veportal.repository;

import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.MasterCourseTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterCourseTemplateRepository extends JpaRepository<MasterCourseTemplate, Long> {
    Optional<MasterCourseTemplate> findByCode(String code);
    List<MasterCourseTemplate> findByStatusOrderBySemesterAscCodeAsc(String status);
}
