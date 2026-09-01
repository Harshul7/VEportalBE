package org.example.veportal.repository;

import java.util.Collection;
import java.util.List;
import org.example.veportal.entity.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long>, JpaSpecificationExecutor<CourseMaterial> {

    List<CourseMaterial> findAllByOrderByIdAsc();

    @Query("""
            select m from CourseMaterial m
            join fetch m.uploadedBy u
            left join fetch m.session
            order by m.id asc
            """)
    List<CourseMaterial> findAllWithUploaderOrderByIdAsc();

    @Query("""
            select m.session.id as sessionId, count(m) as materialCount
            from CourseMaterial m
            where m.session.id in :sessionIds
            group by m.session.id
            """)
    List<SessionMaterialAggregation> countBySessionIds(@Param("sessionIds") Collection<Long> sessionIds);

    interface SessionMaterialAggregation {
        Long getSessionId();

        long getMaterialCount();
    }
}
