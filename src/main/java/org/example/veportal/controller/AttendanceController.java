package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.request.AttendanceSaveRequest;
import org.example.veportal.dto.response.AttendanceRecordResponse;
import org.example.veportal.dto.response.RosterRowResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AttendanceController(AttendanceService attendanceService,
                                AuthenticatedUserProvider authenticatedUserProvider) {
        this.attendanceService = attendanceService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RosterRowResponse>>> roster(
            @PathVariable Long sessionId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false, defaultValue = "name") String sort) {
        List<RosterRowResponse> roster = attendanceService.roster(sessionId, search, status, sort);
        return ResponseEntity.ok(ApiResponse.success(roster, "Attendance roster retrieved"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> save(
            @PathVariable Long sessionId,
            @Valid @RequestBody AttendanceSaveRequest request) {
        List<AttendanceRecordResponse> saved =
                attendanceService.save(sessionId, request, authenticatedUserProvider.currentUser());
        return ResponseEntity.ok(ApiResponse.success(saved, "Attendance saved successfully"));
    }
}
