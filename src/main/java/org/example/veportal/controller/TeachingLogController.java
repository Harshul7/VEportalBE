package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.request.TeachingLogSaveRequest;
import org.example.veportal.dto.response.TeachingLogHistoryItemResponse;
import org.example.veportal.dto.response.TeachingLogResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.TeachingLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TeachingLogController {

    private final TeachingLogService teachingLogService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public TeachingLogController(TeachingLogService teachingLogService,
                                 AuthenticatedUserProvider authenticatedUserProvider) {
        this.teachingLogService = teachingLogService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/sessions/{sessionId}/teaching-log")
    public ResponseEntity<ApiResponse<TeachingLogResponse>> forSession(@PathVariable Long sessionId) {
        TeachingLogResponse response = teachingLogService.forSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Teaching log retrieved"));
    }

    @PutMapping("/sessions/{sessionId}/teaching-log")
    public ResponseEntity<ApiResponse<TeachingLogResponse>> upsert(
            @PathVariable Long sessionId,
            @Valid @RequestBody TeachingLogSaveRequest request) {
        TeachingLogResponse response =
                teachingLogService.upsert(sessionId, request, authenticatedUserProvider.currentUser());
        return ResponseEntity.ok(ApiResponse.success(response, "Teaching log updated"));
    }

    @GetMapping("/teaching-logs")
    public ResponseEntity<ApiResponse<List<TeachingLogHistoryItemResponse>>> history(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PagedResult<TeachingLogHistoryItemResponse> result = teachingLogService.history(page, size);
        return ResponseEntity.ok(ApiResponse.success(result.content(), "Teaching logs retrieved",
                result.pagination()));
    }
}
