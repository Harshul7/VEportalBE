package org.example.veportal.repository;

import java.util.List;
import java.time.LocalDate;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long>, JpaSpecificationExecutor<ClassSession> {

    @Query("""
            select s from ClassSession s
            join fetch s.faculty
            where s.course.id = :courseId
            order by s.sessionNumber desc
            """)
    List<ClassSession> findAllWithFacultyByCourse(@Param("courseId") Long courseId);

    @Query("""
            select coalesce(max(s.sessionNumber), 0) from ClassSession s
            where s.course.id = :courseId
            """)
    int findMaxSessionNumber(@Param("courseId") Long courseId);

    boolean existsByCourseIdAndSessionNumber(Long courseId, Integer sessionNumber);

    long countByCourseIdAndStatus(Long courseId, SessionStatus status);

    long countByGroupId(Long groupId);

    long countByGroupIdAndStatus(Long groupId, SessionStatus status);

    long countByStatus(SessionStatus status);

    @Query(value = "select count(*) > 0 from class_sessions where session_date = :sessionDate and status <> 'CANCELLED' and (:sessionId is null or id <> :sessionId) and (group_id = :groupId or faculty_id = :facultyId) and start_time < :endTime and end_time > :startTime", nativeQuery = true)
    long existsScheduleConflict(@Param("groupId") Long groupId,
                                   @Param("facultyId") Long facultyId,
                                   @Param("sessionDate") LocalDate sessionDate,
                                   @Param("startTime") String startTime,
                                   @Param("endTime") String endTime,
                                   @Param("sessionId") Long sessionId);
}
