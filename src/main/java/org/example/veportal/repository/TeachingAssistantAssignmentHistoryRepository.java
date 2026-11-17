package org.example.veportal.repository;
import java.util.List;
import org.example.veportal.entity.TeachingAssistantAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TeachingAssistantAssignmentHistoryRepository extends JpaRepository<TeachingAssistantAssignmentHistory, Long> {
    List<TeachingAssistantAssignmentHistory> findByGroupIdOrderByAssignedAtDesc(Long groupId);
    long countByGroupId(Long groupId);
    List<TeachingAssistantAssignmentHistory> findByTeachingAssistantIdOrderByAssignedAtDesc(Long taId);
}
