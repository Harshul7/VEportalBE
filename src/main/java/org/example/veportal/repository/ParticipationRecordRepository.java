package org.example.veportal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.ParticipationLevel;
import org.example.veportal.entity.ParticipationRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRecordRepository extends JpaRepository<ParticipationRecord, Long> {

    @Query("""
            select p from ParticipationRecord p
            join fetch p.student
            where p.session.id = :sessionId
            """)
    List<ParticipationRecord> findWithStudentBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
            select p from ParticipationRecord p
            join fetch p.student
            join fetch p.session ses
            order by p.id asc
            """)
    List<ParticipationRecord> findAllWithStudentAndSession();

    Optional<ParticipationRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    @Query("""
            select count(distinct p.session.id) from ParticipationRecord p
            where p.level <> org.example.veportal.entity.ParticipationLevel.NOT_RECORDED
            """)
    long countSessionsWithRecords();

    @Query("""
            select p from ParticipationRecord p
            join fetch p.session
            where p.student.id = :studentId
              and p.level <> org.example.veportal.entity.ParticipationLevel.NOT_RECORDED
            order by p.session.sessionNumber desc
            """)
    List<ParticipationRecord> findRecentForStudent(@Param("studentId") Long studentId, Pageable pageable);
}
