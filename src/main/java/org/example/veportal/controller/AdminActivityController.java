package org.example.veportal.controller;

import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.response.AdminActivityResponse;
import org.example.veportal.service.AdminActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/activity")
@PreAuthorize("hasAnyRole('FACULTY_COORDINATOR','ADMIN')")
public class AdminActivityController {
    private final AdminActivityService activityService;

    public AdminActivityController(AdminActivityService activityService) { this.activityService = activityService; }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<AdminActivityResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String groupName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResult<AdminActivityResponse> result = activityService.list(search, kind, groupName, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.content(), "Activity retrieved", result.pagination()));
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<java.util.List<String>>> groups() {
        return ResponseEntity.ok(ApiResponse.success(activityService.groups(), "Activity groups retrieved"));
    }
}
