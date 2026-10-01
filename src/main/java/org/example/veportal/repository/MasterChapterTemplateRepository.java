package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.MasterChapterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterChapterTemplateRepository extends JpaRepository<MasterChapterTemplate, Long> {
    List<MasterChapterTemplate> findByCourseTemplateIdAndStatusOrderByChapterNumberAsc(Long courseTemplateId, String status);
    long countByCourseTemplateIdAndStatus(Long courseTemplateId, String status);
}
