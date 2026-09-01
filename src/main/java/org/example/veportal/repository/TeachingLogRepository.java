package org.example.veportal.repository;

import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.TeachingLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeachingLogRepository extends JpaRepository<TeachingLog, Long> {

    @Query("""
            select l from TeachingLog l
            join fetch l.session s
            left join fetch l.concepts
            where l.session.id = :sessionId
            """)
    Optional<TeachingLog> findWithConceptsBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
            select l from TeachingLog l
            join fetch l.session s
            order by s.sessionNumber desc
            """)
    List<TeachingLog> findAllOrderBySessionNumberDesc();

    @Query(value = """
            select l from TeachingLog l
            join fetch l.session s
            order by s.sessionNumber desc
            """,
            countQuery = "select count(l) from TeachingLog l")
    Page<TeachingLog> findPageOrderBySessionNumberDesc(Pageable pageable);

    @Query("""
            select l.session.id from TeachingLog l
            where l.session.id in :sessionIds
            """)
    List<Long> findSessionIdsWithLogs(@Param("sessionIds") List<Long> sessionIds);
}
