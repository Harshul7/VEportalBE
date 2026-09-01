package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("""
            update Notification n set n.readAt = CURRENT_TIMESTAMP
            where n.user.id = :userId and n.readAt is null
            """)
    int markAllRead(@Param("userId") Long userId);
}
