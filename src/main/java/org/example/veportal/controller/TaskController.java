package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.request.TaskCreateRequest;
import org.example.veportal.dto.response.TaskResponse;
import org.example.veportal.entity.TaskDocument;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.TaskService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sessions/{sessionId}/tasks")
@PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId)")
public class TaskController {
    private final TaskService taskService;
    private final AuthenticatedUserProvider users;

    public TaskController(TaskService taskService, AuthenticatedUserProvider users) {
        this.taskService = taskService;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> list(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.list(sessionId), "Session tasks retrieved"));
    }

    @PostMapping
    @PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId) and hasAnyRole('FACULTY_COORDINATOR','ADMIN','GROUP_MENTOR','FACULTY')")
    public ResponseEntity<ApiResponse<TaskResponse>> create(@PathVariable Long sessionId,
                                                             @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.create(sessionId, request), "Task created"));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId) and hasAnyRole('FACULTY_COORDINATOR','ADMIN','GROUP_MENTOR','FACULTY')")
    public ResponseEntity<ApiResponse<TaskResponse>> update(@PathVariable Long sessionId, @PathVariable Long taskId,
                                                             @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.update(sessionId, taskId, request), "Task updated"));
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("@resourceAuthorization.canAccessSession(#sessionId) and hasAnyRole('FACULTY_COORDINATOR','ADMIN','GROUP_MENTOR','FACULTY')")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long sessionId, @PathVariable Long taskId) {
        taskService.archive(sessionId, taskId);
        return ResponseEntity.ok(ApiResponse.success(null, "Task archived"));
    }

    @PostMapping(value = "/{taskId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TaskResponse>> upload(@PathVariable Long sessionId, @PathVariable Long taskId,
                                                              @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(taskService.upload(sessionId, taskId, file, users.currentUser()),
                "Task document uploaded"));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long sessionId, @PathVariable Long documentId) {
        TaskDocument document = taskService.requireDocument(documentId);
        if (!document.getTask().getSession().getId().equals(sessionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName().replace("\"", "") + "\"")
                .header(HttpHeaders.CONTENT_TYPE, document.getContentType() == null ? "application/octet-stream" : document.getContentType())
                .body(new FileSystemResource(taskService.storedPath(document)));
    }

    @DeleteMapping("/{taskId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long sessionId, @PathVariable Long taskId,
                                                             @PathVariable Long documentId) {
        taskService.deleteDocument(sessionId, taskId, documentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Task document removed"));
    }
}
