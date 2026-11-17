package org.example.veportal.repository;
import java.util.List;
import org.example.veportal.entity.GroupTransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GroupTransferHistoryRepository extends JpaRepository<GroupTransferHistory, Long> {
    List<GroupTransferHistory> findByStudentIdOrderByTransferredAtDesc(Long studentId);
}
