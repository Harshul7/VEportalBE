package org.example.veportal.repository;
import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.CourseCodeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseCodeMasterRepository extends JpaRepository<CourseCodeMaster, Long> {
    List<CourseCodeMaster> findByStatusOrderByCodeAsc(String status);
    Optional<CourseCodeMaster> findByCodeIgnoreCase(String code);
}
