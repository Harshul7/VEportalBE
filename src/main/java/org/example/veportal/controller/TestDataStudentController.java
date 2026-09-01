package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.request.TestDataStudentRequest;
import org.example.veportal.dto.response.TestDataStudentFiltersResponse;
import org.example.veportal.dto.response.TestDataStudentResponse;
import org.example.veportal.service.TestDataStudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/students")
public class TestDataStudentController {

    private final TestDataStudentService service;

    public TestDataStudentController(TestDataStudentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TestDataStudentResponse>>> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "programme", required = false) String programme,
            @RequestParam(name = "batch", required = false) String batch,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        PagedResult<TestDataStudentResponse> result =
                service.list(search, programme, batch, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.content(), "Test students retrieved",
                result.pagination()));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<TestDataStudentFiltersResponse>> filters() {
        return ResponseEntity.ok(ApiResponse.success(service.filters(), "Test student filters retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestDataStudentResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.get(id), "Test student retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TestDataStudentResponse>> create(
            @Valid @RequestBody TestDataStudentRequest request) {
        TestDataStudentResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Test student created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestDataStudentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TestDataStudentRequest request) {
        TestDataStudentResponse response = service.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Test student updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Test student deleted"));
    }
}
