package org.example.veportal.controller;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.AcademicYear;
import org.example.veportal.entity.Chapter;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseFaculty;
import org.example.veportal.entity.CourseStudent;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.Topic;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.AcademicYearRepository;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ChapterRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.repository.TopicRepository;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.service.MailService;
import org.example.veportal.util.Percent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserAccountRepository userAccountRepository;
    private final CourseRepository courseRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ChapterRepository chapterRepository;
    private final CourseFacultyRepository courseFacultyRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final StudentRepository studentRepository;
    private final TopicRepository topicRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public AdminController(UserAccountRepository userAccountRepository,
                           CourseRepository courseRepository,
                           ClassSessionRepository sessionRepository,
                           AttendanceRecordRepository attendanceRepository,
                           AcademicYearRepository academicYearRepository,
                           ChapterRepository chapterRepository,
                           CourseFacultyRepository courseFacultyRepository,
                           CourseStudentRepository courseStudentRepository,
                           StudentRepository studentRepository,
                           TopicRepository topicRepository,
                           PasswordEncoder passwordEncoder,
                           MailService mailService) {
        this.userAccountRepository = userAccountRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.academicYearRepository = academicYearRepository;
        this.chapterRepository = chapterRepository;
        this.courseFacultyRepository = courseFacultyRepository;
        this.courseStudentRepository = courseStudentRepository;
        this.studentRepository = studentRepository;
        this.topicRepository = topicRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> dashboard() {
        long totalStudents = studentRepository.count();
        long totalFaculty = userAccountRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FACULTY)
                .count();
        long activeCourses = courseRepository.count();
        long classesConducted = sessionRepository.count();
        AcademicYear current = academicYearRepository.findByCurrentTrue().orElse(null);
        long totalChapters = chapterRepository.count();
        String currentYear = current != null ? current.getName() : "2025-2026";

        DashboardStats stats = new DashboardStats(
                totalStudents, totalFaculty, totalChapters, classesConducted,
                0.0, currentYear, activeCourses, totalChapters
        );
        return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard stats retrieved"));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentListItem>>> listStudents(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Specification<Student> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("studentCode")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)));
        }
        if (branchId != null && branchId > 0) {
            String programme = branchNameForId(branchId);
            if (!"General".equals(programme)) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("programme"), programme));
            }
        }
        Page<Student> result = studentRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(size, 100),
                        Sort.by(Sort.Direction.ASC, "id")));

        Map<Long, Double> percents = attendancePercentages(result.getContent());
        List<StudentListItem> items = result.getContent().stream()
                .map(s -> toStudentItem(s, percents.get(s.getId())))
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
        if (studentRepository.existsByStudentCodeIgnoreCase(code)) {
            return ResponseEntity.ok(ApiResponse.error("A student with roll number " + code + " already exists"));
        }
        Student s = new Student();
        s.setStudentCode(code);
        s.setFullName(request.name() == null ? "" : request.name().trim());
        s.setEmail(request.email() == null || request.email().isBlank()
                ? deriveEmail(request.name()) : request.email().trim());
        s.setProgramme(branchNameForId(request.branchId()));
        s.setStatus(AccountStatus.ACTIVE);
        s.setBatch(academicYearBatchName(request.academicYearId()));
        Student saved = studentRepository.save(s);
        return ResponseEntity.ok(ApiResponse.success(toStudentItem(saved, null), "Student created"));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<ApiResponse<StudentListItem>> updateStudent(@PathVariable Long id,
                                                                      @RequestBody CreateStudentRequest request) {
        Student s = studentRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        if (request.rollNumber() != null && !request.rollNumber().isBlank()) {
            s.setStudentCode(request.rollNumber().trim());
        }
        if (request.name() != null && !request.name().isBlank()) {
            s.setFullName(request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            s.setEmail(request.email().trim());
        }
        if (request.branchId() != null) {
            s.setProgramme(branchNameForId(request.branchId()));
        }
        if (request.academicYearId() != null) {
            s.setBatch(academicYearBatchName(request.academicYearId()));
        }
        Student saved = studentRepository.save(s);
        Double percent = attendancePercentages(List.of(saved)).get(saved.getId());
        return ResponseEntity.ok(ApiResponse.success(toStudentItem(saved, percent), "Student updated"));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<StudentListItem>> getStudent(@PathVariable Long id) {
        Student s = studentRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        Double percent = attendancePercentages(List.of(s)).get(s.getId());
        return ResponseEntity.ok(ApiResponse.success(toStudentItem(s, percent), "Student retrieved"));
    }

    @GetMapping("/students/{id}/performance")
    public ResponseEntity<ApiResponse<StudentPerformance>> studentPerformance(@PathVariable Long id) {
        Student s = studentRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        AttendanceRecordRepository.StudentAttendanceAggregation agg =
                attendanceRepository.aggregateByStudents(List.of(s.getId())).stream().findFirst().orElse(null);
        int total = agg != null ? (int) agg.getTotal() : 0;
        int present = agg != null ? (int) agg.getPresent() : 0;
        Double percent = Percent.of(present, total);
        StudentPerformance p = new StudentPerformance(
                s.getId(), s.getFullName(), s.getStudentCode(),
                s.getProgramme(), total, present, total - present,
                percent == null ? 0.0 : percent, List.of(), List.of()
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
        for (String programme : studentRepository.findDistinctProgrammes()) {
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
        List<AcademicYearItem> items = academicYearRepository.findAll(Sort.by(Sort.Direction.DESC, "startYear"))
                .stream()
                .map(this::toAcademicYearItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "Academic years retrieved"));
    }

    @PostMapping("/academic-years")
    public ResponseEntity<ApiResponse<AcademicYearItem>> createAcademicYear(@RequestBody CreateAcademicYearRequest request) {
        String name = request.name() == null || request.name().isBlank()
                ? (request.startYear() + "-" + request.endYear()) : request.name().trim();
        if (academicYearRepository.findByName(name).isPresent()) {
            return ResponseEntity.ok(ApiResponse.error("An academic year named " + name + " already exists"));
        }
        AcademicYear year = new AcademicYear();
        year.setName(name);
        year.setStartYear(request.startYear() == null ? nameYearStart(name) : request.startYear());
        year.setEndYear(request.endYear() == null ? year.getStartYear() + 1 : request.endYear());
        boolean makeCurrent = !academicYearRepository.findByCurrentTrue().isPresent();
        year.setCurrent(makeCurrent);
        year.setStatus(AccountStatus.ACTIVE);
        AcademicYear saved = academicYearRepository.save(year);
        return ResponseEntity.ok(ApiResponse.success(toAcademicYearItem(saved),
                makeCurrent ? "Academic year created and marked as current" : "Academic year created"));
    }

    @PutMapping("/academic-years/{yearId}/current")
    public ResponseEntity<ApiResponse<AcademicYearItem>> setCurrentYear(@PathVariable Long yearId) {
        AcademicYear year = academicYearRepository.findById(yearId).orElse(null);
        if (year == null) {
            return ResponseEntity.ok(ApiResponse.error("Academic year not found"));
        }
        academicYearRepository.findAll().forEach(y -> {
            if (y.isCurrent()) {
                y.setCurrent(false);
                academicYearRepository.save(y);
            }
        });
        year.setCurrent(true);
        AcademicYear saved = academicYearRepository.save(year);
        return ResponseEntity.ok(ApiResponse.success(toAcademicYearItem(saved), "Academic year marked as current"));
    }

    @GetMapping("/academic-years/{yearId}/courses")
    public ResponseEntity<ApiResponse<List<CourseListItem>>> coursesByYear(@PathVariable Long yearId) {
        List<CourseListItem> courses = courseRepository.findByAcademicYearIdOrderByCodeAsc(yearId)
                .stream()
                .map(this::toCourseItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses, "Courses retrieved"));
    }

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<CourseListItem>> createCourse(@RequestBody CreateCourseRequest request) {
        String code = request.code() == null || request.code().isBlank()
                ? "VE-" + System.currentTimeMillis() : request.code().trim();
        if (courseRepository.findByCode(code).isPresent()) {
            return ResponseEntity.ok(ApiResponse.error("A course with code " + code + " already exists"));
        }
        Course course = new Course();
        course.setCode(code);
        course.setName(request.name() == null ? "Untitled Course" : request.name().trim());
        course.setTerm(request.courseType() == null ? "VE1" : request.courseType().trim());
        course.setStatus("ACTIVE");
        if (request.academicYearId() != null) {
            academicYearRepository.findById(request.academicYearId()).ifPresent(course::setAcademicYear);
        }
        Course saved = courseRepository.save(course);
        return ResponseEntity.ok(ApiResponse.success(toCourseItem(saved), "Course created"));
    }

    @GetMapping("/courses/{courseId}/chapters")
    public ResponseEntity<ApiResponse<List<ChapterItem>>> courseChapters(@PathVariable Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            return ResponseEntity.ok(ApiResponse.error("Course not found"));
        }
        List<ChapterItem> chapters = chapterRepository.findByCourseIdOrderByDisplayOrderAscChapterNumberAsc(courseId)
                .stream()
                .map(ch -> toChapterItem(ch, courseId))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(chapters, "Chapters retrieved"));
    }

    @PostMapping("/courses/{courseId}/chapters")
    public ResponseEntity<ApiResponse<ChapterItem>> createChapter(@PathVariable Long courseId,
                                                                  @RequestBody CreateChapterRequest request) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.ok(ApiResponse.error("Course not found"));
        }
        int nextNumber = (int) (chapterRepository.countByCourseId(courseId) + 1);
        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        chapter.setChapterNumber(nextNumber);
        chapter.setTitle(request.title() == null ? "Chapter " + nextNumber : request.title().trim());
        chapter.setDescription(request.description());
        chapter.setDisplayOrder(nextNumber);
        chapter.setStatus("ACTIVE");
        Chapter saved = chapterRepository.save(chapter);
        return ResponseEntity.ok(ApiResponse.success(toChapterItem(saved, courseId), "Chapter created"));
    }

    @GetMapping("/chapters/{chapterId}/topics")
    public ResponseEntity<ApiResponse<List<TopicItem>>> topics(@PathVariable Long chapterId) {
        return ResponseEntity.ok(ApiResponse.success(
                topicRepository.findByChapterIdAndStatusOrderByDisplayOrderAscIdAsc(chapterId, "ACTIVE")
                        .stream().map(this::toTopicItem).toList(), "Topics retrieved"));
    }

    @PostMapping("/chapters/{chapterId}/topics")
    public ResponseEntity<ApiResponse<TopicItem>> createTopic(@PathVariable Long chapterId,
                                                               @RequestBody CreateTopicRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null) {
            return ResponseEntity.ok(ApiResponse.error("Chapter not found"));
        }
        Topic topic = new Topic();
        topic.setChapter(chapter);
        topic.setTitle(request.title() == null ? "" : request.title().trim());
        topic.setDescription(request.description());
        topic.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        topic.setStatus("ACTIVE");
        return ResponseEntity.ok(ApiResponse.success(toTopicItem(topicRepository.save(topic)), "Topic created"));
    }

    private TopicItem toTopicItem(Topic topic) {
        return new TopicItem(topic.getId(), topic.getTitle(),
                topic.getDescription() == null ? "" : topic.getDescription(),
                topic.getDisplayOrder() == null ? 0 : topic.getDisplayOrder(), topic.getStatus());
    }

    @DeleteMapping("/courses/{courseId}/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<Object>> deleteChapter(@PathVariable Long courseId,
                                                             @PathVariable Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null || !chapter.getCourse().getId().equals(courseId)) {
            return ResponseEntity.ok(ApiResponse.error("Chapter not found"));
        }
        chapterRepository.delete(chapter);
        return ResponseEntity.ok(ApiResponse.success(null, "Chapter deleted"));
    }

    @GetMapping("/courses/{courseId}/faculties")
    public ResponseEntity<ApiResponse<List<FacultyAssignmentItem>>> courseFaculties(@PathVariable Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            return ResponseEntity.ok(ApiResponse.error("Course not found"));
        }
        List<Long> assignedIds = courseFacultyRepository.findByCourseId(courseId)
                .stream().map(CourseFaculty::getFacultyId).toList();
        List<FacultyAssignmentItem> items = userAccountRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FACULTY)
                .map(u -> new FacultyAssignmentItem(u.getId(), u.getFullName(), u.getEmail(),
                        u.getStaffCode(), assignedIds.contains(u.getId())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "Course faculty retrieved"));
    }

    @PostMapping("/courses/{courseId}/faculties")
    public ResponseEntity<ApiResponse<Object>> assignFaculty(@PathVariable Long courseId,
                                                             @RequestBody AssignFacultyRequest request) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.ok(ApiResponse.error("Course not found"));
        }
        List<Long> facultyIds = request.facultyIds() == null ? List.of() : request.facultyIds();
        List<CourseFaculty> current = courseFacultyRepository.findByCourseId(courseId);
        List<Long> currentIds = current.stream().map(CourseFaculty::getFacultyId).toList();
        for (CourseFaculty cf : current) {
            if (!facultyIds.contains(cf.getFacultyId())) {
                courseFacultyRepository.delete(cf);
            }
        }
        for (Long facultyId : facultyIds) {
            if (!currentIds.contains(facultyId) && userAccountRepository.existsById(facultyId)) {
                courseFacultyRepository.save(new CourseFaculty(courseId, facultyId));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Faculty assigned to course"));
    }

    @GetMapping("/courses/{courseId}/students")
    public ResponseEntity<ApiResponse<List<StudentAssignmentItem>>> courseStudents(@PathVariable Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            return ResponseEntity.ok(ApiResponse.error("Course not found"));
        }
        List<Long> assignedIds = courseStudentRepository.findByCourseId(courseId)
                .stream().map(CourseStudent::getStudentId).toList();
        List<StudentAssignmentItem> items = studentRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName"))
                .stream()
                .map(s -> new StudentAssignmentItem(s.getId(), s.getFullName(), s.getStudentCode(),
                        s.getProgramme(), assignedIds.contains(s.getId())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "Course students retrieved"));
    }

    @PostMapping("/courses/{courseId}/students")
    public ResponseEntity<ApiResponse<Object>> assignStudents(@PathVariable Long courseId,
                                                              @RequestBody AssignStudentsRequest request) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.ok(ApiResponse.error("Course not found"));
        }
        List<Long> studentIds = request.studentIds() == null ? List.of() : request.studentIds();
        List<CourseStudent> current = courseStudentRepository.findByCourseId(courseId);
        List<Long> currentIds = current.stream().map(CourseStudent::getStudentId).toList();
        for (CourseStudent cs : current) {
            if (!studentIds.contains(cs.getStudentId())) {
                courseStudentRepository.delete(cs);
            }
        }
        for (Long studentId : studentIds) {
            if (!currentIds.contains(studentId) && studentRepository.existsById(studentId)) {
                courseStudentRepository.save(new CourseStudent(courseId, studentId));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Students assigned to course"));
    }

    private StudentListItem toStudentItem(Student s, Double attendancePercent) {
        return new StudentListItem(
                s.getId(),
                s.getStudentCode(),
                s.getFullName(),
                s.getEmail() == null ? "" : s.getEmail(),
                null,
                s.getProgramme(),
                s.getProgramme(),
                1,
                s.getStatus().name(),
                attendancePercent == null ? 0.0 : attendancePercent
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
    }

    private ChapterItem toChapterItem(Chapter ch, Long courseId) {
        return new ChapterItem(
                ch.getId(),
                ch.getChapterNumber(),
                ch.getTitle(),
                ch.getDescription() == null ? "" : ch.getDescription(),
                ch.getDisplayOrder() == null ? 0 : ch.getDisplayOrder(),
                ch.getStatus() == null ? "ACTIVE" : ch.getStatus(),
                courseId,
                ch.getCourse().getName(),
                ch.getCreatedAt() == null ? "" : ch.getCreatedAt().toString()
        );
    }

    private AcademicYearItem toAcademicYearItem(AcademicYear y) {
        return new AcademicYearItem(
                y.getId(),
                y.getName(),
                y.getStartYear(),
                y.getEndYear(),
                y.isCurrent(),
                y.getStatus() == AccountStatus.ACTIVE,
                y.getCreatedAt() == null ? "" : y.getCreatedAt().toString()
        );
    }

    private int nameYearStart(String name) {
        try {
            return Integer.parseInt(name.trim().substring(0, 4));
        } catch (RuntimeException e) {
            return LocalDate.now().getYear();
        }
    }

    private String branchNameForId(Long branchId) {
        if (branchId == null || branchId <= 0) {
            return "General";
        }
        List<String> programmes = studentRepository.findDistinctProgrammes();
        if (branchId - 1 < programmes.size()) {
            return programmes.get(branchId.intValue() - 1);
        }
        return "General";
    }

    private String academicYearBatchName(Long academicYearId) {
        if (academicYearId != null) {
            Optional<AcademicYear> year = academicYearRepository.findById(academicYearId);
            if (year.isPresent()) {
                return year.get().getStartYear().toString();
            }
        }
        return String.valueOf(LocalDate.now().getYear());
    }

    private String deriveEmail(String name) {
        if (name == null || name.isBlank()) {
            return "student@students.iiit.ac.in";
        }
        String base = name.trim().toLowerCase().replaceAll("\\s+", ".");
        return base + "@students.iiit.ac.in";
    }

    private Map<Long, Double> attendancePercentages(List<Student> students) {
        Map<Long, Double> result = new HashMap<>();
        if (students.isEmpty()) {
            return result;
        }
        List<Long> ids = students.stream().map(Student::getId).toList();
        for (AttendanceRecordRepository.StudentAttendanceAggregation agg :
                attendanceRepository.aggregateByStudents(ids)) {
            Double percent = Percent.of(agg.getPresent(), agg.getTotal());
            if (percent != null) {
                result.put(agg.getStudentId(), percent);
            }
        }
        return result;
    }

    private String generatedStudentCode() {
        long next = studentRepository.count() + 1001;
        return "VE" + next;
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

    public record ChapterItem(
            long id, int chapterNumber, String title, String description,
            int displayOrder, String status, long courseId, String courseName,
            String createdAt
    ) {}

    public record FacultyAssignmentItem(
            long id, String name, String email, String employeeId, boolean assigned
    ) {}

    public record StudentAssignmentItem(
            long id, String name, String rollNumber, String programme, boolean assigned
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
    public record CreateTopicRequest(String title, String description, Integer displayOrder) {}
    public record TopicItem(long id, String title, String description, int displayOrder, String status) {}

    public record AssignFacultyRequest(List<Long> facultyIds) {}

    public record AssignStudentsRequest(List<Long> studentIds) {}
}