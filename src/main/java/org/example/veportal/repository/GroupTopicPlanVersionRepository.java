package org.example.veportal.repository;
import java.util.List;
import org.example.veportal.entity.GroupTopicPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GroupTopicPlanVersionRepository extends JpaRepository<GroupTopicPlanVersion,Long>{ List<GroupTopicPlanVersion> findByGroupIdOrderByVersionNumberDesc(Long groupId); }
