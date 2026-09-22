package org.example.veportal.controller;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.example.veportal.entity.Chapter;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseFaculty;
import org.example.veportal.entity.CourseStudent;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.Topic;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.AcademicYearRepository;
import org.example.veportal.repository.BranchRepository;
import org.example.veportal.repository.ChapterRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.repository.TopicRepository;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType PDF = MediaType.APPLICATION_PDF;

    private final ExportService exportService;
    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final CourseFacultyRepository courseFacultyRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final TopicRepository topicRepository;
    private final AcademicYearRepository academicYearRepository;
    private final BranchRepository branchRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ExportController(ExportService exportService, UserAccountRepository userAccountRepository,
                            StudentRepository studentRepository, CourseRepository courseRepository,
                            ChapterRepository chapterRepository, CourseFacultyRepository courseFacultyRepository,
                            CourseStudentRepository courseStudentRepository,
                            TopicRepository topicRepository,
                            AcademicYearRepository academicYearRepository,
                            BranchRepository branchRepository,
                            AuthenticatedUserProvider authenticatedUserProvider) {
        this.exportService = exportService;
        this.userAccountRepository = userAccountRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.courseFacultyRepository = courseFacultyRepository;
        this.courseStudentRepository = courseStudentRepository;
        this.topicRepository = topicRepository;
        this.academicYearRepository = academicYearRepository;
        this.branchRepository = branchRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/admin/exports/{dataset}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> adminExport(@PathVariable String dataset,
                                              @RequestParam(defaultValue = "xlsx") String format) {
        return switch (dataset.toLowerCase()) {
            case "faculty" -> createExport("faculty", "Faculty Users", facultyHeaders(), facultyRows(), format);
            case "students" -> createExport("students", "Students", studentHeaders(), studentRows(studentRepository.findAll()), format);
            case "academic-structure" -> createExport("academic-structure", "Academic Structure", academicHeaders(),
                    academicRows(courseRepository.findAll(), true), format);
            default -> ResponseEntity.notFound().build();
        };
    }

    @GetMapping("/faculty/exports/{dataset}")
    @PreAuthorize("hasRole('FACULTY')")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> facultyExport(@PathVariable String dataset,
                                                @RequestParam(defaultValue = "xlsx") String format) {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        List<Course> courses = assignedCourses(currentUser);
        return switch (dataset.toLowerCase()) {
            case "students" -> createExport("students", "My Course Students", studentHeaders(),
                    studentRows(assignedStudents(courses)), format);
            case "academic-structure" -> createExport("academic-structure", "My Academic Structure", academicHeaders(),
                    academicRows(courses, false), format);
            default -> ResponseEntity.notFound().build();
        };
    }

    private ResponseEntity<byte[]> createExport(String fileBase, String title, List<String> headers,
                                                List<List<String>> rows, String format) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] body = pdf ? exportService.pdf(title, headers, rows) : exportService.xlsx(title, headers, rows);
        String extension = pdf ? "pdf" : "xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileBase + "." + extension + "\"")
                .contentType(pdf ? PDF : XLSX)
                .body(body);
    }

    private List<String> facultyHeaders() {
        return List.of("Name", "Email", "Employee ID", "Role", "Status", "Password State", "Created At");
    }

    private List<List<String>> facultyRows() {
        return userAccountRepository.findAll().stream()
                .map(user -> List.of(text(user.getFullName()), text(user.getEmail()), text(user.getStaffCode()),
                        user.getRole().name(), user.getStatus().name(),
                        user.isMustChangePassword() ? "Password setup required" : "Password configured",
                        text(user.getCreatedAt())))
                .toList();
    }

    private List<String> studentHeaders() {
        return List.of("Roll Number", "Name", "Email", "Branch", "Batch", "Status", "Created At");
    }

    private List<List<String>> studentRows(List<Student> students) {
        return students.stream().map(student -> List.of(text(student.getStudentCode()), text(student.getFullName()),
                text(student.getEmail()), text(student.getBranchCode(), student.getProgramme()), text(student.getBatch()),
                student.getStatus().name(), text(student.getCreatedAt()))).toList();
    }

    private List<String> academicHeaders() {
        return List.of("Record Type", "Academic Year", "Course Code", "Course Name", "Term", "Course Status",
                "Chapter Number", "Chapter Title", "Chapter Status", "Topic", "Topic Status");
    }

    private List<List<String>> academicRows(List<Course> courses, boolean includeReferenceData) {
        List<List<String>> rows = new ArrayList<>();
        if (includeReferenceData) {
            academicYearRepository.findAll().forEach(year -> rows.add(List.of(
                    "Academic Year", text(year.getName()), "", "", "", year.getStatus().name(), "", "", "", "", "")));
            branchRepository.findByActiveTrueOrderByCodeAsc().forEach(branch -> rows.add(List.of(
                    "Branch", "", branch.getCode(), branch.getName(), "", branch.isActive() ? "ACTIVE" : "INACTIVE", "", "", "", "", "")));
        }
        for (Course course : courses) {
            List<Chapter> chapters = chapterRepository.findByCourseIdOrderByDisplayOrderAscChapterNumberAsc(course.getId());
            if (chapters.isEmpty()) {
                rows.add(academicRow(course, null, null));
                continue;
            }
            for (Chapter chapter : chapters) {
                List<Topic> topics = topicRepository.findByChapterIdOrderByDisplayOrderAscIdAsc(chapter.getId());
                if (topics.isEmpty()) rows.add(academicRow(course, chapter, null));
                else for (Topic topic : topics) rows.add(academicRow(course, chapter, topic));
            }
        }
        return rows;
    }

    private List<String> academicRow(Course course, Chapter chapter, Topic topic) {
        String year = course.getAcademicYear() == null ? "" : text(course.getAcademicYear().getName());
        return List.of("Course", year, text(course.getCode()), text(course.getName()), text(course.getTerm()), text(course.getStatus()),
                chapter == null ? "" : text(chapter.getChapterNumber()),
                chapter == null ? "" : text(chapter.getTitle()),
                chapter == null ? "" : text(chapter.getStatus()),
                topic == null ? "" : text(topic.getTitle()), topic == null ? "" : text(topic.getStatus()));
    }

    private List<Course> assignedCourses(UserAccount user) {
        List<Long> ids = courseFacultyRepository.findByFacultyId(user.getId()).stream()
                .map(CourseFaculty::getCourseId).toList();
        return courseRepository.findAllById(ids);
    }

    private List<Student> assignedStudents(List<Course> courses) {
        Set<Long> studentIds = new LinkedHashSet<>();
        for (Course course : courses) {
            for (CourseStudent enrollment : courseStudentRepository.findByCourseId(course.getId())) {
                studentIds.add(enrollment.getStudentId());
            }
        }
        return studentRepository.findAllById(studentIds);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String text(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? text(fallback) : preferred;
    }
}
