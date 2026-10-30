package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.SessionTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTaskRepository extends JpaRepository<SessionTask, Long> {
    List<SessionTask> findBySessionIdAndStatusOrderByDueDateAscIdAsc(Long sessionId, String status);
}
