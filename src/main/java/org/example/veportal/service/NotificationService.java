package org.example.veportal.service;

import java.util.List;
import org.example.veportal.dto.response.NotificationResponse;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.NotificationRepository;
import org.example.veportal.util.RelativeTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final int MAX_NOTIFICATIONS = 30;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listFor(UserAccount user) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, MAX_NOTIFICATIONS))
                .stream()
                .map(notification -> new NotificationResponse(
                        notification.getId().toString(),
                        notification.getTitle(),
                        notification.getBody(),
                        RelativeTime.format(notification.getCreatedAt()),
                        notification.isRead(),
                        notification.getLinkUrl()
                ))
                .toList();
    }

    @Transactional
    public void markAllRead(UserAccount user) {
        notificationRepository.markAllRead(user.getId());
    }
}
