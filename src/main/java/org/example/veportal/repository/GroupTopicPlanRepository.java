package org.example.veportal.repository;
import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.GroupTopicPlan;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GroupTopicPlanRepository extends JpaRepository<GroupTopicPlan, Long> {
    List<GroupTopicPlan> findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(Long groupId);
    Optional<GroupTopicPlan> findByGroupIdAndChapterId(Long groupId, Long chapterId);
    void deleteByGroupIdAndChapterId(Long groupId, Long chapterId);
    void deleteByGroupId(Long groupId);
}
