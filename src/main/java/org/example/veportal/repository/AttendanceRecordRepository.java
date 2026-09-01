package org.example.veportal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.AttendanceRecord;
import org.example.veportal.entity.AttendanceStatus;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.Student;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    @Query("""
            select a from AttendanceRecord a
            join fetch a.student s
            where a.session.id = :sessionId
            """)
    List<AttendanceRecord> findWithStudentBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
            select a from AttendanceRecord a
            join fetch a.student s
            join fetch a.session ses
            """)
    List<AttendanceRecord> findAllWithStudentAndSession();

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    long countByStatus(AttendanceStatus status);

    @Query("""
            select count(a) from AttendanceRecord a
            where a.session.status = org.example.veportal.entity.SessionStatus.COMPLETED
            """)
    long countForCompletedSessions();

    @Query("""
            select count(a) from AttendanceRecord a
            join a.session s
            where s.status = org.example.veportal.entity.SessionStatus.COMPLETED and a.status = 'PRESENT'
            """)
    long countPresentForCompletedSessions();

    @Query("""
            select a.student.id as studentId,
                   count(a) as total,
                   sum(case when a.status = 'PRESENT' then 1 else 0 end) as present
            from AttendanceRecord a
            where a.student.id in :studentIds
            group by a.student.id
            """)
    List<StudentAttendanceAggregation> aggregateByStudents(@Param("studentIds") Collection<Long> studentIds);

    interface StudentAttendanceAggregation {
        Long getStudentId();

        long getTotal();

        long getPresent();
    }

    @Query("""
            select a.session.id as sessionId,
                   count(a) as total,
                   sum(case when a.status = 'PRESENT' then 1 else 0 end) as present
            from AttendanceRecord a
            where a.session.id in :sessionIds
            group by a.session.id
            """)
    List<SessionAttendanceAggregation> aggregateBySessions(@Param("sessionIds") Collection<Long> sessionIds);

    interface SessionAttendanceAggregation {
        Long getSessionId();

        long getTotal();

        long getPresent();
    }

    @Query("""
            select distinct a.session from AttendanceRecord a
            where a.student = :student and a.session.status = org.example.veportal.entity.SessionStatus.COMPLETED
            order by a.session.sessionNumber desc
            """)
    List<ClassSession> findCompletedSessionsForStudent(@Param("student") Student student, Pageable pageable);
}
