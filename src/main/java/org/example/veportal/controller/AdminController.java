package org.example.veportal.controller;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.TestDataStudent;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.TestDataStudentRepository;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.service.MailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserAccountRepository userAccountRepository;
    private final TestDataStudentRepository testDataStudentRepository;
    private final CourseRepository courseRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public AdminController(UserAccountRepository userAccountRepository,
                           TestDataStudentRepository testDataStudentRepository,
                           CourseRepository courseRepository,
                           ClassSessionRepository sessionRepository,
                           AttendanceRecordRepository attendanceRepository,
                           PasswordEncoder passwordEncoder,
                           MailService mailService) {
        this.userAccountRepository = userAccountRepository;
        this.testDataStudentRepository = testDataStudentRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> dashboard() {
        long totalStudents = testDataStudentRepository.count();
        long totalFaculty = userAccountRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FACULTY)
                .count();
        long activeCourses = courseRepository.count();
        long classesConducted = sessionRepository.count();

        DashboardStats stats = new DashboardStats(
                totalStudents, totalFaculty, 0, classesConducted,
                0.0, "2025-2026", activeCourses, 0
        );
        return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard stats retrieved"));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentListItem>>> listStudents(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<TestDataStudent> result = testDataStudentRepository.findAll(
                PageRequest.of(Math.max(page, 0), Math.min(size, 100),
                        Sort.by(Sort.Direction.ASC, "id")));

        List<StudentListItem> items = result.getContent().stream()
                .map(s -> toStudentItem(s))
                .toList();

        org.example.veportal.dto.PageMeta meta =
                new org.example.veportal.dto.PageMeta(result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages());
        PagedResult<StudentListItem> paged = new PagedResult<>(items, meta);
        return ResponseEntity.ok(ApiResponse.success(paged.content(), "Students retrieved", paged.pagination()));
    }

    @PostMapping("/students")
    public ResponseEntity<ApiResponse<StudentListItem>> createStudent(@RequestBody CreateStudentRequest request) {
        String code = request.rollNumber() == null || request.rollNumber().isBlank()
                ? generatedStudentCode() : request.rollNumber().trim();
        if (testDataStudentRepository.existsByStudentCodeIgnoreCase(code)) {
            return ResponseEntity.ok(ApiResponse.error("A student with roll number " + code + " already exists"));
        }
        TestDataStudent s = new TestDataStudent();
        s.setStudentCode(code);
        s.setFullName(request.name() == null ? "" : request.name().trim());
        s.setEmail(request.email() == null ? null : request.email().trim());
        s.setProgramme(branchNameForId(request.branchId()));
        s.setStatus(AccountStatus.ACTIVE);
        s.setBatch("2025");
        TestDataStudent saved = testDataStudentRepository.save(s);
        return ResponseEntity.ok(ApiResponse.success(toStudentItem(saved), "Student created"));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<ApiResponse<StudentListItem>> updateStudent(@PathVariable Long id,
                                                                      @RequestBody CreateStudentRequest request) {
        TestDataStudent s = testDataStudentRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        if (request.rollNumber() != null && !request.rollNumber().isBlank()) {
            s.setStudentCode(request.rollNumber().trim());
        }
        if (request.name() != null && !request.name().isBlank()) {
            s.setFullName(request.name().trim());
        }
        if (request.email() != null) {
            s.setEmail(request.email().trim());
        }
        if (request.branchId() != null) {
            s.setProgramme(branchNameForId(request.branchId()));
        }
        TestDataStudent saved = testDataStudentRepository.save(s);
        return ResponseEntity.ok(ApiResponse.success(toStudentItem(saved), "Student updated"));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<StudentListItem>> getStudent(@PathVariable Long id) {
        TestDataStudent s = testDataStudentRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(toStudentItem(s), "Student retrieved"));
    }

    @GetMapping("/students/{id}/performance")
    public ResponseEntity<ApiResponse<StudentPerformance>> studentPerformance(@PathVariable Long id) {
        TestDataStudent s = testDataStudentRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        StudentPerformance p = new StudentPerformance(
                s.getId(), s.getFullName(), s.getStudentCode(),
                s.getProgramme(), 0, 0, 0, 0.0, List.of(), List.of()
        );
        return ResponseEntity.ok(ApiResponse.success(p, "Performance retrieved"));
    }

    @GetMapping("/faculty")
    public ResponseEntity<ApiResponse<List<FacultyListItem>>> listFaculty(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        List<FacultyListItem> faculty = userAccountRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FACULTY)
                .filter(u -> search == null || search.isBlank()
                        || u.getFullName().toLowerCase().contains(search.toLowerCase())
                        || u.getEmail().toLowerCase().contains(search.toLowerCase()))
                .map(this::toFacultyItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(faculty, "Faculty retrieved"));
    }

    @PostMapping("/faculty")
    public ResponseEntity<ApiResponse<FacultyListItem>> createFaculty(@RequestBody CreateFacultyRequest request) {
        String email = request.email() == null ? null : request.email().trim();
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("Email is required"));
        }
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.ok(ApiResponse.error("A user with email " + email + " already exists"));
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setFullName(request.name() == null ? "" : request.name().trim());
        user.setStaffCode(request.employeeId() == null || request.employeeId().isBlank()
                ? generatedStaffCode() : request.employeeId().trim());
        user.setDepartment(request.department() == null ? "" : request.department().trim());
        user.setRole(Role.FACULTY);
        user.setStatus(AccountStatus.ACTIVE);
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        UserAccount saved = userAccountRepository.save(user);
        mailService.sendCredentials(saved.getEmail(), saved.getFullName(), saved.getEmail(), temporaryPassword);
        return ResponseEntity.ok(ApiResponse.success(
                toFacultyItem(saved, temporaryPassword), "Faculty created; use the temporary password to sign in"));
    }

    @PostMapping("/faculty/{id}/account")
    public ResponseEntity<ApiResponse<FacultyListItem>> createFacultyAccount(@PathVariable Long id,
                                                                             @RequestBody AccountCreateRequest request) {
        UserAccount user = userAccountRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error("Faculty not found"));
        }
        if (request.role() != null && "ADMIN".equalsIgnoreCase(request.role())) {
            user.setRole(Role.ADMIN);
        } else {
            user.setRole(Role.FACULTY);
        }
        user.setStatus(AccountStatus.ACTIVE);
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        UserAccount saved = userAccountRepository.save(user);
        if (request.sendEmail() == null || request.sendEmail()) {
            mailService.sendCredentials(saved.getEmail(), saved.getFullName(), saved.getEmail(), temporaryPassword);
        }
        return ResponseEntity.ok(ApiResponse.success(
                toFacultyItem(saved, temporaryPassword), "Account activated; use the temporary password to sign in"));
    }

    @PatchMapping("/faculty/{id}/status")
    public ResponseEntity<ApiResponse<FacultyListItem>> updateFacultyStatus(@PathVariable Long id,
                                                                            @RequestBody FacultyStatusRequest request) {
        UserAccount user = userAccountRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error("Faculty not found"));
        }
        user.setStatus(request.active() ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
        UserAccount saved = userAccountRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(toFacultyItem(saved), "Faculty status updated"));
    }

    @PostMapping("/faculty/{id}/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetFacultyPassword(@PathVariable Long id) {
        UserAccount user = userAccountRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error("Faculty not found"));
        }
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        userAccountRepository.save(user);
        mailService.sendCredentials(user.getEmail(), user.getFullName(), user.getEmail(), temporaryPassword);
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("temporaryPassword", temporaryPassword),
                "Password reset; new temporary password generated"));
    }

    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<List<BranchItem>>> listBranches() {
        List<BranchItem> branches = new ArrayList<>();
        int id = 1;
        for (String programme : testDataStudentRepository.findDistinctProgrammes()) {
            branches.add(new BranchItem(id++, programme, programme, true, LocalDateTime.now().toString()));
        }
        if (branches.isEmpty()) {
            branches.add(new BranchItem(1, "B.Tech CSE", "B.Tech CSE", true, LocalDateTime.now().toString()));
        }
        return ResponseEntity.ok(ApiResponse.success(branches, "Branches retrieved"));
    }

    @PostMapping("/branches")
    public ResponseEntity<ApiResponse<BranchItem>> createBranch(@RequestBody CreateBranchRequest request) {
        String name = request.name() == null || request.name().isBlank()
                ? (request.code() == null ? "New Branch" : request.code()) : request.name().trim();
        String code = request.code() == null || request.code().isBlank() ? name : request.code().trim();
        BranchItem created = new BranchItem(999L, code, name, true, LocalDateTime.now().toString());
        return ResponseEntity.ok(ApiResponse.success(created, "Branch created"));
    }

    @GetMapping("/academic-years")
    public ResponseEntity<ApiResponse<List<AcademicYearItem>>> listAcademicYears() {
        return ResponseEntity.ok(ApiResponse.success(
                List.of(new AcademicYearItem(1L, "2025-2026", 2025, 2026, true, true,
                        LocalDateTime.now().toString())),
                "Academic years retrieved"));
    }

    @PostMapping("/academic-years")
    public ResponseEntity<ApiResponse<AcademicYearItem>> createAcademicYear(@RequestBody CreateAcademicYearRequest request) {
        AcademicYearItem created = new AcademicYearItem(2L,
                request.name() == null || request.name().isBlank() ? "2026-2027" : request.name().trim(),
                request.startYear() == null ? 2026 : request.startYear(),
                request.endYear() == null ? 2027 : request.endYear(),
                false, true, LocalDateTime.now().toString());
        return ResponseEntity.ok(ApiResponse.success(created, "Academic year created"));
    }

    @PutMapping("/academic-years/{yearId}/current")
    public ResponseEntity<ApiResponse<AcademicYearItem>> setCurrentYear(@PathVariable Long yearId) {
        AcademicYearItem item = new AcademicYearItem(yearId, "2025-2026", 2025, 2026, true, true,
                LocalDateTime.now().toString());
        return ResponseEntity.ok(ApiResponse.success(item, "Academic year marked as current"));
    }

    @GetMapping("/academic-years/{yearId}/courses")
    public ResponseEntity<ApiResponse<List<CourseListItem>>> coursesByYear(@PathVariable Long yearId) {
        List<CourseListItem> courses = courseRepository.findAll().stream()
                .map(this::toCourseItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses, "Courses retrieved"));
    }

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<CourseListItem>> createCourse(@RequestBody CreateCourseRequest request) {
        Course course = new Course();
        course.setCode(request.code() == null ? "VE-" + System.currentTimeMillis() : request.code().trim());
        course.setName(request.name() == null ? "Untitled Course" : request.name().trim());
        course.setTerm(request.courseType() == null ? "VE1" : request.courseType().trim());
        course.setStatus("ACTIVE");
        Course saved = courseRepository.save(course);
        return ResponseEntity.ok(ApiResponse.success(toCourseItem(saved), "Course created"));
    }

    @GetMapping("/courses/{courseId}/chapters")
    public ResponseEntity<ApiResponse<List<Object>>> courseChapters(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Chapters retrieved"));
    }

    @PostMapping("/courses/{courseId}/chapters")
    public ResponseEntity<ApiResponse<Object>> createChapter(@PathVariable Long courseId,
                                                             @RequestBody CreateChapterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(null, "Chapter created"));
    }

    private StudentListItem toStudentItem(TestDataStudent s) {
        return new StudentListItem(
                s.getId(),
                s.getStudentCode(),
                s.getFullName(),
                s.getEmail() == null ? "" : s.getEmail(),
                null,
                s.getProgramme(),
                s.getProgramme(),
                0,
                s.getStatus().name(),
                0
        );
    }

    private FacultyListItem toFacultyItem(UserAccount u) {
        return toFacultyItem(u, null);
    }

    private FacultyListItem toFacultyItem(UserAccount u, String temporaryPassword) {
        return new FacultyListItem(
                u.getId(),
                u.getStaffCode(),
                u.getFullName(),
                u.getEmail(),
                u.getStaffCode(),
                u.getDepartment() == null ? "" : u.getDepartment(),
                u.getRole().name(),
                u.getStatus() == AccountStatus.ACTIVE,
                true,
                u.getCreatedAt().toString(),
                temporaryPassword
        );
    }

    private CourseListItem toCourseItem(Course c) {
        return new CourseListItem(
                c.getId(),
                c.getName(),
                c.getCode(),
                c.getTerm() != null ? c.getTerm() : "VE1",
                1,
                1L,
                "2025-2026",
                c.getStatus() != null ? c.getStatus() : "ACTIVE",
                0,
                c.getCreatedAt().toString()
        );
    }

    private String branchNameForId(Long branchId) {
        if (branchId == null || branchId <= 0) {
            return "General";
        }
        List<String> programmes = testDataStudentRepository.findDistinctProgrammes();
        if (branchId - 1 < programmes.size()) {
            return programmes.get(branchId.intValue() - 1);
        }
        return "General";
    }

    private String generatedStudentCode() {
        long next = testDataStudentRepository.count() + 1001;
        return "TES" + next;
    }

    private String generatedStaffCode() {
        return "STF-" + (System.currentTimeMillis() % 100000);
    }

    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String symbols = "!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(symbols.charAt(random.nextInt(symbols.length())));
        String pool = upper + lower + digits + symbols;
        for (int i = 0; i < 6; i++) {
            sb.append(pool.charAt(random.nextInt(pool.length())));
        }
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char t = chars[i];
            chars[i] = chars[j];
            chars[j] = t;
        }
        return new String(chars);
    }

    public record DashboardStats(
            long totalStudents, long totalFaculty, long totalChapters, long classesConducted,
            double overallAttendancePercentage, String currentAcademicYear,
            long activeCourses, long activeChapters
    ) {}

    public record StudentListItem(
            long id, String rollNumber, String name, String email,
            Long branchId, String branchName, String branchCode,
            int semester, String status, double attendancePercentage
    ) {}

    public record FacultyListItem(
            long id, String facultyId, String name, String email,
            String employeeId, String department, String role,
            boolean active, boolean hasAccount, String createdAt,
            String temporaryPassword
    ) {}

    public record CourseListItem(
            long id, String name, String code, String courseType,
            int semester, long academicYearId, String academicYearName,
            String status, int chapterCount, String createdAt
    ) {}

    public record BranchItem(long id, String code, String name, boolean active, String createdAt) {}

    public record AcademicYearItem(long id, String name, int startYear, int endYear,
                                   boolean current, boolean active, String createdAt) {}

    public record StudentPerformance(
            long studentId, String studentName, String rollNumber, String branchName,
            int totalClasses, int presentClasses, int absentClasses, double attendancePercentage,
            List<Object> chapterWiseAttendance, List<Object> participationSummary
    ) {}

    public record CreateStudentRequest(String rollNumber, String name, String email,
                                       Long branchId, Long academicYearId, Integer semester) {}

    public record CreateFacultyRequest(String name, String email, String employeeId, String department) {}

    public record AccountCreateRequest(String role, Boolean sendEmail) {}

    public record FacultyStatusRequest(boolean active) {}

    public record CreateBranchRequest(String code, String name) {}

    public record CreateAcademicYearRequest(String name, Integer startYear, Integer endYear) {}

    public record CreateCourseRequest(String name, String code, String courseType, Integer semester,
                                      Long academicYearId) {}

    public record CreateChapterRequest(String title, String description) {}
}