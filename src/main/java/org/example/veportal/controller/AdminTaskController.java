package org.example.veportal.controller;

import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.response.AdminTaskResponse;
import org.example.veportal.service.AdminTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tasks")
@PreAuthorize("hasAnyRole('FACULTY_COORDINATOR','ADMIN')")
public class AdminTaskController {
    private final AdminTaskService taskService;

    public AdminTaskController(AdminTaskService taskService) { this.taskService = taskService; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminTaskResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        PagedResult<AdminTaskResponse> result = taskService.list(search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.content(), "Assigned tasks retrieved", result.pagination()));
    }
}
