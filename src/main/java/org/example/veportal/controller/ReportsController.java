package org.example.veportal.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.entity.AttendanceRecord;
import org.example.veportal.entity.ParticipationLevel;
import org.example.veportal.entity.ParticipationRecord;
import org.example.veportal.entity.Student;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ParticipationRecordRepository;
import org.example.veportal.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final ParticipationRecordRepository participationRepository;

    public ReportsController(StudentRepository studentRepository,
                             AttendanceRecordRepository attendanceRepository,
                             ParticipationRecordRepository participationRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.participationRepository = participationRepository;
    }

    @GetMapping("/students/{id}/performance")
    public ResponseEntity<ApiResponse<StudentPerformanceReport>> studentPerformance(
            @PathVariable Long id,
            @RequestParam(name = "courseId", required = false) Long courseId) {
        Student student = studentRepository.findById(id).orElse(null);
        if (student == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }

        List<AttendanceRecord> records = attendanceRepository.findStudentHistory(id, courseId);
        long present = records.stream().filter(r -> r.getStatus().name().equals("PRESENT")).count();
        Map<Long, ChapterSummary> chapters = new LinkedHashMap<>();
        for (AttendanceRecord record : records) {
            var chapter = record.getSession().getChapter();
            if (chapter == null) continue;
            ChapterSummary current = chapters.computeIfAbsent(chapter.getId(),
                    ignored -> new ChapterSummary(chapter.getId(), chapter.getChapterNumber(),
                            chapter.getTitle(), 0, 0));
            chapters.put(chapter.getId(), new ChapterSummary(current.chapterId(), current.chapterNumber(),
                    current.chapterTitle(), current.totalClasses() + 1,
                    current.presentClasses() + (record.getStatus().name().equals("PRESENT") ? 1 : 0)));
        }

        List<ParticipationRecord> participation = participationRepository.findStudentHistory(id, courseId);
        Map<String, Long> participationSummary = new LinkedHashMap<>();
        for (ParticipationRecord record : participation) {
            if (record.getLevel() != ParticipationLevel.NOT_RECORDED) {
                participationSummary.merge(record.getLevel().name(), 1L, Long::sum);
            }
        }
        int total = records.size();
        double percentage = total == 0 ? 0.0 : (present * 100.0) / total;
        StudentPerformanceReport report = new StudentPerformanceReport(
                student.getId(), student.getFullName(), student.getStudentCode(), student.getProgramme(),
                total, (int) present, total - (int) present, percentage,
                chapters.values().stream().map(c -> new ChapterAttendance(
                        c.chapterId(), c.chapterTitle(), c.chapterNumber(), c.totalClasses(),
                        c.presentClasses(), c.totalClasses() == 0 ? 0.0 :
                                c.presentClasses() * 100.0 / c.totalClasses())).toList(),
                participationSummary.entrySet().stream()
                        .map(e -> new ParticipationSummary(e.getKey(), e.getValue())).toList());
        return ResponseEntity.ok(ApiResponse.success(report, "Performance retrieved"));
    }

    private record ChapterSummary(long chapterId, int chapterNumber, String chapterTitle,
                                  int totalClasses, int presentClasses) {}

    public record ChapterAttendance(long chapterId, String chapterTitle, int chapterNumber,
                                    int totalClasses, int presentClasses, double percentage) {}
    public record ParticipationSummary(String level, long count) {}
    public record StudentPerformanceReport(
            long studentId, String studentName, String rollNumber, String branchName,
            int totalClasses, int presentClasses, int absentClasses, double attendancePercentage,
            List<ChapterAttendance> chapterWiseAttendance,
            List<ParticipationSummary> participationSummary) {}
}
