package org.example.veportal.repository;

import java.util.Collection;
import java.util.List;
import org.example.veportal.entity.TeachingLogConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeachingLogConceptRepository extends JpaRepository<TeachingLogConcept, Long> {

    @Query("""
            select c from TeachingLogConcept c
            where c.log.id in :logIds
            order by c.log.id asc, c.position asc
            """)
    List<TeachingLogConcept> findByLogIds(@Param("logIds") Collection<Long> logIds);
}
