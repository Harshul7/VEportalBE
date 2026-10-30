package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.TaskDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDocumentRepository extends JpaRepository<TaskDocument, Long> {
    List<TaskDocument> findByTaskIdOrderByUploadedAtDesc(Long taskId);
}
