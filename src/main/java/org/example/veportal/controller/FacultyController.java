package org.example.veportal.controller;

import java.time.LocalDate;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.entity.AcademicYear;
import org.example.veportal.entity.Chapter;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseFaculty;
import org.example.veportal.entity.CourseMaterial;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.Topic;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.AcademicYearRepository;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ChapterRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.repository.TopicRepository;
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
    private final CourseFacultyRepository courseFacultyRepository;
    private final ChapterRepository chapterRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TopicRepository topicRepository;

    public FacultyController(AuthenticatedUserProvider authenticatedUserProvider,
                             CourseRepository courseRepository,
                             ClassSessionRepository sessionRepository,
                             StudentRepository studentRepository,
                             AttendanceRecordRepository attendanceRepository,
                             CourseMaterialRepository materialRepository,
                             SessionService sessionService,
                             CourseFacultyRepository courseFacultyRepository,
                             ChapterRepository chapterRepository,
                             CourseStudentRepository courseStudentRepository,
                             AcademicYearRepository academicYearRepository,
                             TopicRepository topicRepository) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.materialRepository = materialRepository;
        this.sessionService = sessionService;
        this.courseFacultyRepository = courseFacultyRepository;
        this.chapterRepository = chapterRepository;
        this.courseStudentRepository = courseStudentRepository;
        this.academicYearRepository = academicYearRepository;
        this.topicRepository = topicRepository;
    }

    private List<Course> coursesForCurrentUser(UserAccount currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return courseRepository.findAll();
        }
        List<Long> courseIds = courseFacultyRepository.findByFacultyId(currentUser.getId())
                .stream().map(CourseFaculty::getCourseId).toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return courseRepository.findAllById(courseIds);
    }

    private boolean canAccessCourse(UserAccount user, Long courseId) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return courseFacultyRepository.existsByCourseIdAndFacultyId(courseId, user.getId());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<FacultyDashboardStats>> dashboard() {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        List<Course> courses = coursesForCurrentUser(currentUser);
        long totalStudents = studentRepository.count();
        long classesConducted = sessionRepository.count();
        long materialsCount = materialRepository.count();
        AcademicYear current = academicYearRepository.findByCurrentTrue().orElse(null);
        String currentYear = current != null ? current.getName() : "2025-2026";

        FacultyDashboardStats stats = new FacultyDashboardStats(
                courses.size(),
                0,
                classesConducted,
                totalStudents,
                0.0,
                materialsCount,
                currentYear
        );
        return ResponseEntity.ok(ApiResponse.success(stats, "Faculty dashboard stats retrieved"));
    }

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<CourseListItem>>> courses() {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        List<CourseListItem> courses = coursesForCurrentUser(currentUser).stream()
                .map(c -> {
                    int chapterCount = (int) chapterRepository.countByCourseId(c.getId());
                    AcademicYear year = c.getAcademicYear();
                    return new CourseListItem(
                            c.getId(),
                            c.getName(),
                            c.getCode(),
                            c.getTerm() != null ? c.getTerm() : "VE1",
                            1,
                            year != null ? year.getId() : 0,
                            year != null ? year.getName() : "",
                            c.getStatus() != null ? c.getStatus() : "ACTIVE",
                            chapterCount,
                            c.getCreatedAt() == null ? "" : c.getCreatedAt().toString()
                    );
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses, "Courses retrieved"));
    }

    @GetMapping("/courses/{courseId}/chapters")
    public ResponseEntity<ApiResponse<List<ChapterItem>>> courseChapters(@PathVariable Long courseId) {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        if (!canAccessCourse(currentUser, courseId)) {
            return ResponseEntity.ok(ApiResponse.error("Course not accessible"));
        }

        List<ChapterItem> chapters = chapterRepository.findByCourseIdOrderByDisplayOrderAscChapterNumberAsc(courseId)
                .stream()
                .map(this::toChapterItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(chapters, "Chapters retrieved"));
    }

    @GetMapping("/chapters/{chapterId}/topics")
    public ResponseEntity<ApiResponse<List<TopicItem>>> topics(@PathVariable Long chapterId) {
        return ResponseEntity.ok(ApiResponse.success(
                topicRepository.findByChapterIdAndStatusOrderByDisplayOrderAscIdAsc(chapterId, "ACTIVE")
                        .stream().map(t -> new TopicItem(t.getId(), t.getTitle(),
                                t.getDescription() == null ? "" : t.getDescription())).toList(),
                "Topics retrieved"));
    }

    @GetMapping("/courses/{courseId}/students")
    public ResponseEntity<ApiResponse<List<CourseStudentItem>>> courseStudents(@PathVariable Long courseId) {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        if (!canAccessCourse(currentUser, courseId)) {
            return ResponseEntity.ok(ApiResponse.error("Course not accessible"));
        }
        List<Long> studentIds = courseStudentRepository.findByCourseId(courseId)
                .stream().map(org.example.veportal.entity.CourseStudent::getStudentId).toList();
        List<CourseStudentItem> items = studentIds.isEmpty()
                ? List.of()
                : studentRepository.findAllById(studentIds).stream()
                        .map(s -> new CourseStudentItem(s.getId(), s.getFullName(),
                                s.getStudentCode(), s.getProgramme()))
                        .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "Course students retrieved"));
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
        if (body.chapterId() != null) {
            chapterRepository.findById(body.chapterId())
                    .filter(ch -> ch.getCourse().getId().equals(course.getId()))
                    .ifPresent(session::setChapter);
        }
        if (body.topicId() != null) {
            topicRepository.findById(body.topicId())
                    .filter(topic -> body.chapterId() == null
                            || topic.getChapter().getId().equals(body.chapterId()))
                    .ifPresent(session::setTopicEntity);
        }
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

        Chapter chapter = saved.getChapter();
        ClassSessionListItem item = new ClassSessionListItem(
                saved.getId(),
                course.getId(),
                course.getName(),
                chapter != null ? chapter.getId() : 0L,
                chapter != null ? chapter.getTitle() : "",
                chapter != null ? chapter.getChapterNumber() : 0,
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

    private ChapterItem toChapterItem(Chapter ch) {
        return new ChapterItem(
                ch.getId(),
                ch.getChapterNumber(),
                ch.getTitle(),
                ch.getDescription() == null ? "" : ch.getDescription(),
                ch.getDisplayOrder() == null ? 0 : ch.getDisplayOrder(),
                ch.getStatus() == null ? "ACTIVE" : ch.getStatus(),
                ch.getCourse().getId(),
                ch.getCourse().getName(),
                ch.getCreatedAt() == null ? "" : ch.getCreatedAt().toString()
        );
    }

    public record ChapterItem(
            long id, int chapterNumber, String title, String description,
            int displayOrder, String status, long courseId, String courseName,
            String createdAt
    ) {}

    public record TopicItem(long id, String title, String description) {}

    public record CourseStudentItem(
            long id, String name, String rollNumber, String programme
    ) {}

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
            Long topicId,
            String sessionDate,
            String topic,
            String description,
            String remarks
    ) {}
}
