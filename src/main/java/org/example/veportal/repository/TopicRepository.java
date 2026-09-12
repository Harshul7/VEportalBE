package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByChapterIdAndStatusOrderByDisplayOrderAscIdAsc(Long chapterId, String status);
}
