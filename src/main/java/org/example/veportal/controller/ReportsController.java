package org.example.veportal.controller;

import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.entity.Student;
import org.example.veportal.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final StudentRepository studentRepository;

    public ReportsController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @GetMapping("/students/{id}/performance")
    public ResponseEntity<ApiResponse<StudentPerformanceReport>> studentPerformance(
            @PathVariable Long id,
            @RequestParam(name = "courseId", required = false) Long courseId) {
        Student s = studentRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        StudentPerformanceReport p = new StudentPerformanceReport(
                s.getId(), s.getFullName(), s.getStudentCode(),
                s.getProgramme(), 0, 0, 0, 0.0,
                List.of(), List.of()
        );
        return ResponseEntity.ok(ApiResponse.success(p, "Performance retrieved"));
    }

    public record StudentPerformanceReport(
            long studentId, String studentName, String rollNumber, String branchName,
            int totalClasses, int presentClasses, int absentClasses, double attendancePercentage,
            List<Object> chapterWiseAttendance, List<Object> participationSummary
    ) {}
}
