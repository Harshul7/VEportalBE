package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.request.SessionCreateRequest;
import org.example.veportal.dto.response.SessionListItemResponse;
import org.example.veportal.dto.response.SessionResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionsController {

    private final SessionService sessionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public SessionsController(SessionService sessionService,
                              AuthenticatedUserProvider authenticatedUserProvider) {
        this.sessionService = sessionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionListItemResponse>>> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PagedResult<SessionListItemResponse> result = sessionService.list(search, status, page, size,
                authenticatedUserProvider.currentUser());
        return ResponseEntity.ok(ApiResponse.success(result.content(), "Sessions retrieved", result.pagination()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> create(@Valid @RequestBody SessionCreateRequest request) {
        SessionResponse response = sessionService.create(request, authenticatedUserProvider.currentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Session created"));
    }

    @GetMapping("/{sessionId}")
    @org.springframework.security.access.prepost.PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId)")
    public ResponseEntity<ApiResponse<SessionResponse>> get(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.get(sessionId), "Session retrieved"));
    }

    @PatchMapping("/{sessionId}/status")
    @org.springframework.security.access.prepost.PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId)")
    public ResponseEntity<ApiResponse<SessionResponse>> updateStatus(@PathVariable Long sessionId,
                                                                       @RequestBody StatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.updateStatus(sessionId, request.status()), "Session status updated"));
    }

    public record StatusRequest(String status) {}
}
