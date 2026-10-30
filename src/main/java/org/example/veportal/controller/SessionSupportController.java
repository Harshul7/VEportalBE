package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.request.SessionSupportRequest;
import org.example.veportal.dto.response.SessionSupportResponse;
import org.example.veportal.dto.response.SupportStaffResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.SessionSupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/support")
public class SessionSupportController {
    private final SessionSupportService service;
    private final AuthenticatedUserProvider users;

    public SessionSupportController(SessionSupportService service, AuthenticatedUserProvider users) { this.service = service; this.users = users; }

    @GetMapping
    @PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId)")
    public ResponseEntity<ApiResponse<List<SessionSupportResponse>>> list(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(service.list(sessionId), "Session support retrieved"));
    }

    @GetMapping("/staff")
    @PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId)")
    public ResponseEntity<ApiResponse<List<SupportStaffResponse>>> staff(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(service.staff(sessionId, users.currentUser()), "Teaching Assistants retrieved"));
    }

    @PostMapping
    @PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId) and hasAnyRole('FACULTY_COORDINATOR','ADMIN','GROUP_MENTOR','FACULTY')")
    public ResponseEntity<ApiResponse<SessionSupportResponse>> assign(@PathVariable Long sessionId, @Valid @RequestBody SessionSupportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.assign(sessionId, request, users.currentUser()), "Teaching Assistant assigned"));
    }

    @DeleteMapping("/{teachingAssistantId}")
    @PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId) and hasAnyRole('FACULTY_COORDINATOR','ADMIN','GROUP_MENTOR','FACULTY')")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long sessionId, @PathVariable Long teachingAssistantId) {
        service.remove(sessionId, teachingAssistantId, users.currentUser());
        return ResponseEntity.ok(ApiResponse.success(null, "Teaching Assistant unassigned"));
    }
}
