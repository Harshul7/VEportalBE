package org.example.veportal.service;

import org.example.veportal.entity.ActivityLog;
import org.example.veportal.entity.Notification;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.ActivityLogRepository;
import org.example.veportal.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityLogRepository activityLogRepository;
    private final NotificationRepository notificationRepository;

    public ActivityService(ActivityLogRepository activityLogRepository,
                           NotificationRepository notificationRepository) {
        this.activityLogRepository = activityLogRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void record(UserAccount actor, org.example.veportal.entity.ActivityKind kind,
                       String title, String contextText, String href) {
        ActivityLog log = new ActivityLog();
        log.setUser(actor);
        log.setKind(kind);
        log.setTitle(title);
        log.setContextText(contextText);
        log.setHref(href);
        activityLogRepository.save(log);
    }

    @Transactional
    public void notifyUser(UserAccount recipient, String title, String body, String href) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLinkUrl(href);
        notification.setReadAt(null);
        notificationRepository.save(notification);
    }
}
