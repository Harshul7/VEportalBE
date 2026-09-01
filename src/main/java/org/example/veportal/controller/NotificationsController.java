package org.example.veportal.controller;

import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.response.NotificationResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {

    private final NotificationService notificationService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public NotificationsController(NotificationService notificationService,
                                   AuthenticatedUserProvider authenticatedUserProvider) {
        this.notificationService = notificationService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list() {
        List<NotificationResponse> notifications =
                notificationService.listFor(authenticatedUserProvider.currentUser());
        return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications retrieved"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead(authenticatedUserProvider.currentUser());
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
}
