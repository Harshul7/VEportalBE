package org.example.veportal.controller;

import java.time.LocalDate;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final CourseRepository courseRepository;
    private final ClassSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final CourseMaterialRepository materialRepository;
    private final SessionService sessionService;

    public FacultyController(AuthenticatedUserProvider authenticatedUserProvider,
                             CourseRepository courseRepository,
                             ClassSessionRepository sessionRepository,
                             StudentRepository studentRepository,
                             AttendanceRecordRepository attendanceRepository,
                             CourseMaterialRepository materialRepository,
                             SessionService sessionService) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.materialRepository = materialRepository;
        this.sessionService = sessionService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<FacultyDashboardStats>> dashboard() {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> currentUser.getRole() == Role.ADMIN
                        || (c.getFaculty() != null && c.getFaculty().getId().equals(currentUser.getId())))
                .toList();
        long totalStudents = studentRepository.count();
        long classesConducted = sessionRepository.count();
        long materialsCount = materialRepository.count();

        FacultyDashboardStats stats = new FacultyDashboardStats(
                courses.size(),
                0,
                classesConducted,
                totalStudents,
                0.0,
                materialsCount,
                "2025-2026"
        );
        return ResponseEntity.ok(ApiResponse.success(stats, "Faculty dashboard stats retrieved"));
    }

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<CourseListItem>>> courses() {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        List<CourseListItem> courses = courseRepository.findAll().stream()
                .filter(c -> currentUser.getRole() == Role.ADMIN
                        || (c.getFaculty() != null && c.getFaculty().getId().equals(currentUser.getId())))
                .map(c -> new CourseListItem(
                        c.getId(),
                        c.getName(),
                        c.getCode(),
                        c.getTerm() != null ? c.getTerm() : "VE1",
                        1,
                        0L,
                        "2025-2026",
                        c.getStatus() != null ? c.getStatus() : "ACTIVE",
                        0,
                        c.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses, "Courses retrieved"));
    }

    @GetMapping("/courses/{courseId}/chapters")
    public ResponseEntity<ApiResponse<List<Object>>> courseChapters(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Chapters retrieved"));
    }

    @GetMapping("/class-sessions")
    public ResponseEntity<ApiResponse<List<ClassSessionListItem>>> classSessions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PagedResult<org.example.veportal.dto.response.SessionListItemResponse> result =
                sessionService.list(null, null, page, size);

        List<ClassSessionListItem> items = result.content().stream()
                .map(s -> new ClassSessionListItem(
                        Long.parseLong(s.id()),
                        0L,
                        "",
                        0L,
                        "",
                        0,
                        0L,
                        s.facultyName() == null ? "" : s.facultyName(),
                        s.date(),
                        s.topic(),
                        "",
                        "",
                        s.status(),
                        0,
                        0,
                        0.0,
                        s.date()
                ))
                .toList();

        PagedResult<ClassSessionListItem> paged = new PagedResult<>(items, result.pagination());
        return ResponseEntity.ok(ApiResponse.success(paged.content(), "Sessions retrieved", paged.pagination()));
    }

    @PostMapping("/class-sessions")
    public ResponseEntity<ApiResponse<ClassSessionListItem>> createClassSession(@RequestBody SessionCreateBodyRequest body) {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        Long courseId = body.courseId() != null ? body.courseId() : null;
        Course course = courseId != null
                ? courseRepository.findById(courseId)
                    .orElseThrow(() -> NotFoundException.resource("Course", courseId))
                : courseRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> NotFoundException.resource("Course", "none"));

        ClassSession session = new ClassSession();
        session.setCourse(course);
        session.setSessionNumber(sessionRepository.findMaxSessionNumber(course.getId()) + 1);
        session.setTopic(body.topic() == null || body.topic().isBlank()
                ? "Class Session on " + (body.sessionDate() == null ? LocalDate.now() : LocalDate.parse(body.sessionDate()))
                : body.topic().trim());
        session.setSessionDate(LocalDate.parse(body.sessionDate()));
        session.setStartTime("10:00");
        session.setEndTime("11:30");
        session.setRoom("LH-1");
        session.setFaculty(currentUser);
        session.setStatus(SessionStatus.UPCOMING);
        ClassSession saved = sessionRepository.save(session);

        ClassSessionListItem item = new ClassSessionListItem(
                saved.getId(),
                course.getId(),
                course.getName(),
                0L,
                "",
                0,
                currentUser.getId(),
                currentUser.getFullName(),
                saved.getSessionDate().toString(),
                saved.getTopic(),
                body.description() == null ? "" : body.description(),
                body.remarks() == null ? "" : body.remarks(),
                saved.getStatus().name(),
                0,
                0,
                0.0,
                saved.getCreatedAt().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(item, "Class session created"));
    }

    public record ClassSessionListItem(
            long id,
            long courseId,
            String courseName,
            long chapterId,
            String chapterTitle,
            int chapterNumber,
            long facultyId,
            String facultyName,
            String sessionDate,
            String topic,
            String description,
            String remarks,
            String status,
            int attendanceCount,
            int totalStudents,
            double attendancePercentage,
            String createdAt
    ) {}

    public record FacultyDashboardStats(
            long assignedCourses,
            long totalChapters,
            long classesConducted,
            long totalStudents,
            double averageAttendance,
            long materialsCount,
            String currentAcademicYear
    ) {}

    public record CourseListItem(
            long id,
            String name,
            String code,
            String courseType,
            int semester,
            long academicYearId,
            String academicYearName,
            String status,
            int chapterCount,
            String createdAt
    ) {}

    public record SessionCreateBodyRequest(
            Long courseId,
            Long chapterId,
            String sessionDate,
            String topic,
            String description,
            String remarks
    ) {}
}
