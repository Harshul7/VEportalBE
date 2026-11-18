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
import org.example.veportal.config.AppProperties;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.AcademicYear;
import org.example.veportal.entity.Branch;
import org.example.veportal.entity.Chapter;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseFaculty;
import org.example.veportal.entity.CourseStudent;
import org.example.veportal.entity.CourseGroup;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.Topic;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.entity.MasterChapterTemplate;
import org.example.veportal.entity.MasterCourseTemplate;
import org.example.veportal.entity.Room;
import org.example.veportal.entity.AcademicSemester;
import org.example.veportal.entity.CourseCodeMaster;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.AcademicYearRepository;
import org.example.veportal.repository.BranchRepository;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ChapterRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.CourseGroupRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.repository.TopicRepository;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.repository.TeachingLogRepository;
import org.example.veportal.repository.MasterChapterTemplateRepository;
import org.example.veportal.repository.MasterCourseTemplateRepository;
import org.example.veportal.repository.RoomRepository;
import org.example.veportal.repository.AcademicSemesterRepository;
import org.example.veportal.repository.CourseCodeMasterRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.GroupTopicPlanRepository;
import org.example.veportal.repository.GroupTransferHistoryRepository;
import org.example.veportal.repository.TeachingAssistantAssignmentHistoryRepository;
import org.example.veportal.repository.StudentCourseResultRepository;
import org.example.veportal.repository.ParticipationRecordRepository;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.entity.GroupTransferHistory;
import org.example.veportal.entity.TeachingAssistantAssignmentHistory;
import org.example.veportal.entity.StudentCourseResult;
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.service.MailService;
import org.example.veportal.service.AuthService;
import org.example.veportal.util.Percent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.InputStream;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('FACULTY_COORDINATOR','ADMIN')")
public class AdminController {

    private final UserAccountRepository userAccountRepository;
    private final CourseRepository courseRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AcademicYearRepository academicYearRepository;
    private final BranchRepository branchRepository;
    private final ChapterRepository chapterRepository;
    private final CourseFacultyRepository courseFacultyRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final StudentRepository studentRepository;
    private final TopicRepository topicRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuthService authService;
    private final TeachingLogRepository teachingLogRepository;
    private final MasterCourseTemplateRepository masterCourseTemplateRepository;
    private final MasterChapterTemplateRepository masterChapterTemplateRepository;
    private final RoomRepository roomRepository;
    private final AcademicSemesterRepository academicSemesterRepository;
    private final CourseCodeMasterRepository courseCodeMasterRepository;
    private final GroupTransferHistoryRepository groupTransferHistoryRepository;
    private final TeachingAssistantAssignmentHistoryRepository taAssignmentHistoryRepository;
    private final StudentCourseResultRepository studentCourseResultRepository;
    private final ParticipationRecordRepository participationRecordRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final GroupTopicPlanRepository groupTopicPlanRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final AppProperties appProperties;

