package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.ActivityLog;
import org.example.veportal.entity.ActivityKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<ActivityLog> findByKindAndGroupNameAndCourseIdOrderByCreatedAtDesc(ActivityKind kind, String groupName, Long courseId);
}
