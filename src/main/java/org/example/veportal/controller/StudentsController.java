package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.request.StudentCreateRequest;
import org.example.veportal.dto.request.StudentUpdateRequest;
import org.example.veportal.dto.response.StudentFiltersResponse;
import org.example.veportal.dto.response.StudentResponse;
import org.example.veportal.service.StudentService;
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
@RequestMapping("/api/students")
public class StudentsController {

    private final StudentService studentService;

    public StudentsController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "programme", required = false) String programme,
            @RequestParam(name = "batch", required = false) String batch,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        PagedResult<StudentResponse> result =
                studentService.list(search, programme, batch, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.content(), "Students retrieved",
                result.pagination()));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<StudentFiltersResponse>> filters() {
        StudentFiltersResponse response = studentService.filters();
        return ResponseEntity.ok(ApiResponse.success(response, "Student filters retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.get(id), "Student retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> create(
            @Valid @RequestBody StudentCreateRequest request) {
        StudentResponse response = studentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Student added to the directory"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        StudentResponse response = studentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Student record updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Student deleted"));
    }
}