    public AdminController(UserAccountRepository userAccountRepository,
                           CourseRepository courseRepository,
                           ClassSessionRepository sessionRepository,
                           AttendanceRecordRepository attendanceRepository,
                           AcademicYearRepository academicYearRepository,
                           BranchRepository branchRepository,
                           ChapterRepository chapterRepository,
                           CourseFacultyRepository courseFacultyRepository,
                           CourseStudentRepository courseStudentRepository,
                           CourseGroupRepository courseGroupRepository,
                           StudentRepository studentRepository,
                           TopicRepository topicRepository,
                           PasswordEncoder passwordEncoder,
                           MailService mailService,
                           AuthService authService,
                           TeachingLogRepository teachingLogRepository,
                           MasterCourseTemplateRepository masterCourseTemplateRepository,
                           MasterChapterTemplateRepository masterChapterTemplateRepository,
                           RoomRepository roomRepository,
                           AcademicSemesterRepository academicSemesterRepository,
                           CourseCodeMasterRepository courseCodeMasterRepository,
                           GroupTransferHistoryRepository groupTransferHistoryRepository,
                           TeachingAssistantAssignmentHistoryRepository taAssignmentHistoryRepository,
                           StudentCourseResultRepository studentCourseResultRepository,
                           CourseMaterialRepository courseMaterialRepository,
                           ParticipationRecordRepository participationRecordRepository,
                           GroupTopicPlanRepository groupTopicPlanRepository,
                           AuthenticatedUserProvider authenticatedUserProvider,
                           AppProperties appProperties) {
        this.userAccountRepository = userAccountRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.academicYearRepository = academicYearRepository;
        this.branchRepository = branchRepository;
        this.chapterRepository = chapterRepository;
        this.courseFacultyRepository = courseFacultyRepository;
        this.courseStudentRepository = courseStudentRepository;
        this.courseGroupRepository = courseGroupRepository;
        this.studentRepository = studentRepository;
        this.topicRepository = topicRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.authService = authService;
        this.teachingLogRepository = teachingLogRepository;
        this.masterCourseTemplateRepository = masterCourseTemplateRepository;
        this.masterChapterTemplateRepository = masterChapterTemplateRepository;
        this.roomRepository = roomRepository;
        this.academicSemesterRepository = academicSemesterRepository;
        this.courseCodeMasterRepository = courseCodeMasterRepository;
        this.groupTransferHistoryRepository = groupTransferHistoryRepository;
        this.taAssignmentHistoryRepository = taAssignmentHistoryRepository;
        this.studentCourseResultRepository = studentCourseResultRepository;
        this.courseMaterialRepository = courseMaterialRepository;
        this.participationRecordRepository = participationRecordRepository;
        this.groupTopicPlanRepository = groupTopicPlanRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.appProperties = appProperties;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> dashboard(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        var filteredCourses = courseRepository.findAll().stream()
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .filter(c -> academicYearId == null || (c.getAcademicYear() != null && academicYearId.equals(c.getAcademicYear().getId())))
                .filter(c -> courseId == null || courseId.equals(c.getId())).toList();
        var filteredCourseIds = filteredCourses.stream().map(Course::getId).collect(java.util.stream.Collectors.toSet());
        var filteredGroups = courseGroupRepository.findAll().stream().filter(g -> filteredCourseIds.contains(g.getCourse().getId())).filter(g -> groupId == null || groupId.equals(g.getId())).toList();
        var filteredGroupIds = filteredGroups.stream().map(CourseGroup::getId).collect(java.util.stream.Collectors.toSet());
        List<Long> filteredStudentIds = (groupId != null
                ? courseStudentRepository.findByGroupIdIn(filteredGroupIds)
                : filteredCourseIds.stream().flatMap(id -> courseStudentRepository.findByCourseId(id).stream()).toList())
                .stream().map(CourseStudent::getStudentId).distinct().toList();
        long totalStudents = filteredStudentIds.size();
        long totalFaculty = userAccountRepository.findAll().stream()
                .filter(u -> u.getRole().isMentor() || u.getRole() == Role.TA)
                .count();
        var filteredSessions = sessionRepository.findAll().stream().filter(s -> filteredCourseIds.contains(s.getCourse().getId())).filter(s -> groupId == null || (s.getGroup() != null && filteredGroupIds.contains(s.getGroup().getId()))).filter(s -> fromDate == null || !s.getSessionDate().isBefore(fromDate)).filter(s -> toDate == null || !s.getSessionDate().isAfter(toDate)).toList();
        long activeCourses = filteredCourses.size();
        long classesConducted = filteredSessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
        long upcomingSessions = filteredSessions.stream().filter(s -> s.getStatus() == SessionStatus.SCHEDULED || s.getStatus() == SessionStatus.UPCOMING).count();
        List<Long> sessionIds = filteredSessions.stream().map(s -> s.getId()).toList();
        long sessionsWithLogs = sessionIds.isEmpty() ? 0 : teachingLogRepository.findSessionIdsWithLogs(sessionIds).stream().distinct().count();
        long pendingReports = Math.max(0, classesConducted - sessionsWithLogs);
        long totalAttendance = filteredSessions.stream().flatMap(s -> attendanceRepository.findWithStudentBySessionId(s.getId()).stream()).count();
        long presentAttendance = filteredSessions.stream().flatMap(s -> attendanceRepository.findWithStudentBySessionId(s.getId()).stream()).filter(a -> a.getStatus() == org.example.veportal.entity.AttendanceStatus.PRESENT).count();
        double attendancePercent = totalAttendance == 0 ? 0.0 : presentAttendance * 100.0 / totalAttendance;
        AcademicYear current = academicYearRepository.findByCurrentTrue().orElse(null);
        long totalChapters = chapterRepository.count();
        String currentYear = current != null ? current.getName() : "2025-2026";

        DashboardStats stats = new DashboardStats(
                totalStudents, totalFaculty, totalChapters, classesConducted,
                attendancePercent, currentYear, activeCourses, totalChapters,
                upcomingSessions, classesConducted, pendingReports,
                filteredGroups.size(), filteredGroups.stream().filter(g -> g.getMentor() == null).count(), filteredGroups.stream().filter(g -> g.getTeachingAssistant() == null).count(),
                userAccountRepository.findAll().stream().filter(u -> u.getRole().isMentor() && u.getStatus() == AccountStatus.ACTIVE).count(), userAccountRepository.findAll().stream().filter(u -> u.getRole() == Role.TA && u.getStatus() == AccountStatus.ACTIVE).count(),
                studentCourseResultRepository.findAll().stream().filter(r -> filteredCourseIds.contains(r.getCourse().getId()) && filteredStudentIds.contains(r.getStudent().getId()) && r.getPublishedAt() != null && r.getFinalResult() == StudentCourseResult.FinalResult.PASS).count(),
                studentCourseResultRepository.findAll().stream().filter(r -> filteredCourseIds.contains(r.getCourse().getId()) && filteredStudentIds.contains(r.getStudent().getId()) && r.getPublishedAt() != null && r.getFinalResult() == StudentCourseResult.FinalResult.FAIL).count()
        );
        return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard stats retrieved"));
    }

    @GetMapping("/students")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<StudentListItem>>> listStudents(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(name = "academicYearId", required = false) Long academicYearId,
            @RequestParam(name = "groupId", required = false) Long groupId,
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Specification<Student> spec = (root, query, cb) -> cb.conjunction();
        if (!includeInactive) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.equal(root.get("status"), AccountStatus.ACTIVE),
                    cb.isNull(root.get("status"))));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("studentCode")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)));
        }
        if (branchId != null && branchId > 0) {
            Branch branch = branchRepository.findById(branchId).orElse(null);
            if (branch != null) {
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.equal(root.get("branchCode"), branch.getCode()),
                        cb.equal(root.get("programme"), branch.getCode())));
            }
        }
        if (academicYearId != null && academicYearId > 0) {
            spec = spec.and((root, query, cb) -> {
                var enrollmentQuery = query.subquery(Long.class);
                var enrollment = enrollmentQuery.from(CourseStudent.class);
                enrollmentQuery.select(cb.literal(1L));
                enrollmentQuery.where(
                        cb.equal(enrollment.get("studentId"), root.get("id")),
                        cb.equal(enrollment.get("course").get("academicYear").get("id"), academicYearId)
                );
                return cb.exists(enrollmentQuery);
            });
        }
        if (groupId != null && groupId > 0) {
            spec = spec.and((root, query, cb) -> {
                var enrollmentQuery = query.subquery(Long.class);
                var enrollment = enrollmentQuery.from(CourseStudent.class);
                enrollmentQuery.select(cb.literal(1L));
                enrollmentQuery.where(
                        cb.equal(enrollment.get("studentId"), root.get("id")),
                        cb.equal(enrollment.get("groupId"), groupId)
                );
                return cb.exists(enrollmentQuery);
            });
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
    @Transactional
    public ResponseEntity<ApiResponse<StudentListItem>> createStudent(@RequestBody CreateStudentRequest request) {
        if (request.rollNumber() == null || request.rollNumber().isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("Student ID is required"));
        }
        String code = request.rollNumber().trim();
        if (studentRepository.existsByStudentCodeIgnoreCase(code)) {
            return ResponseEntity.ok(ApiResponse.error("A student with roll number " + code + " already exists"));
        }
        Course selectedCourse = null;
        if (request.courseId() != null) {
            selectedCourse = courseRepository.findById(request.courseId()).orElse(null);
            if (selectedCourse == null) return ResponseEntity.ok(ApiResponse.error("Selected course was not found"));
            if (request.academicYearId() == null || selectedCourse.getAcademicYear() == null || !selectedCourse.getAcademicYear().getId().equals(request.academicYearId())) {
                return ResponseEntity.ok(ApiResponse.error("Selected course does not belong to the selected academic year"));
            }
            if (request.groupId() == null || validGroupId(request.courseId(), request.groupId()) == null) {
                return ResponseEntity.ok(ApiResponse.error("Select a valid group for the selected course"));
            }
        }
        Student s = new Student();
        s.setStudentCode(code);
        s.setFullName(request.name() == null ? "" : request.name().trim());
        s.setEmail(request.email() == null || request.email().isBlank()
                ? deriveEmail(request.name()) : request.email().trim());
        s.setProgramme(branchNameForId(request.branchId()));
        s.setBranchCode(branchCodeForId(request.branchId()));
        s.setStatus(AccountStatus.ACTIVE);
        s.setBatch(academicYearBatchName(request.academicYearId()));
        if (request.facultyId() != null) {
            UserAccount faculty = userAccountRepository.findById(request.facultyId()).orElse(null);
            if (faculty == null || !faculty.getRole().isMentor()) {
                return ResponseEntity.ok(ApiResponse.error("Selected professor was not found"));
            }
            s.setFaculty(faculty);
        }
        Student saved = studentRepository.save(s);
        if (request.courseId() != null) {
            Long groupId = validGroupId(request.courseId(), request.groupId());
            courseStudentRepository.save(new CourseStudent(request.courseId(), saved.getId(), groupId));
        }
        return ResponseEntity.ok(ApiResponse.success(toStudentItem(saved, null), "Student created"));
    }

    @DeleteMapping("/students/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deleteStudent(@PathVariable Long id) {
        Student student = studentRepository.findById(id).orElse(null);
        if (student == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Student not found"));

        boolean hasAcademicHistory = hasProtectedAcademicHistory(id);
        if (hasAcademicHistory) {
            student.setStatus(AccountStatus.INACTIVE);
            studentRepository.save(student);
            return ResponseEntity.ok(ApiResponse.success(null, "Student has academic history and was deactivated instead of deleted"));
        }
        courseStudentRepository.deleteAll(courseStudentRepository.findByStudentId(id));
        studentRepository.delete(student);
        return ResponseEntity.ok(ApiResponse.success(null, "Student deleted"));
    }

    @PostMapping("/students/bulk-delete")
    @Transactional
    public ResponseEntity<ApiResponse<BulkStudentDeleteResult>> bulkDeleteStudents(@RequestBody BulkStudentDeleteRequest request) {
        if (request == null || request.studentIds() == null || request.studentIds().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Select at least one student"));
        }

        List<Long> ids = request.studentIds().stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Select at least one valid student"));
        }

        int deleted = 0;
        int deactivated = 0;
        List<Long> missing = new ArrayList<>();
        for (Long id : ids) {
            Student student = studentRepository.findById(id).orElse(null);
            if (student == null) {
                missing.add(id);
                continue;
            }

            if (hasProtectedAcademicHistory(id)) {
                student.setStatus(AccountStatus.INACTIVE);
                studentRepository.save(student);
                deactivated++;
            } else {
                courseStudentRepository.deleteAll(courseStudentRepository.findByStudentId(id));
                studentRepository.delete(student);
                deleted++;
            }
        }

        BulkStudentDeleteResult result = new BulkStudentDeleteResult(ids.size(), deleted, deactivated, missing);
        return ResponseEntity.ok(ApiResponse.success(result, "Selected students processed"));
    }

    private boolean hasProtectedAcademicHistory(Long studentId) {
        return attendanceRepository.existsByStudentId(studentId)
                || participationRecordRepository.existsByStudentId(studentId)
                || !studentCourseResultRepository.findByStudentId(studentId).isEmpty()
                || !groupTransferHistoryRepository.findByStudentIdOrderByTransferredAtDesc(studentId).isEmpty();
    }

    @PostMapping(value = "/students/import", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<StudentImportResult>> importStudents(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long groupId) {
        if (file.isEmpty()) return ResponseEntity.ok(ApiResponse.error("Excel file is required"));
        UserAccount faculty = facultyId == null ? null : userAccountRepository.findById(facultyId).orElse(null);
        if (facultyId != null && (faculty == null || !faculty.getRole().isMentor())) {
            return ResponseEntity.ok(ApiResponse.error("Selected professor was not found"));
        }
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        try (InputStream input = file.getInputStream(); var workbook = WorkbookFactory.create(input)) {
            var sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() < 1) return ResponseEntity.ok(ApiResponse.error("Excel file has no student rows"));
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> headers = new HashMap<>();
            Row header = sheet.getRow(0);
            for (Cell cell : header) headers.put(formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT), cell.getColumnIndex());
            int idColumn = firstColumn(headers, "student id", "student_id", "student code", "student_code", "roll number", "roll_number");
            int nameColumn = firstColumn(headers, "name", "student name", "student_name");
            if (idColumn < 0 || nameColumn < 0) return ResponseEntity.ok(ApiResponse.error("Excel must contain Student ID and Name columns"));
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                String code = formatter.formatCellValue(row.getCell(idColumn)).trim();
                String name = formatter.formatCellValue(row.getCell(nameColumn)).trim();
                if (code.isBlank() && name.isBlank()) continue;
                if (code.isBlank() || name.isBlank()) { skipped++; errors.add("Row " + (rowIndex + 1) + ": Student ID and Name are required"); continue; }
                if (studentRepository.existsByStudentCodeIgnoreCase(code)) { skipped++; errors.add("Row " + (rowIndex + 1) + ": duplicate Student ID " + code); continue; }
                Student student = new Student();
                student.setStudentCode(code);
                student.setFullName(name);
                int emailColumn = firstColumn(headers, "email", "student email");
                String email = emailColumn < 0 ? deriveEmail(name) : formatter.formatCellValue(row.getCell(emailColumn)).trim();
                student.setEmail(email.isBlank() ? deriveEmail(name) : email);
                int programmeColumn = firstColumn(headers, "programme", "program", "branch");
                student.setProgramme(programmeColumn < 0 ? "General" : value(formatter, row, programmeColumn, "General"));
                student.setBranchCode(programmeColumn < 0 ? null : value(formatter, row, programmeColumn, null));
                student.setBatch(academicYearBatchName(academicYearId));
                student.setStatus(AccountStatus.ACTIVE);
                if (faculty != null) student.setFaculty(faculty);
                studentRepository.save(student);
                if (courseId != null && courseRepository.existsById(courseId)) {
                    courseStudentRepository.save(new CourseStudent(courseId, student.getId(), validGroupId(courseId, groupId)));
                }
                imported++;
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Unable to read Excel file: " + e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(new StudentImportResult(imported, skipped, errors),
                "Student import completed"));
    }

    private int firstColumn(Map<String, Integer> headers, String... names) {
        for (String name : names) if (headers.containsKey(name)) return headers.get(name);
        return -1;
    }

    private String value(DataFormatter formatter, Row row, int column, String fallback) {
        String value = formatter.formatCellValue(row.getCell(column)).trim();
        return value.isBlank() ? fallback : value;
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<ApiResponse<StudentListItem>> updateStudent(@PathVariable Long id,
                                                                      @RequestBody CreateStudentRequest request) {
        Student s = studentRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.ok(ApiResponse.error("Student not found"));
        }
        if (request.rollNumber() != null && !request.rollNumber().isBlank()) {
            if (studentRepository.findByStudentCodeIgnoreCase(request.rollNumber().trim())
                    .filter(existing -> !existing.getId().equals(id)).isPresent()) {
                return ResponseEntity.ok(ApiResponse.error("A student with Student ID " + request.rollNumber().trim() + " already exists"));
            }
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
            s.setBranchCode(branchCodeForId(request.branchId()));
        }
        if (request.academicYearId() != null) {
            s.setBatch(academicYearBatchName(request.academicYearId()));
        }
        Student saved = studentRepository.save(s);
        if (request.courseId() != null && courseRepository.existsById(request.courseId())
                && !courseStudentRepository.existsByCourseIdAndStudentId(request.courseId(), saved.getId())) {
            Long groupId = validGroupId(request.courseId(), request.groupId());
            if (request.groupId() != null && groupId == null) return ResponseEntity.ok(ApiResponse.error("Selected group does not belong to this course"));
            courseStudentRepository.save(new CourseStudent(request.courseId(), saved.getId(), groupId));
        }
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

    @GetMapping("/students/{id}/group-transfers")
    public ResponseEntity<ApiResponse<List<GroupTransferItem>>> groupTransfers(@PathVariable Long id) {
        if (!studentRepository.existsById(id)) return ResponseEntity.ok(ApiResponse.error("Student not found"));
        List<GroupTransferItem> items = groupTransferHistoryRepository.findByStudentIdOrderByTransferredAtDesc(id).stream()
                .map(h -> new GroupTransferItem(h.getId(), h.getCourse().getId(), h.getCourse().getCode(),
                        h.getPreviousGroup() == null ? null : h.getPreviousGroup().getId(),
                        h.getPreviousGroup() == null ? null : h.getPreviousGroup().getName(),
                        h.getNewGroup().getId(), h.getNewGroup().getName(), h.getTransferredAt().toString(),
                        h.getReason(), h.getPerformedBy().getFullName())).toList();
        return ResponseEntity.ok(ApiResponse.success(items, "Group transfer history retrieved"));
    }

    public record BulkStudentDeleteRequest(List<Long> studentIds) {}
    public record BulkStudentDeleteResult(int requested, int deleted, int deactivated, List<Long> missing) {}

    @PostMapping("/students/{id}/group-transfers")
    @Transactional
    public ResponseEntity<ApiResponse<GroupTransferItem>> transferStudent(@PathVariable Long id,
            @RequestBody TransferStudentRequest request) {
        Student student = studentRepository.findById(id).orElse(null);
        Course course = request.courseId() == null ? null : courseRepository.findById(request.courseId()).orElse(null);
        CourseGroup target = request.targetGroupId() == null ? null : courseGroupRepository.findById(request.targetGroupId()).orElse(null);
        if (student == null || course == null || target == null || !target.getCourse().getId().equals(course.getId())) {
            return ResponseEntity.ok(ApiResponse.error("Student, course, and target group must be valid and related"));
        }
        String reason = request.reason() == null ? "Administrative group transfer" : request.reason().trim();
        if (reason.isBlank()) return ResponseEntity.ok(ApiResponse.error("Transfer reason is required"));
        CourseStudent mapping = courseStudentRepository.findByCourseId(request.courseId()).stream()
                .filter(value -> value.getStudentId().equals(id)).findFirst().orElse(null);
        if (mapping == null) return ResponseEntity.ok(ApiResponse.error("Student is not enrolled in this course offering"));
        if (target.getId().equals(mapping.getGroupId())) return ResponseEntity.ok(ApiResponse.error("Student is already in the selected group"));
        CourseGroup previous = mapping.getGroupId() == null ? null : courseGroupRepository.findById(mapping.getGroupId()).orElse(null);
        mapping.setGroupId(target.getId());
        courseStudentRepository.save(mapping);
        GroupTransferHistory history = new GroupTransferHistory();
        history.setStudent(student); history.setCourse(course); history.setPreviousGroup(previous); history.setNewGroup(target);
        history.setTransferredAt(LocalDateTime.now()); history.setReason(reason); history.setPerformedBy(authenticatedUserProvider.currentUser());
        GroupTransferHistory saved = groupTransferHistoryRepository.save(history);
        return ResponseEntity.ok(ApiResponse.success(new GroupTransferItem(saved.getId(), course.getId(), course.getCode(),
                previous == null ? null : previous.getId(), previous == null ? null : previous.getName(), target.getId(), target.getName(),
                saved.getTransferredAt().toString(), reason, saved.getPerformedBy().getFullName()), "Student transferred successfully"));
    }

    @PostMapping("/course-groups/{groupId}/students/transfer")
    @Transactional
    public ResponseEntity<ApiResponse<BulkGroupTransferResult>> transferStudentsToGroup(
            @PathVariable Long groupId, @RequestBody BulkGroupTransferRequest request) {
        CourseGroup target = courseGroupRepository.findById(groupId).orElse(null);
        List<Long> studentIds = request.studentIds() == null ? List.of() : request.studentIds().stream().distinct().toList();
        if (target == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Target group not found"));
        if (studentIds.isEmpty()) return ResponseEntity.ok(ApiResponse.error("Select at least one student to move"));
        String reason = request.reason() == null || request.reason().isBlank()
                ? "Administrative bulk group transfer" : request.reason().trim();

        List<CourseStudent> mappings = courseStudentRepository.findByCourseId(target.getCourse().getId()).stream()
                .filter(mapping -> studentIds.contains(mapping.getStudentId()))
                .toList();
        if (mappings.size() != studentIds.size()) {
            return ResponseEntity.ok(ApiResponse.error("Every selected student must be enrolled in this course offering"));
        }

        UserAccount actor = authenticatedUserProvider.currentUser();
        int moved = 0;
        for (CourseStudent mapping : mappings) {
            if (groupId.equals(mapping.getGroupId())) continue;
            CourseGroup previous = mapping.getGroupId() == null ? null
                    : courseGroupRepository.findById(mapping.getGroupId()).orElse(null);
            Student student = studentRepository.findById(mapping.getStudentId()).orElse(null);
            if (student == null) continue;
            mapping.setGroupId(groupId);
            courseStudentRepository.save(mapping);

            GroupTransferHistory history = new GroupTransferHistory();
            history.setStudent(student);
            history.setCourse(target.getCourse());
            history.setPreviousGroup(previous);
            history.setNewGroup(target);
            history.setTransferredAt(LocalDateTime.now());
            history.setReason(reason);
            history.setPerformedBy(actor);
            groupTransferHistoryRepository.save(history);
            moved++;
        }
        return ResponseEntity.ok(ApiResponse.success(
                new BulkGroupTransferResult(studentIds.size(), moved, studentIds.size() - moved, target.getId(), target.getName()),
                moved == 0 ? "All selected students are already in this group" : "Students moved successfully"));
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
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<FacultyListItem>>> listFaculty(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "includeInactive", defaultValue = "true") boolean includeInactive,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        List<FacultyListItem> faculty = userAccountRepository.findAll().stream()
                .filter(u -> u.getRole().isMentor() || u.getRole() == Role.TA)
                .filter(u -> search == null || search.isBlank()
                        || u.getFullName().toLowerCase().contains(search.toLowerCase())
                        || u.getEmail().toLowerCase().contains(search.toLowerCase()))
                .filter(u -> includeInactive || u.getStatus() == AccountStatus.ACTIVE)
                .map(this::toFacultyItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(faculty, "Faculty retrieved"));
    }

    @PostMapping("/faculty")
    @Transactional
    public ResponseEntity<ApiResponse<FacultyListItem>> createFaculty(@RequestBody CreateFacultyRequest request) {
        String email = request.email() == null ? null : request.email().trim();
        if (request.employeeId() == null || request.employeeId().isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("Employee ID is required"));
        }
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("Email is required"));
        }
        String instituteRole = request.department() == null ? "" : request.department().trim();
        if (!List.of("Faculty", "Staff (Full Time)", "Staff (Part-time)", "PhD Student")
                .contains(instituteRole)) {
            return ResponseEntity.ok(ApiResponse.error("Select a valid institute role"));
        }
        if (request.academicYearId() != null && !academicYearRepository.existsById(request.academicYearId())) {
            return ResponseEntity.ok(ApiResponse.error("Selected academic year was not found"));
        }
        if (request.semesterId() != null) {
            AcademicSemester selectedSemester = academicSemesterRepository.findById(request.semesterId()).orElse(null);
            if (selectedSemester == null || (request.academicYearId() != null
                    && !selectedSemester.getAcademicYear().getId().equals(request.academicYearId()))) {
                return ResponseEntity.ok(ApiResponse.error("Selected semester does not belong to the selected academic year"));
            }
            if (request.courseId() != null) {
                Course selectedCourse = courseRepository.findById(request.courseId()).orElse(null);
                if (selectedCourse == null || selectedCourse.getSemesterRef() == null
                        || !selectedCourse.getSemesterRef().getId().equals(selectedSemester.getId())) {
                    return ResponseEntity.ok(ApiResponse.error("Selected semester does not match the mapped course"));
                }
            }
        }
        if (request.courseId() != null && request.academicYearId() != null) {
            Course selectedCourse = courseRepository.findById(request.courseId()).orElse(null);
            if (selectedCourse == null || selectedCourse.getAcademicYear() == null
                    || !selectedCourse.getAcademicYear().getId().equals(request.academicYearId())) {
                return ResponseEntity.ok(ApiResponse.error("Mapped course does not belong to the selected academic year"));
            }
        }
        boolean teachingAssistant = "TA".equalsIgnoreCase(request.role());
        if (!teachingAssistant && request.academicYearId() == null) {
            return ResponseEntity.ok(ApiResponse.error("Group Mentor onboarding requires an academic year"));
        }
        Branch selectedBranch = request.branchId() == null ? null : branchRepository.findById(request.branchId()).orElse(null);
        if (request.branchId() != null && (selectedBranch == null || !selectedBranch.isActive())) {
            return ResponseEntity.ok(ApiResponse.error("Selected branch was not found or is inactive"));
        }
        if (selectedBranch != null && request.academicYearId() != null
                && selectedBranch.getAcademicYear() != null
                && !selectedBranch.getAcademicYear().getId().equals(request.academicYearId())) {
            return ResponseEntity.ok(ApiResponse.error("Selected branch does not belong to the selected academic year"));
        }
        String employeeId = request.employeeId() == null ? null : request.employeeId().trim();
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.ok(ApiResponse.error("A user with email " + email + " already exists"));
        }
        if (employeeId != null && !employeeId.isBlank() && userAccountRepository.existsByStaffCodeIgnoreCase(employeeId)) {
            return ResponseEntity.ok(ApiResponse.error("A professor with Employee ID " + employeeId + " already exists"));
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setFullName(request.name() == null ? "" : request.name().trim());
        user.setStaffCode(employeeId == null || employeeId.isBlank()
                ? generatedStaffCode() : request.employeeId().trim());
        user.setDepartment(instituteRole);
        user.setAcademicYear(request.academicYearId() == null ? null
                : academicYearRepository.findById(request.academicYearId()).orElse(null));
        user.setBranch(selectedBranch);
        user.setRole("TA".equalsIgnoreCase(request.role()) ? Role.TA : Role.GROUP_MENTOR);
        user.setStatus(AccountStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
        user.setMustChangePassword(true);
        if ("TA".equalsIgnoreCase(request.role()) && (request.courseId() == null || request.groupId() == null)) {
            return ResponseEntity.ok(ApiResponse.error("Teaching Assistants must be mapped to a course group during onboarding"));
        }
        if ("TA".equalsIgnoreCase(request.role()) && request.courseId() != null && request.groupId() != null) {
            CourseGroup selectedGroup = courseGroupRepository.findById(request.groupId()).orElse(null);
            if (selectedGroup == null || !selectedGroup.getCourse().getId().equals(request.courseId())) {
                return ResponseEntity.ok(ApiResponse.error("Selected group does not belong to this course"));
            }
            if (selectedGroup.getTeachingAssistant() != null) {
                return ResponseEntity.ok(ApiResponse.error("This group already has an active Teaching Assistant. Reassign it from the group assignment workflow."));
            }
        }
        UserAccount saved = userAccountRepository.save(user);
        if (request.courseId() != null) {
            if (!courseRepository.existsById(request.courseId())) {
                return ResponseEntity.ok(ApiResponse.error("Selected course was not found"));
            }
            Long groupId = validGroupId(request.courseId(), request.groupId());
            if (request.groupId() != null && groupId == null) return ResponseEntity.ok(ApiResponse.error("Selected group does not belong to this course"));
            courseFacultyRepository.save(new CourseFaculty(request.courseId(), saved.getId(), groupId));
            if (groupId != null && saved.getRole().isMentor()) {
                CourseGroup group = courseGroupRepository.findById(groupId).orElseThrow();
                group.setMentor(saved); courseGroupRepository.save(group);
            } else if (groupId != null && saved.getRole() == Role.TA) {
                CourseGroup group = courseGroupRepository.findById(groupId).orElseThrow();
                group.setTeachingAssistant(saved); courseGroupRepository.save(group);
                recordTaAssignment(group, saved, "Initial TA onboarding");
            }
        }
        authService.issuePasswordSetup(saved);
        return ResponseEntity.ok(ApiResponse.success(
                toFacultyItem(saved), "Faculty created; an activation link was sent by email"));
    }

    @PutMapping("/faculty/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<FacultyListItem>> updateFaculty(@PathVariable Long id,
                                                                       @RequestBody CreateFacultyRequest request) {
        UserAccount user = userAccountRepository.findById(id).orElse(null);
        if (user == null || !(user.getRole().isMentor() || user.getRole() == Role.TA)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Profile not found"));
        }
        String email = request.email() == null ? "" : request.email().trim();
        String employeeId = request.employeeId() == null ? "" : request.employeeId().trim();
        String name = request.name() == null ? "" : request.name().trim();
        String department = request.department() == null ? "" : request.department().trim();
        if (name.isBlank() || email.isBlank() || employeeId.isBlank() || department.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("Name, email, employee ID and institute role are required"));
        }
        Optional<UserAccount> sameEmail = userAccountRepository.findByEmailIgnoreCase(email);
        if (sameEmail.isPresent() && !sameEmail.get().getId().equals(id)) {
            return ResponseEntity.ok(ApiResponse.error("A user with email " + email + " already exists"));
        }
        Optional<UserAccount> sameStaffCode = userAccountRepository.findByStaffCodeIgnoreCase(employeeId);
        if (sameStaffCode.isPresent() && !sameStaffCode.get().getId().equals(id)) {
            return ResponseEntity.ok(ApiResponse.error("A user with Employee ID " + employeeId + " already exists"));
        }
        user.setFullName(name);
        user.setEmail(email);
        user.setStaffCode(employeeId);
        user.setDepartment(department);
        if (request.academicYearId() != null && !academicYearRepository.existsById(request.academicYearId())) {
            return ResponseEntity.ok(ApiResponse.error("Selected academic year was not found"));
        }
        Branch selectedBranch = request.branchId() == null ? null : branchRepository.findById(request.branchId()).orElse(null);
        if (request.branchId() != null && (selectedBranch == null || !selectedBranch.isActive())) {
            return ResponseEntity.ok(ApiResponse.error("Selected branch was not found or is inactive"));
        }
        if (selectedBranch != null && request.academicYearId() != null
                && selectedBranch.getAcademicYear() != null
                && !selectedBranch.getAcademicYear().getId().equals(request.academicYearId())) {
            return ResponseEntity.ok(ApiResponse.error("Selected branch does not belong to the selected academic year"));
        }
        if (request.academicYearId() != null) user.setAcademicYear(academicYearRepository.findById(request.academicYearId()).orElse(null));
        // Branch is optional metadata for staff accounts. A group can contain
        // students from multiple branches, so it must never constrain the
        // mentor's group assignment or access scope. Setting the value even
        // when null also lets the coordinator clear an old branch mapping.
        user.setBranch(selectedBranch);
        return ResponseEntity.ok(ApiResponse.success(toFacultyItem(userAccountRepository.save(user)), "Profile updated"));
    }

    @DeleteMapping("/faculty/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<FacultyListItem>> deleteFaculty(@PathVariable Long id) {
        UserAccount user = userAccountRepository.findById(id).orElse(null);
        if (user == null || !(user.getRole().isMentor() || user.getRole() == Role.TA)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Profile not found"));
        }
        courseGroupRepository.findAll().stream()
                .filter(group -> (group.getMentor() != null && group.getMentor().getId().equals(id))
                        || (group.getTeachingAssistant() != null && group.getTeachingAssistant().getId().equals(id)))
                .forEach(group -> {
                    if (group.getMentor() != null && group.getMentor().getId().equals(id)) group.setMentor(null);
                    if (group.getTeachingAssistant() != null && group.getTeachingAssistant().getId().equals(id)) group.setTeachingAssistant(null);
                    courseGroupRepository.save(group);
                });
        user.setStatus(AccountStatus.INACTIVE);
        return ResponseEntity.ok(ApiResponse.success(toFacultyItem(userAccountRepository.save(user)), "Profile deleted and account deactivated"));
    }

    @PostMapping("/faculty/{id}/account")
    public ResponseEntity<ApiResponse<FacultyListItem>> createFacultyAccount(@PathVariable Long id,
                                                                             @RequestBody AccountCreateRequest request) {
        UserAccount user = userAccountRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error("Faculty not found"));
        }
        if (request.role() != null && ("FACULTY_COORDINATOR".equalsIgnoreCase(request.role()) || "ADMIN".equalsIgnoreCase(request.role()))) {
            user.setRole(Role.FACULTY_COORDINATOR);
        } else if (request.role() != null && "TA".equalsIgnoreCase(request.role())) {
            user.setRole(Role.TA);
        } else {
            user.setRole(Role.GROUP_MENTOR);
        }
        user.setStatus(AccountStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
        user.setMustChangePassword(true);
        UserAccount saved = userAccountRepository.save(user);
        if (request.sendEmail() == null || request.sendEmail()) {
            authService.issuePasswordSetup(saved);
        }
        return ResponseEntity.ok(ApiResponse.success(
                toFacultyItem(saved), "Account activated; an activation link was sent by email"));
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
    public ResponseEntity<ApiResponse<PasswordResetResult>> resetFacultyPassword(@PathVariable Long id) {
        UserAccount user = userAccountRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error("Faculty not found"));
        }
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        UserAccount saved = userAccountRepository.save(user);
        authService.issuePasswordSetup(user);
        String visiblePassword = appProperties.security().allowTemporaryPasswordDisplay()
                ? temporaryPassword : null;
        return ResponseEntity.ok(ApiResponse.success(
                new PasswordResetResult(saved.getEmail(), visiblePassword),
                visiblePassword == null
                        ? "Password setup link sent by email"
                        : "Temporary password generated. It must be changed at first login."));
    }

    public record PasswordResetResult(String email, String temporaryPassword) {}

    @GetMapping("/branches")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<BranchItem>>> listBranches(
            @RequestParam(name = "academicYearId", required = false) Long academicYearId) {
        List<BranchItem> branches = branchRepository.findByActiveTrueOrderByCodeAsc().stream()
                .filter(branch -> academicYearId == null || branch.getAcademicYear() == null
                        || branch.getAcademicYear().getId().equals(academicYearId))
                .map(b -> new BranchItem(b.getId(), b.getCode(), b.getCode(), b.isActive(),
                        b.getCreatedAt() == null ? "" : b.getCreatedAt().toString(),
                        b.getAcademicYear() == null ? null : b.getAcademicYear().getId(),
                        b.getAcademicYear() == null ? null : b.getAcademicYear().getName())).toList();
        return ResponseEntity.ok(ApiResponse.success(branches, "Branches retrieved"));
    }

    @PostMapping("/branches")
    public ResponseEntity<ApiResponse<BranchItem>> createBranch(@RequestBody CreateBranchRequest request) {
        AcademicYear academicYear = request.academicYearId() == null ? null
                : academicYearRepository.findById(request.academicYearId()).orElse(null);
        if (request.academicYearId() != null && academicYear == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Academic year not found"));
        }
        if (request.code() == null || request.code().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Branch short code is required"));
        }
        String code = request.code().trim().toUpperCase();
        String name = code;
        if (code.length() > 20 || name.length() > 120) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Branch code or name is too long"));
        }
        if (branchRepository.findByCodeIgnoreCase(code).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("A branch with this code already exists"));
        }
        Branch branch = new Branch();
        branch.setCode(code.toUpperCase());
        branch.setName(name);
        branch.setActive(true);
        branch.setAcademicYear(academicYear);
        Branch saved = branchRepository.save(branch);
        return ResponseEntity.ok(ApiResponse.success(new BranchItem(saved.getId(), saved.getCode(), saved.getCode(),
                saved.isActive(), saved.getCreatedAt() == null ? "" : saved.getCreatedAt().toString(),
                academicYear == null ? null : academicYear.getId(), academicYear == null ? null : academicYear.getName()), "Branch created"));
    }

    @PutMapping("/branches/{branchId}")
    public ResponseEntity<ApiResponse<BranchItem>> updateBranch(@PathVariable Long branchId,
                                                                  @RequestBody CreateBranchRequest request) {
        Branch branch = branchRepository.findById(branchId).orElse(null);
        if (branch == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Branch not found"));
        String code = request.code() == null ? branch.getCode() : request.code().trim().toUpperCase();
        String name = code;
        if (name.isBlank() || code.isBlank() || code.length() > 20 || name.length() > 120) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid branch code and name are required"));
        }
        var duplicate = branchRepository.findByCodeIgnoreCase(code);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(branchId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("A branch with this code already exists"));
        }
        AcademicYear year = request.academicYearId() == null ? null
                : academicYearRepository.findById(request.academicYearId()).orElse(null);
        if (request.academicYearId() != null && year == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Academic year not found"));
        }
        branch.setCode(code); branch.setName(name); branch.setAcademicYear(year);
        Branch saved = branchRepository.save(branch);
        return ResponseEntity.ok(ApiResponse.success(toBranchItem(saved), "Branch updated"));
    }

    @DeleteMapping("/branches/{branchId}")
    public ResponseEntity<ApiResponse<Object>> archiveBranch(@PathVariable Long branchId) {
        Branch branch = branchRepository.findById(branchId).orElse(null);
        if (branch == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Branch not found"));
        if (studentRepository.findAll().stream().anyMatch(student -> branch.getCode().equalsIgnoreCase(student.getBranchCode()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("This branch is used by students and can only be deactivated after student records are reassigned"));
        }
        branch.setActive(false); branchRepository.save(branch);
        return ResponseEntity.ok(ApiResponse.success(null, "Branch archived"));
    }

    private BranchItem toBranchItem(Branch branch) {
        return new BranchItem(branch.getId(), branch.getCode(), branch.getCode(), branch.isActive(),
                branch.getCreatedAt() == null ? "" : branch.getCreatedAt().toString(),
                branch.getAcademicYear() == null ? null : branch.getAcademicYear().getId(),
                branch.getAcademicYear() == null ? null : branch.getAcademicYear().getName());
    }

    @GetMapping("/academic-years")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AcademicYearItem>>> listAcademicYears() {
        List<AcademicYearItem> items = academicYearRepository.findAll(Sort.by(Sort.Direction.DESC, "startYear"))
                .stream()
                .filter(year -> year.getStatus() == null || year.getStatus() == AccountStatus.ACTIVE)
                .map(this::toAcademicYearItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "Academic years retrieved"));
    }

    @PostMapping("/academic-years")
    public ResponseEntity<ApiResponse<AcademicYearItem>> createAcademicYear(@RequestBody CreateAcademicYearRequest request) {
        if (request.startYear() == null || request.endYear() == null
                || request.startYear() <= 0 || request.endYear() <= request.startYear()) {
            return ResponseEntity.ok(ApiResponse.error("Valid start and end years are required"));
        }
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

    @PutMapping("/academic-years/{yearId}")
    public ResponseEntity<ApiResponse<AcademicYearItem>> updateAcademicYear(@PathVariable Long yearId, @RequestBody CreateAcademicYearRequest request) {
        AcademicYear year = academicYearRepository.findById(yearId).orElse(null);
        if (year == null) return ResponseEntity.ok(ApiResponse.error("Academic year not found"));
        if (request.startYear() == null || request.endYear() == null || request.endYear() <= request.startYear()) return ResponseEntity.ok(ApiResponse.error("Valid start and end years are required"));
        String name = request.name() == null || request.name().isBlank() ? request.startYear() + "-" + request.endYear() : request.name().trim();
        academicYearRepository.findByName(name).filter(existing -> !existing.getId().equals(yearId)).ifPresent(existing -> { throw new org.example.veportal.exception.ConflictException("Academic year already exists"); });
        year.setName(name); year.setStartYear(request.startYear()); year.setEndYear(request.endYear());
        return ResponseEntity.ok(ApiResponse.success(toAcademicYearItem(academicYearRepository.save(year)), "Academic year updated"));
    }

    @DeleteMapping("/academic-years/{yearId}")
    public ResponseEntity<ApiResponse<Object>> archiveAcademicYear(@PathVariable Long yearId) {
        AcademicYear year = academicYearRepository.findById(yearId).orElse(null);
        if (year == null) return ResponseEntity.ok(ApiResponse.error("Academic year not found"));
        year.setStatus(AccountStatus.INACTIVE);
        year.setCurrent(false);
        academicYearRepository.save(year);
        return ResponseEntity.ok(ApiResponse.success(null, "Academic year archived"));
    }

    @GetMapping("/academic-years/{yearId}/semesters")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<SemesterItem>>> semesters(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(academicSemesterRepository.findByAcademicYearIdAndStatusOrderByNameAsc(yearId, "ACTIVE")
                .stream().map(s -> new SemesterItem(s.getId(), s.getName(), s.getAcademicYear().getId())).toList(), "Semesters retrieved"));
    }

    @PostMapping("/academic-years/{yearId}/semesters")
    public ResponseEntity<ApiResponse<SemesterItem>> createSemester(@PathVariable Long yearId, @RequestBody SemesterRequest request) {
        AcademicYear year = academicYearRepository.findById(yearId).orElse(null);
        if (year == null) return ResponseEntity.ok(ApiResponse.error("Academic year not found"));
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank()) return ResponseEntity.ok(ApiResponse.error("Semester name is required"));
        if (academicSemesterRepository.findByAcademicYearIdAndNameIgnoreCase(yearId, name).isPresent()) return ResponseEntity.ok(ApiResponse.error("Semester already exists"));
        AcademicSemester semester = new AcademicSemester(); semester.setAcademicYear(year); semester.setName(name);
        AcademicSemester saved = academicSemesterRepository.save(semester);
        return ResponseEntity.ok(ApiResponse.success(new SemesterItem(saved.getId(), saved.getName(), yearId), "Semester added"));
    }

    @PutMapping("/semesters/{semesterId}")
    public ResponseEntity<ApiResponse<SemesterItem>> updateSemester(@PathVariable Long semesterId, @RequestBody SemesterRequest request) {
        AcademicSemester semester = academicSemesterRepository.findById(semesterId).orElse(null);
        if (semester == null) return ResponseEntity.ok(ApiResponse.error("Semester not found"));
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank()) return ResponseEntity.ok(ApiResponse.error("Semester name is required"));
        semester.setName(name);
        AcademicSemester saved = academicSemesterRepository.save(semester);
        return ResponseEntity.ok(ApiResponse.success(new SemesterItem(saved.getId(), saved.getName(), saved.getAcademicYear().getId()), "Semester updated"));
    }

    @DeleteMapping("/semesters/{semesterId}")
    public ResponseEntity<ApiResponse<Object>> deleteSemester(@PathVariable Long semesterId) {
        AcademicSemester semester = academicSemesterRepository.findById(semesterId).orElse(null);
        if (semester == null) return ResponseEntity.ok(ApiResponse.error("Semester not found"));
        semester.setStatus("ARCHIVED"); academicSemesterRepository.save(semester);
        return ResponseEntity.ok(ApiResponse.success(null, "Semester archived"));
    }

    @GetMapping("/master-data/course-codes")
    public ResponseEntity<ApiResponse<List<CourseCodeItem>>> courseCodes() {
        return ResponseEntity.ok(ApiResponse.success(courseCodeMasterRepository.findByStatusOrderByCodeAsc("ACTIVE").stream()
                .map(c -> new CourseCodeItem(c.getId(), c.getCode(), c.getName())).toList(), "Course codes retrieved"));
    }

    @PostMapping("/master-data/course-codes")
    public ResponseEntity<ApiResponse<CourseCodeItem>> createCourseCode(@RequestBody CourseCodeRequest request) {
        String code = request.code() == null ? "" : request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name() == null ? "" : request.name().trim();
        if (code.isBlank() || name.isBlank()) return ResponseEntity.ok(ApiResponse.error("Course code and name are required"));
        if (courseCodeMasterRepository.findByCodeIgnoreCase(code).isPresent()) return ResponseEntity.ok(ApiResponse.error("Course code already exists"));
        CourseCodeMaster value = new CourseCodeMaster(); value.setCode(code); value.setName(name);
        CourseCodeMaster saved = courseCodeMasterRepository.save(value);
        return ResponseEntity.ok(ApiResponse.success(new CourseCodeItem(saved.getId(), saved.getCode(), saved.getName()), "Course code added"));
    }

    @PutMapping("/master-data/course-codes/{codeId}")
    public ResponseEntity<ApiResponse<CourseCodeItem>> updateCourseCode(@PathVariable Long codeId, @RequestBody CourseCodeRequest request) {
        CourseCodeMaster value = courseCodeMasterRepository.findById(codeId).orElse(null);
        if (value == null) return ResponseEntity.ok(ApiResponse.error("Course code not found"));
        value.setCode(request.code().trim().toUpperCase(Locale.ROOT)); value.setName(request.name().trim());
        CourseCodeMaster saved = courseCodeMasterRepository.save(value);
        return ResponseEntity.ok(ApiResponse.success(new CourseCodeItem(saved.getId(), saved.getCode(), saved.getName()), "Course code updated"));
    }

    @DeleteMapping("/master-data/course-codes/{codeId}")
    public ResponseEntity<ApiResponse<Object>> deleteCourseCode(@PathVariable Long codeId) {
        CourseCodeMaster value = courseCodeMasterRepository.findById(codeId).orElse(null);
        if (value == null) return ResponseEntity.ok(ApiResponse.error("Course code not found"));
        value.setStatus("ARCHIVED"); courseCodeMasterRepository.save(value);
        return ResponseEntity.ok(ApiResponse.success(null, "Course code archived"));
    }

    @GetMapping("/academic-years/{yearId}/courses")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CourseListItem>>> coursesByYear(@PathVariable Long yearId) {
        List<CourseListItem> courses = courseRepository.findByAcademicYearIdOrderByCodeAsc(yearId)
                .stream()
                .filter(course -> !"ARCHIVED".equalsIgnoreCase(course.getStatus()))
                .map(this::toCourseItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses, "Courses retrieved"));
    }

    @GetMapping("/courses/{courseId}/groups")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CourseGroupItem>>> courseGroups(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(courseGroupRepository.findByCourseIdOrderByNameAsc(courseId).stream().map(this::toCourseGroupItem).toList(), "Course groups retrieved"));
    }

    @GetMapping("/course-groups/{groupId}/ta-history")
    public ResponseEntity<ApiResponse<List<TaAssignmentHistoryItem>>> taHistory(@PathVariable Long groupId) {
        if (!courseGroupRepository.existsById(groupId)) return ResponseEntity.status(404).body(ApiResponse.error("Group not found"));
        return ResponseEntity.ok(ApiResponse.success(taAssignmentHistoryRepository.findByGroupIdOrderByAssignedAtDesc(groupId).stream().map(h -> new TaAssignmentHistoryItem(h.getId(), h.getTeachingAssistant().getId(), h.getTeachingAssistant().getFullName(), h.getAssignedAt(), h.getEndedAt(), h.getStatus(), h.getReason())).toList(), "TA assignment history retrieved"));
    }

    @PostMapping("/courses/{courseId}/groups")
    public ResponseEntity<ApiResponse<CourseGroupItem>> createCourseGroup(@PathVariable Long courseId, @RequestBody CourseGroupRequest request) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.ok(ApiResponse.error("Course not found"));
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank()) return ResponseEntity.ok(ApiResponse.error("Group name is required"));
        if (courseGroupRepository.findByCourseIdAndNameIgnoreCase(courseId, name).isPresent()) return ResponseEntity.ok(ApiResponse.error("This group already exists for the course"));
        CourseGroup group = new CourseGroup(); group.setCourse(course); group.setName(name);
        if (request.mentorId() != null) {
            UserAccount mentor = userAccountRepository.findById(request.mentorId()).orElse(null);
            if (mentor == null || !mentor.getRole().isMentor()) return ResponseEntity.ok(ApiResponse.error("Selected mentor was not found"));
            group.setMentor(mentor);
        }
        if (request.taId() != null) {
            UserAccount ta = userAccountRepository.findById(request.taId()).orElse(null);
            if (ta == null || ta.getRole() != Role.TA) return ResponseEntity.ok(ApiResponse.error("Selected teaching assistant was not found"));
            group.setTeachingAssistant(ta);
        }
        return ResponseEntity.ok(ApiResponse.success(toCourseGroupItem(courseGroupRepository.save(group)), "Group created"));
    }

    @PutMapping("/course-groups/{groupId}")
    @Transactional
    public ResponseEntity<ApiResponse<CourseGroupItem>> updateCourseGroup(@PathVariable Long groupId, @RequestBody CourseGroupRequest request) {
        CourseGroup group = courseGroupRepository.findById(groupId).orElse(null);
        if (group == null) return ResponseEntity.ok(ApiResponse.error("Group not found"));
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank()) return ResponseEntity.ok(ApiResponse.error("Group name is required"));
        if (courseGroupRepository.findByCourseIdAndNameIgnoreCase(group.getCourse().getId(), name).filter(existing -> !existing.getId().equals(groupId)).isPresent()) return ResponseEntity.ok(ApiResponse.error("This group already exists for the course"));
        group.setName(name);
        if (request.mentorId() == null) group.setMentor(null); else {
            UserAccount mentor = userAccountRepository.findById(request.mentorId()).orElse(null);
            if (mentor == null || !mentor.getRole().isMentor()) return ResponseEntity.ok(ApiResponse.error("Selected mentor was not found"));
            group.setMentor(mentor);
        }
        UserAccount previousTa = group.getTeachingAssistant();
        if (request.taId() == null) {
            if (previousTa != null) {
                taAssignmentHistoryRepository.findByGroupIdOrderByAssignedAtDesc(groupId).stream()
                        .filter(h -> "ACTIVE".equals(h.getStatus()) && h.getEndedAt() == null)
                        .findFirst()
                        .ifPresent(h -> {
                            h.setEndedAt(LocalDateTime.now());
                            h.setStatus("ENDED");
                            taAssignmentHistoryRepository.save(h);
                        });
            }
            group.setTeachingAssistant(null);
        } else {
            UserAccount ta = userAccountRepository.findById(request.taId()).orElse(null);
            if (ta == null || ta.getRole() != Role.TA) return ResponseEntity.ok(ApiResponse.error("Selected teaching assistant was not found"));
            if (group.getTeachingAssistant() != null && !group.getTeachingAssistant().getId().equals(ta.getId())) {
                taAssignmentHistoryRepository.findByGroupIdOrderByAssignedAtDesc(groupId).stream().filter(h -> "ACTIVE".equals(h.getStatus()) && h.getEndedAt() == null).findFirst().ifPresent(h -> { h.setEndedAt(LocalDateTime.now()); h.setStatus("ENDED"); taAssignmentHistoryRepository.save(h); });
            }
            group.setTeachingAssistant(ta);
        }
        CourseGroup savedGroup = courseGroupRepository.save(group);
        if (savedGroup.getTeachingAssistant() != null && (previousTa == null || !previousTa.getId().equals(savedGroup.getTeachingAssistant().getId()))) recordTaAssignment(savedGroup, savedGroup.getTeachingAssistant(), "Group assignment updated");
        return ResponseEntity.ok(ApiResponse.success(toCourseGroupItem(savedGroup), "Group updated"));
    }

    @DeleteMapping("/course-groups/{groupId}")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deleteCourseGroup(@PathVariable Long groupId) {
        CourseGroup group = courseGroupRepository.findById(groupId).orElse(null);
        if (group == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Group not found"));
        List<CourseStudent> enrollments = courseStudentRepository.findByGroupId(groupId);
        List<CourseStudent> protectedEnrollments = enrollments.stream()
                .filter(enrollment -> studentRepository.findById(enrollment.getStudentId())
                        .map(student -> student.getStatus() == AccountStatus.ACTIVE || hasProtectedAcademicHistory(student.getId()))
                        .orElse(false))
                .toList();
        if (!protectedEnrollments.isEmpty()) return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("This group cannot be deleted because active students or protected academic history is still linked to it. Transfer or remove the students first."));
        // Remove only stale mappings left by removed/deactivated students without protected history.
        if (!enrollments.isEmpty()) courseStudentRepository.deleteAll(enrollments);
        if (sessionRepository.countByGroupId(groupId) > 0) return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("This group cannot be deleted because it has class sessions or academic history. Archive it instead."));
        if (!groupTopicPlanRepository.findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(groupId).isEmpty()) return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("This group cannot be deleted because it has a topic plan. Archive it instead."));
        if (taAssignmentHistoryRepository.countByGroupId(groupId) > 0) return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("This group cannot be deleted because assignment history exists. Archive it instead."));
        courseGroupRepository.delete(group);
        return ResponseEntity.ok(ApiResponse.success(null, "Group deleted"));
    }

    @PostMapping("/courses")
    @Transactional
    public ResponseEntity<ApiResponse<CourseListItem>> createCourse(@RequestBody CreateCourseRequest request) {
        if (request.academicYearId() == null) {
            return ResponseEntity.ok(ApiResponse.error("Select an academic year before creating a course"));
        }
        AcademicYear academicYear = academicYearRepository.findById(request.academicYearId()).orElse(null);
        if (academicYear == null) {
            return ResponseEntity.ok(ApiResponse.error("Academic year not found"));
        }
        if (request.name() == null || request.name().isBlank()
                || request.code() == null || request.code().isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("Course name and code are required"));
        }
        String code = request.code() == null || request.code().isBlank()
                ? "VE-" + System.currentTimeMillis() : request.code().trim();
        if (courseRepository.findByAcademicYearIdAndCode(academicYear.getId(), code).isPresent()) {
            return ResponseEntity.ok(ApiResponse.error("A course with code " + code + " already exists in " + academicYear.getName()));
        }
        Course course = new Course();
        course.setCode(code);
        course.setName(request.name().trim());
        course.setCourseType(request.courseType() == null ? "VE1" : request.courseType().trim());
        course.setTerm(course.getCourseType());
        course.setSemester(request.semester() == null || request.semester() < 1 ? 1 : request.semester());
        if (request.semesterId() != null) {
            AcademicSemester semester = academicSemesterRepository.findById(request.semesterId()).orElse(null);
            if (semester == null || !semester.getAcademicYear().getId().equals(academicYear.getId())) return ResponseEntity.ok(ApiResponse.error("Selected semester does not belong to this academic year"));
            course.setSemesterRef(semester);
            course.setTerm(semester.getName());
        }
        course.setGroupName(request.groupName() == null || request.groupName().isBlank()
                ? course.getName().trim() : request.groupName().trim());
        course.setStatus("ACTIVE");
        course.setAcademicYear(academicYear);
        Course saved = courseRepository.save(course);
        List<String> requestedGroups = request.groups() == null ? List.of() : request.groups().stream().filter(java.util.Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
        if (requestedGroups.isEmpty()) requestedGroups = List.of(course.getGroupName());
        for (String groupName : requestedGroups) {
            CourseGroup group = new CourseGroup(); group.setCourse(saved); group.setName(groupName); courseGroupRepository.save(group);
        }
        // Book topics are central master data. New course offerings must not
        // create a second, course-owned copy of every topic.
        return ResponseEntity.ok(ApiResponse.success(toCourseItem(saved), "Course created"));
    }

    @PutMapping("/courses/{courseId}")
    @Transactional
    public ResponseEntity<ApiResponse<CourseListItem>> updateCourse(@PathVariable Long courseId, @RequestBody CreateCourseRequest request) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.ok(ApiResponse.error("Course not found"));
        if (request.name() == null || request.name().isBlank() || request.code() == null || request.code().isBlank()) return ResponseEntity.ok(ApiResponse.error("Course name and code are required"));
        course.setName(request.name().trim()); course.setCode(request.code().trim());
        if (request.courseType() != null && !request.courseType().isBlank()) course.setCourseType(request.courseType().trim());
        if (request.semester() != null && request.semester() > 0) course.setSemester(request.semester());
        if (request.semesterId() != null) {
            AcademicSemester semester = academicSemesterRepository.findById(request.semesterId()).orElse(null);
            if (semester == null || !semester.getAcademicYear().getId().equals(course.getAcademicYear().getId())) return ResponseEntity.ok(ApiResponse.error("Selected semester does not belong to this academic year"));
            course.setSemesterRef(semester); course.setTerm(semester.getName());
        }
        return ResponseEntity.ok(ApiResponse.success(toCourseItem(courseRepository.save(course)), "Course updated"));
    }

    @DeleteMapping("/courses/{courseId}")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deleteCourseOffering(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Course offering not found"));
        List<CourseGroup> groups = courseGroupRepository.findByCourseIdOrderByNameAsc(courseId);
        List<String> dependencies = new ArrayList<>();
        if (!groups.isEmpty()) dependencies.add(groups.size() + " group" + (groups.size() == 1 ? "" : "s"));
        if (!courseStudentRepository.findByCourseId(courseId).isEmpty()) dependencies.add("student enrollments");
        if (!sessionRepository.findAllWithFacultyByCourse(courseId).isEmpty()) dependencies.add("class sessions");
        if (!courseFacultyRepository.findByCourseId(courseId).isEmpty()) dependencies.add("mentor mappings");
        if (chapterRepository.countByCourseId(courseId) > 0) dependencies.add("legacy topic records");
        if (courseMaterialRepository.countByCourseId(courseId) > 0) dependencies.add("course materials");
        if (studentCourseResultRepository.findAll().stream().anyMatch(result -> result.getCourse() != null && result.getCourse().getId().equals(courseId))) dependencies.add("published or draft results");
        if (!dependencies.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("This course offering cannot be deleted because it contains " + String.join(", ", dependencies) + ". Archive it instead to preserve academic history."));
        }
        courseRepository.delete(course);
        return ResponseEntity.ok(ApiResponse.success(null, "Course offering deleted"));
    }

    @PatchMapping("/courses/{courseId}/archive")
    public ResponseEntity<ApiResponse<Object>> archiveCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Course offering not found"));
        course.setStatus("ARCHIVED"); courseRepository.save(course);
        return ResponseEntity.ok(ApiResponse.success(null, "Course offering archived"));
    }

    @GetMapping("/master-data/courses")
    public ResponseEntity<ApiResponse<List<MasterCourseTemplateItem>>> masterCourses() {
        List<MasterCourseTemplateItem> result = masterCourseTemplateRepository.findByStatusOrderBySemesterAscCodeAsc("ACTIVE")
                .stream().map(template -> new MasterCourseTemplateItem(
                        template.getId(), template.getCode(), template.getName(), template.getCourseType(),
                        template.getSemester(), masterChapterTemplateRepository
                                .findByCourseTemplateIdAndStatusOrderByChapterNumberAsc(template.getId(), "ACTIVE")
                                .stream().map(this::toMasterChapterItem).toList())).toList();
        return ResponseEntity.ok(ApiResponse.success(result, "Master curriculum retrieved"));
    }

    @GetMapping("/master-data/rooms")
    public ResponseEntity<ApiResponse<List<RoomItem>>> masterRooms() {
        List<RoomItem> rooms = roomRepository.findAllByOrderByDisplayOrderAscCodeAsc().stream()
                .map(this::toRoomItem).toList();
        return ResponseEntity.ok(ApiResponse.success(rooms, "Master rooms retrieved"));
    }

    @PostMapping("/master-data/rooms")
    public ResponseEntity<ApiResponse<RoomItem>> createMasterRoom(@RequestBody CreateRoomRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            return ResponseEntity.ok(ApiResponse.error("Room code is required"));
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (roomRepository.findByCodeIgnoreCase(code).isPresent()) {
            return ResponseEntity.ok(ApiResponse.error("A room with code " + code + " already exists"));
        }
        Room room = new Room();
        room.setCode(code);
        room.setDisplayOrder(request.displayOrder() == null ? (int) roomRepository.count() + 1 : request.displayOrder());
        room.setActive(true);
        return ResponseEntity.ok(ApiResponse.success(toRoomItem(roomRepository.save(room)), "Master room added"));
    }

    @DeleteMapping("/master-data/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Object>> archiveMasterRoom(@PathVariable Long roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.ok(ApiResponse.error("Room not found"));
        room.setActive(false);
        roomRepository.save(room);
        return ResponseEntity.ok(ApiResponse.success(null, "Master room archived"));
    }

    @PostMapping("/master-data/courses/{templateId}/chapters")
    public ResponseEntity<ApiResponse<MasterChapterItem>> createMasterChapter(@PathVariable Long templateId,
                                                                                @RequestBody CreateMasterChapterRequest request) {
        MasterCourseTemplate template = masterCourseTemplateRepository.findById(templateId).orElse(null);
        if (template == null) return ResponseEntity.ok(ApiResponse.error("Master course not found"));
        if (request.title() == null || request.title().isBlank()) return ResponseEntity.ok(ApiResponse.error("Chapter title is required"));
        int nextNumber = (int) masterChapterTemplateRepository.countByCourseTemplateIdAndStatus(templateId, "ACTIVE") + 1;
        MasterChapterTemplate chapter = new MasterChapterTemplate();
        chapter.setCourseTemplate(template);
        chapter.setChapterNumber(nextNumber);
        chapter.setTitle(request.title().trim());
        chapter.setDescription(request.description() == null ? "" : request.description().trim());
        chapter.setStatus("ACTIVE");
        return ResponseEntity.ok(ApiResponse.success(toMasterChapterItem(masterChapterTemplateRepository.save(chapter)), "Master chapter added"));
    }

    @DeleteMapping("/master-data/courses/{templateId}/chapters/{chapterId}")
    public ResponseEntity<ApiResponse<Object>> archiveMasterChapter(@PathVariable Long templateId, @PathVariable Long chapterId) {
        MasterChapterTemplate chapter = masterChapterTemplateRepository.findById(chapterId).orElse(null);
        if (chapter == null || !chapter.getCourseTemplate().getId().equals(templateId)) return ResponseEntity.ok(ApiResponse.error("Master chapter not found"));
        chapter.setStatus("ARCHIVED");
        masterChapterTemplateRepository.save(chapter);
        return ResponseEntity.ok(ApiResponse.success(null, "Master chapter archived"));
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
        chapter.setStatus("ARCHIVED");
        chapterRepository.save(chapter);
        return ResponseEntity.ok(ApiResponse.success(null, "Chapter archived"));
    }

    @GetMapping("/courses/{courseId}/faculties")
    public ResponseEntity<ApiResponse<List<FacultyAssignmentItem>>> courseFaculties(@PathVariable Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            return ResponseEntity.ok(ApiResponse.error("Course not found"));
        }
        List<Long> assignedIds = courseFacultyRepository.findByCourseId(courseId)
                .stream().map(CourseFaculty::getFacultyId).toList();
        List<FacultyAssignmentItem> items = userAccountRepository.findAll().stream()
                .filter(u -> u.getRole().isMentor())
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
            courseFacultyRepository.findByFacultyId(facultyId).stream()
                    .filter(existing -> !existing.getCourseId().equals(courseId))
                    .filter(existing -> courseRepository.findById(existing.getCourseId())
                            .map(other -> other.getAcademicYear() != null && course.getAcademicYear() != null
                                    && other.getAcademicYear().getId().equals(course.getAcademicYear().getId()))
                            .orElse(false))
                    .forEach(courseFacultyRepository::delete);
            if (!currentIds.contains(facultyId) && userAccountRepository.findById(facultyId)
                    .filter(u -> u.getRole().isMentor()).isPresent()) {
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
                .map(s -> {
                    CourseStudent assignment = courseStudentRepository.findByCourseId(courseId).stream()
                            .filter(item -> item.getStudentId().equals(s.getId())).findFirst().orElse(null);
                    CourseGroup group = assignment == null || assignment.getGroupId() == null ? null
                            : courseGroupRepository.findById(assignment.getGroupId()).orElse(null);
                    return new StudentAssignmentItem(s.getId(), s.getFullName(), s.getStudentCode(),
                            s.getProgramme(), assignment != null, group == null ? null : group.getId(),
                            group == null ? null : group.getName());
                })
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
            courseStudentRepository.findAll().stream()
                    .filter(existing -> existing.getStudentId().equals(studentId) && !existing.getCourseId().equals(courseId))
                    .filter(existing -> courseRepository.findById(existing.getCourseId())
                            .map(other -> other.getAcademicYear() != null && course.getAcademicYear() != null
                                    && other.getAcademicYear().getId().equals(course.getAcademicYear().getId()))
                            .orElse(false))
                    .forEach(courseStudentRepository::delete);
            if (!currentIds.contains(studentId) && studentRepository.existsById(studentId)) {
                courseStudentRepository.save(new CourseStudent(courseId, studentId));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Students assigned to course"));
    }

    private StudentListItem toStudentItem(Student s, Double attendancePercent) {
        VeSummary ve = veSummary(s);
        StudentAcademicContext academic = academicContext(s);
        return new StudentListItem(
                s.getId(),
                s.getStudentCode(),
                s.getFullName(),
                s.getEmail() == null ? "" : s.getEmail(),
                s.getBranchCode() == null ? null : branchRepository.findByCodeIgnoreCase(s.getBranchCode()).map(Branch::getId).orElse(null),
                s.getProgramme(),
                s.getProgramme(),
                1,
                s.getStatus().name(),
                attendancePercent == null ? 0.0 : attendancePercent,
                ve.passed(), ve.status(), ve.taken(),
                academic.academicYearId(), academic.academicYearName(), academic.courseId(), academic.groupId(), academic.groupName()
        );
    }

    private StudentAcademicContext academicContext(Student student) {
        return courseStudentRepository.findByStudentId(student.getId()).stream()
                .filter(enrollment -> enrollment.getCourse() != null && enrollment.getCourse().getAcademicYear() != null)
                .max(java.util.Comparator.comparing(enrollment -> enrollment.getCreatedAt() == null
                        ? java.time.LocalDateTime.MIN : enrollment.getCreatedAt()))
                .map(enrollment -> new StudentAcademicContext(
                        enrollment.getCourse().getAcademicYear().getId(),
                        enrollment.getCourse().getAcademicYear().getName(),
                        enrollment.getCourse().getId(), enrollment.getGroupId(),
                        enrollment.getGroupId() == null ? null : courseGroupRepository.findById(enrollment.getGroupId()).map(CourseGroup::getName).orElse(null)))
                .orElseGet(() -> new StudentAcademicContext(null,
                        student.getBatch() == null ? "" : student.getBatch(), null, null, null));
    }

    private VeSummary veSummary(Student student) {
        var published = studentCourseResultRepository.findByStudentId(student.getId()).stream()
                .filter(result -> result.getPublishedAt() != null && result.getFinalResult() != null)
                .filter(result -> result.getCourse() != null
                        && ("VE1".equalsIgnoreCase(result.getCourse().getCourseType())
                        || "VE1".equalsIgnoreCase(result.getCourse().getCode())))
                .max(java.util.Comparator.comparing(StudentCourseResult::getPublishedAt))
                .orElse(null);
        if (published != null) {
            boolean passed = published.getFinalResult() == StudentCourseResult.FinalResult.PASS;
            return new VeSummary(passed, passed ? "PASS" : "FAIL",
                    published.getPublishedAt().toLocalDate().toString());
        }

        boolean hasVe1Activity = attendanceRepository.findStudentHistory(student.getId(), null).stream()
                .anyMatch(record -> record.getSession() != null && record.getSession().getCourse() != null
                        && ("VE1".equalsIgnoreCase(record.getSession().getCourse().getCourseType())
                        || "VE1".equalsIgnoreCase(record.getSession().getCourse().getCode())));
        return new VeSummary(false, hasVe1Activity ? "IN_PROGRESS" : "NOT_EVALUATED", "");
    }

    private FacultyListItem toFacultyItem(UserAccount u) {
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
                u.getBranch() == null ? null : u.getBranch().getId(),
                u.getBranch() == null ? null : u.getBranch().getCode(),
                u.getAcademicYear() == null ? null : u.getAcademicYear().getId(),
                u.getAcademicYear() == null ? null : u.getAcademicYear().getName()
        );
    }

    private CourseListItem toCourseItem(Course c) {
        int chapterCount = (int) chapterRepository.countByCourseId(c.getId());
        AcademicYear year = c.getAcademicYear();
        return new CourseListItem(
                c.getId(),
                c.getName(),
                c.getCode(),
                c.getCourseType() != null ? c.getCourseType() : (c.getTerm() != null ? c.getTerm() : "VE1"),
                c.getSemester() == null ? 1 : c.getSemester(),
                year != null ? year.getId() : 0,
                year != null ? year.getName() : "",
                c.getStatus() != null ? c.getStatus() : "ACTIVE",
                chapterCount,
                c.getCreatedAt() == null ? "" : c.getCreatedAt().toString(),
                c.getSemesterRef() == null ? null : c.getSemesterRef().getId(),
                c.getSemesterRef() == null ? null : c.getSemesterRef().getName()
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

    private CourseGroupItem toCourseGroupItem(CourseGroup group) {
        UserAccount mentor = group.getMentor();
        UserAccount ta = group.getTeachingAssistant();
        long studentCount = courseStudentRepository.findByGroupId(group.getId()).stream()
                .map(enrollment -> studentRepository.findById(enrollment.getStudentId()).orElse(null))
                .filter(student -> student != null && student.getStatus() == AccountStatus.ACTIVE)
                .count();
        return new CourseGroupItem(group.getId(), group.getName(), mentor == null ? null : mentor.getId(), mentor == null ? null : mentor.getFullName(), mentor == null ? null : mentor.getEmail(), ta == null ? null : ta.getId(), ta == null ? null : ta.getFullName(), ta == null ? null : ta.getEmail(), studentCount);
    }

    private void recordTaAssignment(CourseGroup group, UserAccount ta, String reason) {
        TeachingAssistantAssignmentHistory history = new TeachingAssistantAssignmentHistory();
        history.setGroup(group); history.setTeachingAssistant(ta); history.setAssignedBy(authenticatedUserProvider.currentUser());
        history.setAssignedAt(LocalDateTime.now()); history.setReason(reason); history.setStatus("ACTIVE");
        taAssignmentHistoryRepository.save(history);
    }

    private Long validGroupId(Long courseId, Long groupId) {
        if (groupId == null) return null;
        return courseGroupRepository.findById(groupId).filter(group -> group.getCourse().getId().equals(courseId)).map(CourseGroup::getId).orElse(null);
    }

    private RoomItem toRoomItem(Room room) {
        return new RoomItem(room.getId(), room.getCode(), room.isActive(), room.getDisplayOrder(),
                room.getCreatedAt() == null ? "" : room.getCreatedAt().toString());
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
        return branchRepository.findById(branchId).map(Branch::getCode).orElse("General");
    }

    private String branchCodeForId(Long branchId) {
        if (branchId == null || branchId <= 0) return null;
        return branchRepository.findById(branchId).map(Branch::getCode).orElse(null);
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
            long activeCourses, long activeChapters, long upcomingSessions,
            long completedSessions, long pendingSessionReports, long activeGroups, long groupsWithoutMentors,
            long groupsWithoutTas, long activeMentors, long activeTas, long ve1Passed, long ve1Failed
    ) {}

    public record StudentListItem(
            long id, String rollNumber, String name, String email,
            Long branchId, String branchName, String branchCode,
            int semester, String status, double attendancePercentage, boolean vePassed,
            String ve1Status, String ve1Taken, Long academicYearId, String academicYearName,
            Long courseId, Long groupId, String groupName
    ) {}

    private record VeSummary(boolean passed, String status, String taken) {}
    private record StudentAcademicContext(Long academicYearId, String academicYearName,
                                          Long courseId, Long groupId, String groupName) {}

    public record FacultyListItem(
            long id, String facultyId, String name, String email,
            String employeeId, String department, String role,
            boolean active, boolean hasAccount, String createdAt,
            Long branchId, String branchCode, Long academicYearId, String academicYearName
    ) {}

    public record CourseListItem(
            long id, String name, String code, String courseType,
            int semester, long academicYearId, String academicYearName,
            String status, int chapterCount, String createdAt,
            Long semesterId, String semesterName
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
            long id, String name, String rollNumber, String programme, boolean assigned,
            Long groupId, String groupName
    ) {}

    public record BranchItem(long id, String code, String name, boolean active, String createdAt,
                             Long academicYearId, String academicYearName) {}
    public record RoomItem(long id, String code, boolean active, int displayOrder, String createdAt) {}

    public record AcademicYearItem(long id, String name, int startYear, int endYear,
                                   boolean current, boolean active, String createdAt) {}

    public record StudentPerformance(
            long studentId, String studentName, String rollNumber, String branchName,
            int totalClasses, int presentClasses, int absentClasses, double attendancePercentage,
            List<Object> chapterWiseAttendance, List<Object> participationSummary
    ) {}

    public record CreateStudentRequest(String rollNumber, String name, String email,
                                       Long branchId, Long academicYearId, Integer semester,
                                       Long facultyId, Long courseId, Long groupId) {}

    public record StudentImportResult(int imported, int skipped, List<String> errors) {}

    public record CreateFacultyRequest(String name, String email, String employeeId, String department,
                                       Long courseId, String role, Long groupId, Long academicYearId,
                                       Long semesterId, Long branchId) {}

    public record AccountCreateRequest(String role, Boolean sendEmail) {}

    public record FacultyStatusRequest(boolean active) {}

    public record CreateBranchRequest(String code, String name, Long academicYearId) {}
    public record CreateRoomRequest(String code, Integer displayOrder) {}

    public record CreateAcademicYearRequest(String name, Integer startYear, Integer endYear) {}
    public record SemesterRequest(String name) {}
    public record SemesterItem(long id, String name, long academicYearId) {}
    public record CourseCodeRequest(String code, String name) {}
    public record CourseCodeItem(long id, String code, String name) {}

    public record CreateCourseRequest(String name, String code, String courseType, Integer semester,
                                      Long academicYearId, String groupName, List<String> groups, Long semesterId) {}

    public record CourseGroupRequest(String name, Long mentorId, Long taId) {}
    public record CourseGroupItem(long id, String name, Long mentorId, String mentorName, String mentorEmail, Long taId, String taName, String taEmail, long studentCount) {}
    public record TaAssignmentHistoryItem(long id, long taId, String taName, LocalDateTime assignedAt, LocalDateTime endedAt, String status, String reason) {}

    public record CreateChapterRequest(String title, String description) {}
    public record CreateMasterChapterRequest(String title, String description) {}
    public record CreateTopicRequest(String title, String description, Integer displayOrder) {}
    public record TopicItem(long id, String title, String description, int displayOrder, String status) {}

    public record MasterCourseTemplateItem(long id, String code, String name, String courseType,
                                           int semester, List<MasterChapterItem> chapters) {}
    public record MasterChapterItem(long id, int chapterNumber, String title, String description, String status) {}

    private MasterChapterItem toMasterChapterItem(MasterChapterTemplate chapter) {
        return new MasterChapterItem(chapter.getId(), chapter.getChapterNumber(), chapter.getTitle(),
                chapter.getDescription() == null ? "" : chapter.getDescription(), chapter.getStatus());
    }

    public record AssignFacultyRequest(List<Long> facultyIds) {}

    public record AssignStudentsRequest(List<Long> studentIds) {}
    public record TransferStudentRequest(Long courseId, Long targetGroupId, String reason) {}
    public record BulkGroupTransferRequest(List<Long> studentIds, String reason) {}
    public record BulkGroupTransferResult(int requested, int moved, int alreadyInTarget, long targetGroupId, String targetGroupName) {}
    public record GroupTransferItem(long id, long courseId, String courseCode, Long previousGroupId,
                                    String previousGroupName, long newGroupId, String newGroupName,
                                    String transferredAt, String reason, String performedBy) {}
}
