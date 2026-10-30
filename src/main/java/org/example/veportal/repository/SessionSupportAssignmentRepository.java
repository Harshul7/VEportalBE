package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.SessionSupportAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionSupportAssignmentRepository extends JpaRepository<SessionSupportAssignment, Long> {
    List<SessionSupportAssignment> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    boolean existsBySessionIdAndTeachingAssistantId(Long sessionId, Long teachingAssistantId);
    void deleteBySessionIdAndTeachingAssistantId(Long sessionId, Long teachingAssistantId);
}
