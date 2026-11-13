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
import org.example.veportal.service.ExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final ParticipationRecordRepository participationRepository;
    private final ExportService exportService;

    public ReportsController(StudentRepository studentRepository,
                             AttendanceRecordRepository attendanceRepository,
                             ParticipationRecordRepository participationRepository,
                             ExportService exportService) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.participationRepository = participationRepository;
        this.exportService = exportService;
    }

    @GetMapping("/students/{id}/performance")
    @PreAuthorize("@resourceAuthorization.canAccessStudent(#id, #courseId)")
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

    @GetMapping("/students/{id}/ve-history")
    @PreAuthorize("@resourceAuthorization.canAccessStudent(#id, null)")
    public ResponseEntity<ApiResponse<List<VeCourseHistory>>> veHistory(@PathVariable Long id) {
        if (studentRepository.findById(id).isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        Map<Long, VeCourseHistoryBuilder> grouped = new LinkedHashMap<>();
        attendanceRepository.findStudentHistory(id, null).forEach(record -> {
            var course = record.getSession().getCourse();
            var builder = grouped.computeIfAbsent(course.getId(), ignored -> new VeCourseHistoryBuilder(
                    course.getId(), course.getCode(), course.getName(), course.getCourseType(),
                    course.getAcademicYear() == null ? "" : course.getAcademicYear().getName()));
            builder.total++;
            if (record.getStatus().name().equals("PRESENT")) builder.present++;
            if (builder.firstSessionDate == null || record.getSession().getSessionDate().isBefore(builder.firstSessionDate)) {
                builder.firstSessionDate = record.getSession().getSessionDate();
            }
            if (builder.lastSessionDate == null || record.getSession().getSessionDate().isAfter(builder.lastSessionDate)) {
                builder.lastSessionDate = record.getSession().getSessionDate();
            }
        });
        participationRepository.findStudentHistory(id, null).forEach(record -> {
            var courseId = record.getSession().getCourse().getId();
            var builder = grouped.get(courseId);
            if (builder != null) {
                builder.participationRecords++;
                if (record.getLevel() == ParticipationLevel.HIGH) builder.highParticipationRecords++;
            }
        });
        List<VeCourseHistory> result = grouped.values().stream().map(VeCourseHistoryBuilder::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(result, "VE course history retrieved"));
    }

    @GetMapping(value = "/students/{id}/performance/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportStudentPerformance(@PathVariable Long id,
                                                            @RequestParam(name = "courseId", required = false) Long courseId,
                                                            @RequestParam(name = "format", defaultValue = "csv") String format) {
        Student student = studentRepository.findById(id).orElseThrow(() ->
                org.example.veportal.exception.NotFoundException.resource("Student", id));
        List<AttendanceRecord> records = attendanceRepository.findStudentHistory(id, courseId);
        long present = records.stream().filter(r -> r.getStatus().name().equals("PRESENT")).count();
        StringBuilder csv = new StringBuilder("Roll Number,Name,Branch,Section,Date,Chapter,Status,Participation,Participation Notes\n");
        Map<Long, ParticipationRecord> participationBySession = new LinkedHashMap<>();
        participationRepository.findStudentHistory(id, courseId).forEach(p -> participationBySession.put(p.getSession().getId(), p));
        List<List<String>> rows = new ArrayList<>();
        for (AttendanceRecord record : records) {
            var chapter = record.getSession().getChapter();
            ParticipationRecord participation = participationBySession.get(record.getSession().getId());
            List<String> row = List.of(student.getStudentCode(), student.getFullName(), student.getProgramme(), student.getBatch(),
                    String.valueOf(record.getSession().getSessionDate()), chapter == null ? "" : chapter.getChapterNumber() + " - " + chapter.getTitle(),
                    record.getStatus().name(), participation == null ? "" : participation.getLevel().name(),
                    participation == null ? "" : participation.getNotes());
            rows.add(row);
            csv.append(csv(student.getStudentCode())).append(',')
                    .append(csv(student.getFullName())).append(',')
                    .append(csv(student.getProgramme())).append(',')
                    .append(csv(student.getBatch())).append(',')
                    .append(record.getSession().getSessionDate()).append(',')
                    .append(csv(chapter == null ? "" : chapter.getChapterNumber() + " - " + chapter.getTitle())).append(',')
                    .append(record.getStatus().name()).append(',')
                    .append(csv(participation == null ? "" : participation.getLevel().name())).append(',')
                    .append(csv(participation == null ? "" : participation.getNotes())).append('\n');
        }
        csv.append("\nSummary,,,,,,,,\nPresent,").append(present)
                .append(",Total,").append(records.size())
                .append(",Attendance %," ).append(records.isEmpty() ? "0" : String.format(java.util.Locale.ROOT, "%.2f", present * 100.0 / records.size())).append('\n');
        if ("pdf".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            List<String> headers = List.of("Roll Number", "Name", "Branch", "Batch", "Date", "Chapter", "Status", "Participation", "Participation Notes");
            byte[] body = "pdf".equalsIgnoreCase(format)
                    ? exportService.pdf("Student Performance - " + student.getFullName(), headers, rows)
                    : exportService.xlsx("Student Performance", headers, rows);
            String extension = format.toLowerCase();
            MediaType contentType = "pdf".equalsIgnoreCase(format) ? MediaType.APPLICATION_PDF
                    : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"student-performance-" + student.getStudentCode() + "." + extension + "\"")
                    .contentType(contentType).body(body);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"student-performance-" + student.getStudentCode() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static final class VeCourseHistoryBuilder {
        private final Long courseId;
        private final String courseCode;
        private final String courseName;
        private final String courseType;
        private final String academicYear;
        private int total;
        private int present;
        private java.time.LocalDate firstSessionDate;
        private java.time.LocalDate lastSessionDate;
        private int participationRecords;
        private int highParticipationRecords;

        private VeCourseHistoryBuilder(Long courseId, String courseCode, String courseName, String courseType,
                                       String academicYear) {
            this.courseId = courseId;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.courseType = courseType;
            this.academicYear = academicYear;
        }

        private VeCourseHistory toResponse() {
            double percentage = total == 0 ? 0.0 : present * 100.0 / total;
            return new VeCourseHistory(courseId, courseCode, courseName, courseType, academicYear,
                    firstSessionDate, lastSessionDate, total, present, total - present, percentage,
                    total > 0 && percentage >= 75.0,
                    participationRecords, highParticipationRecords);
        }
    }

    public record VeCourseHistory(Long courseId, String courseCode, String courseName, String courseType,
                                  String academicYear, java.time.LocalDate firstSessionDate,
                                  java.time.LocalDate lastSessionDate, int totalClasses, int presentClasses,
                                  int absentClasses, double attendancePercentage, boolean passed,
                                  int participationRecords,
                                  int highParticipationRecords) {}

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
