package org.example.veportal.controller;

import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.response.SupportStaffResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.SessionSupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session-support")
public class SupportStaffController {
    private final SessionSupportService service;
    private final AuthenticatedUserProvider users;
    public SupportStaffController(SessionSupportService service, AuthenticatedUserProvider users) { this.service = service; this.users = users; }

    @GetMapping("/sessions/{sessionId}/staff")
    @PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId)")
    public ResponseEntity<ApiResponse<List<SupportStaffResponse>>> staff(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(service.staff(sessionId, users.currentUser()), "Teaching Assistants retrieved"));
    }
}
