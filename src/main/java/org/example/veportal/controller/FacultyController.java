package org.example.veportal.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.entity.AcademicYear;
import org.example.veportal.entity.Branch;
import org.example.veportal.entity.Chapter;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseFaculty;
import org.example.veportal.entity.CourseStudent;
import org.example.veportal.entity.CourseMaterial;
import org.example.veportal.entity.CourseGroup;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.Room;
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.Topic;
import org.example.veportal.entity.GroupTopicPlanVersion;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.repository.AcademicYearRepository;
import org.example.veportal.repository.BranchRepository;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ChapterRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.CourseGroupRepository;
import org.example.veportal.repository.GroupTopicPlanRepository;
import org.example.veportal.repository.GroupTopicPlanVersionRepository;
import org.example.veportal.repository.StudentCourseResultRepository;
import org.example.veportal.repository.SessionTaskRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.repository.TopicRepository;
import org.example.veportal.repository.RoomRepository;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

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
    private final BranchRepository branchRepository;
    private final TopicRepository topicRepository;
    private final RoomRepository roomRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final GroupTopicPlanRepository groupTopicPlanRepository;
    private final GroupTopicPlanVersionRepository groupTopicPlanVersionRepository;
    private final StudentCourseResultRepository studentCourseResultRepository;
    private final SessionTaskRepository sessionTaskRepository;

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
                             TopicRepository topicRepository,
                             BranchRepository branchRepository,
                             RoomRepository roomRepository,
                             CourseGroupRepository courseGroupRepository,
                             GroupTopicPlanRepository groupTopicPlanRepository,
                             GroupTopicPlanVersionRepository groupTopicPlanVersionRepository,
                             StudentCourseResultRepository studentCourseResultRepository,
                             SessionTaskRepository sessionTaskRepository) {
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
        this.branchRepository = branchRepository;
        this.roomRepository = roomRepository;
        this.courseGroupRepository = courseGroupRepository;
        this.groupTopicPlanRepository = groupTopicPlanRepository;
        this.groupTopicPlanVersionRepository = groupTopicPlanVersionRepository;
        this.studentCourseResultRepository = studentCourseResultRepository;
        this.sessionTaskRepository = sessionTaskRepository;
    }

    private List<Course> coursesForCurrentUser(UserAccount currentUser) {
        if (currentUser.getRole().isCoordinator()) {
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
        if (user.getRole().isCoordinator()) {
            return true;
        }
        if (user.getRole().isMentor() || user.getRole() == Role.TA) {
            return courseGroupRepository.findByCourseIdOrderByNameAsc(courseId).stream()
                    .anyMatch(group -> canAccessGroup(user, group));
        }
        return courseFacultyRepository.existsByCourseIdAndFacultyId(courseId, user.getId());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<FacultyDashboardStats>> dashboard() {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        boolean coordinator = currentUser.getRole().isCoordinator();
        List<CourseGroup> visibleGroups = coordinator ? List.of() : courseGroupRepository.findAll().stream()
                .filter(group -> canAccessGroup(currentUser, group))
                .toList();
        List<Course> courses = coordinator
                ? coursesForCurrentUser(currentUser)
                : visibleGroups.stream().map(CourseGroup::getCourse).distinct().toList();
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<Long> visibleGroupIds = visibleGroups.stream().map(CourseGroup::getId).toList();
        List<Long> studentIds = (coordinator
                ? courseIds.stream().flatMap(id -> courseStudentRepository.findByCourseId(id).stream()).toList()
                : visibleGroupIds.stream().flatMap(id -> courseStudentRepository.findByGroupId(id).stream()).toList())
                .stream()
                .map(org.example.veportal.entity.CourseStudent::getStudentId).distinct().toList();
        long totalStudents = studentIds.size();
        List<ClassSession> visibleSessions = coordinator
                ? courseIds.stream().flatMap(id -> sessionRepository.findAllWithFacultyByCourse(id).stream()).toList()
                : sessionRepository.findAll().stream()
                        .filter(session -> session.getGroup() != null && visibleGroupIds.contains(session.getGroup().getId()))
                        .toList();
        long classesConducted = visibleSessions.stream()
                .filter(session -> session.getStatus() == SessionStatus.COMPLETED).count();
        List<Long> sessionIds = visibleSessions.stream()
                .map(org.example.veportal.entity.ClassSession::getId).toList();
        long materialsCount = coordinator
                ? (courseIds.isEmpty() ? 0 : materialRepository.countByCourseIdIn(courseIds))
                : materialRepository.countBySessionIds(sessionIds).stream()
                        .mapToLong(CourseMaterialRepository.SessionMaterialAggregation::getMaterialCount).sum();
        long totalAttendance = 0;
        long presentAttendance = 0;
        for (var aggregate : attendanceRepository.aggregateBySessions(sessionIds)) {
            totalAttendance += aggregate.getTotal();
            presentAttendance += aggregate.getPresent();
        }
        double averageAttendance = totalAttendance == 0 ? 0.0 : presentAttendance * 100.0 / totalAttendance;
        AcademicYear current = academicYearRepository.findByCurrentTrue().orElse(null);
        String currentYear = current != null ? current.getName() : "2025-2026";

        long topicCount = coordinator
                ? chapterRepository.countByCourseIdIn(courseIds)
                : visibleGroupIds.stream().flatMap(id -> groupTopicPlanRepository.findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(id).stream())
                        .map(plan -> plan.getChapter().getId()).distinct().count();
        FacultyDashboardStats stats = new FacultyDashboardStats(
                coordinator ? courses.size() : visibleGroups.size(),
                topicCount,
                classesConducted,
                totalStudents,
                averageAttendance,
                materialsCount,
                currentYear
        );
        return ResponseEntity.ok(ApiResponse.success(stats, "Faculty dashboard stats retrieved"));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<String>>> rooms() {
        return ResponseEntity.ok(ApiResponse.success(
                roomRepository.findByActiveTrueOrderByDisplayOrderAscCodeAsc().stream().map(Room::getCode).toList(),
                "Rooms retrieved"));
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
                            c.getCourseType() != null ? c.getCourseType() : (c.getTerm() != null ? c.getTerm() : "VE1"),
                            c.getSemester() == null ? 1 : c.getSemester(),
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

    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<List<BranchItem>>> branches() {
        List<BranchItem> branches = branchRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(b -> new BranchItem(b.getId(), b.getCode(), b.getCode(), b.isActive(),
                        b.getCreatedAt() == null ? "" : b.getCreatedAt().toString())).toList();
        return ResponseEntity.ok(ApiResponse.success(branches, "Branches retrieved"));
    }

    @GetMapping("/courses/{courseId}/chapters")
    public ResponseEntity<ApiResponse<List<ChapterItem>>> courseChapters(@PathVariable Long courseId) {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        if (!canAccessCourse(currentUser, courseId)) {
            return ResponseEntity.ok(ApiResponse.error("Course not accessible"));
        }

        List<ChapterItem> chapters = chapterRepository.findByCourseIdOrderByDisplayOrderAscChapterNumberAsc(courseId)
                .stream().filter(ch -> !"ARCHIVED".equalsIgnoreCase(ch.getStatus()))
                .map(this::toChapterItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(chapters, "Chapters retrieved"));
    }

    @GetMapping("/chapters/{chapterId}/topics")
    public ResponseEntity<ApiResponse<List<TopicItem>>> topics(@PathVariable Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null || !canAccessCourse(authenticatedUserProvider.currentUser(), chapter.getCourse().getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Chapter not accessible"));
        }
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
        List<Long> studentIds = courseStudentRepository.findByCourseId(courseId).stream()
                .filter(enrollment -> currentUser.getRole().isCoordinator()
                        || (enrollment.getGroup() != null && canAccessGroup(currentUser, enrollment.getGroup())))
                .map(org.example.veportal.entity.CourseStudent::getStudentId).toList();
        List<CourseStudentItem> items = studentIds.isEmpty()
                ? List.of()
                : studentRepository.findAllById(studentIds).stream()
                        .map(s -> new CourseStudentItem(s.getId(), s.getFullName(),
                                s.getStudentCode(), s.getProgramme()))
                        .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "Course students retrieved"));
    }

    @GetMapping("/students")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<FacultyStudentItem>>> students(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(name = "groupId", required = false) Long groupId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UserAccount user = authenticatedUserProvider.currentUser();
        List<Long> ids;
        if (groupId != null) {
            CourseGroup group = courseGroupRepository.findById(groupId).orElse(null);
            if (group == null || !canAccessGroup(user, group)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Group not accessible"));
            }
            ids = courseStudentRepository.findByGroupId(groupId).stream().map(CourseStudent::getStudentId).distinct().toList();
        } else {
            ids = assignedStudentIds(user);
        }
        String term = search == null ? "" : search.trim().toLowerCase();
        Branch branch = branchId == null ? null : branchRepository.findById(branchId).orElse(null);
        List<Student> filtered = studentRepository.findAllById(ids).stream()
                .filter(s -> term.isBlank() || s.getFullName().toLowerCase().contains(term)
                        || s.getStudentCode().toLowerCase().contains(term))
                .filter(s -> branch == null || branch.getCode().equalsIgnoreCase(s.getBranchCode())
                        || branch.getCode().equalsIgnoreCase(s.getProgramme()))
                .sorted(java.util.Comparator.comparing(Student::getFullName))
                .toList();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        List<Student> current = filtered.subList(from, to);
        Map<Long, AttendanceRecordRepository.StudentAttendanceAggregation> aggregates = new HashMap<>();
        if (!current.isEmpty()) {
            attendanceRepository.aggregateByStudents(current.stream().map(Student::getId).toList())
                    .forEach(a -> aggregates.put(a.getStudentId(), a));
        }
        List<FacultyStudentItem> items = current.stream().map(s -> toFacultyStudentItem(s, aggregates.get(s.getId()), user, groupId)).toList();
        int totalPages = (int) Math.ceil(filtered.size() / (double) safeSize);
        return ResponseEntity.ok(ApiResponse.success(items, "Students retrieved",
                new org.example.veportal.dto.PageMeta(safePage, safeSize, filtered.size(), totalPages)));
    }

    @GetMapping("/students/{studentId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<FacultyStudentItem>> student(@PathVariable Long studentId) {
        if (!assignedStudentIds(authenticatedUserProvider.currentUser()).contains(studentId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Student not accessible"));
        }
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) return ResponseEntity.status(404).body(ApiResponse.error("Student not found"));
        AttendanceRecordRepository.StudentAttendanceAggregation aggregate = attendanceRepository
                .aggregateByStudents(List.of(studentId)).stream().findFirst().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(toFacultyStudentItem(student, aggregate, authenticatedUserProvider.currentUser(), null), "Student retrieved"));
    }

    private List<Long> assignedStudentIds(UserAccount user) {
        if (user.getRole().isCoordinator()) {
            return studentRepository.findAll().stream().map(Student::getId).toList();
        }
        List<Long> groupIds = courseGroupRepository.findAll().stream()
                .filter(group -> canAccessGroup(user, group)).map(CourseGroup::getId).toList();
        if (!groupIds.isEmpty()) {
            return courseStudentRepository.findByGroupIdIn(groupIds).stream().map(CourseStudent::getStudentId).distinct().toList();
        }
        return List.of();
    }

    private FacultyStudentItem toFacultyStudentItem(Student s,
            AttendanceRecordRepository.StudentAttendanceAggregation aggregate, UserAccount user, Long requestedGroupId) {
        VeSummary ve = veSummary(s);
        long total = aggregate == null ? 0 : aggregate.getTotal();
        long present = aggregate == null ? 0 : aggregate.getPresent();
        double percentage = total == 0 ? 0 : present * 100.0 / total;
        CourseGroup context = courseGroupRepository.findAll().stream()
                .filter(group -> requestedGroupId == null || group.getId().equals(requestedGroupId))
                .filter(group -> canAccessGroup(user, group))
                .filter(group -> courseStudentRepository.findByGroupId(group.getId()).stream().anyMatch(enrollment -> enrollment.getStudentId().equals(s.getId())))
                .findFirst().orElse(null);
        return new FacultyStudentItem(s.getId(), s.getStudentCode(), s.getFullName(), s.getEmail(),
                s.getBranchCode(), s.getProgramme(), s.getBranchCode(), null, s.getBatch(), 1,
                s.getStatus().name(), s.getCreatedAt() == null ? "" : s.getCreatedAt().toString(),
                percentage, total, present, ve.passed(), ve.status(), ve.taken(),
                context == null ? null : context.getId(), context == null ? "" : context.getName(),
                context == null ? "" : context.getCourse().getName(), context == null ? "" : context.getCourse().getCode(),
                context == null ? 0 : (int) sessionTaskRepository.findByStatusOrderByStartDateAscIdAsc("ACTIVE").stream()
                        .filter(task -> task.getSession().getGroup() != null && task.getSession().getGroup().getId().equals(context.getId())).count());
    }

    @GetMapping("/courses/{courseId}/groups")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<GroupItem>>> courseGroups(@PathVariable Long courseId) {
        UserAccount currentUser = authenticatedUserProvider.currentUser();
        if (!canAccessCourse(currentUser, courseId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Course not accessible"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                courseGroupRepository.findByCourseIdOrderByNameAsc(courseId).stream()
                        .filter(group -> currentUser.getRole().isCoordinator() || canAccessGroup(currentUser, group))
                        .map(group -> new GroupItem(group.getId(), group.getName(),
                                group.getMentor() == null ? null : group.getMentor().getFullName()))
                        .toList(), "Groups retrieved"));
    }

    @GetMapping("/groups")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<GroupItem>>> myGroups() {
        UserAccount user = authenticatedUserProvider.currentUser();
        List<GroupItem> groups = courseGroupRepository.findAll().stream()
                .filter(group -> user.getRole().isCoordinator()
                        || (group.getMentor() != null && group.getMentor().getId().equals(user.getId()))
                        || (group.getTeachingAssistant() != null && group.getTeachingAssistant().getId().equals(user.getId())))
                .map(this::toGroupItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(groups, "Assigned groups retrieved"));
    }

    @GetMapping("/my-groups")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<GroupItem>>> authenticatedGroups() {
        return myGroups();
    }

    @GetMapping("/groups/{groupId}/overview")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<GroupItem>> groupOverview(@PathVariable Long groupId) {
        UserAccount user = authenticatedUserProvider.currentUser();
        CourseGroup group = courseGroupRepository.findById(groupId).orElse(null);
        if (group == null || !canAccessGroup(user, group)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Group not accessible"));
        }
        return ResponseEntity.ok(ApiResponse.success(toGroupItem(group), "Group overview retrieved"));
    }

    private GroupItem toGroupItem(CourseGroup group) {
        Course course = group.getCourse();
        AcademicYear year = course.getAcademicYear();
        return new GroupItem(
                group.getId(), group.getName(),
                course.getCode() + "-" + group.getName().replaceAll("\\s+", "-"),
                course.getId(), course.getName(), course.getCode(),
                year == null ? 0 : year.getId(),
                year == null ? "" : year.getName(),
                course.getSemesterRef() == null ? (course.getSemester() == null ? "" : "Semester " + course.getSemester()) : course.getSemesterRef().getName(),
                group.getMentor() == null ? null : group.getMentor().getId(),
                group.getMentor() == null ? null : group.getMentor().getFullName(),
                group.getTeachingAssistant() == null ? null : group.getTeachingAssistant().getId(),
                group.getTeachingAssistant() == null ? null : group.getTeachingAssistant().getFullName(),
                courseStudentRepository.findByGroupId(group.getId()).size(),
                (int) groupTopicPlanRepository.findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(group.getId()).stream().map(p -> p.getChapter().getId()).distinct().count(),
                sessionRepository.countByGroupId(group.getId()),
                sessionRepository.countByGroupIdAndStatus(group.getId(), SessionStatus.COMPLETED),
                "ARCHIVED".equalsIgnoreCase(course.getStatus()) ? "ARCHIVED" : "ACTIVE"
        );
    }

    @GetMapping("/groups/{groupId}/topics")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<GroupTopicItem>>> groupTopics(@PathVariable Long groupId) {
        CourseGroup group = courseGroupRepository.findById(groupId).orElse(null);
        if (group == null || !canAccessGroup(authenticatedUserProvider.currentUser(), group)) return ResponseEntity.status(403).body(ApiResponse.error("Group not accessible"));
        Map<Long, org.example.veportal.entity.GroupTopicPlan> plans = groupTopicPlanRepository.findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(groupId)
                .stream().collect(java.util.stream.Collectors.toMap(plan -> plan.getChapter().getId(), plan -> plan));
        List<GroupTopicItem> items = chapterRepository.findByCourseIdOrderByDisplayOrderAscChapterNumberAsc(group.getCourse().getId()).stream()
                .filter(chapter -> !"ARCHIVED".equalsIgnoreCase(chapter.getStatus()))
                .map(chapter -> {
                    var plan = plans.get(chapter.getId());
                    return new GroupTopicItem(chapter.getId(), chapter.getChapterNumber(), chapter.getTitle(),
                            plan == null ? null : plan.getWeekNumber(), plan == null ? null : plan.getDisplayOrder(),
                            plan != null && plan.isLocked(), plan != null);
                }).toList();
        return ResponseEntity.ok(ApiResponse.success(items, "Group topics retrieved"));
    }

    @PutMapping("/groups/{groupId}/topics")
    @Transactional
    public ResponseEntity<ApiResponse<List<GroupTopicItem>>> saveGroupTopics(@PathVariable Long groupId, @RequestBody List<GroupTopicRequest> request) {
        CourseGroup group = courseGroupRepository.findById(groupId).orElse(null);
        UserAccount user = authenticatedUserProvider.currentUser();
        if (group == null || !canAccessGroup(user, group)) return ResponseEntity.status(403).body(ApiResponse.error("Group not accessible"));
        if (user.getRole() == Role.TA) return ResponseEntity.status(403).body(ApiResponse.error("Teaching Assistants can view topics but cannot plan them"));
        if (request == null) return ResponseEntity.badRequest().body(ApiResponse.error("Topic plan is required"));
        var requestedIds = request.stream().map(GroupTopicRequest::chapterId).toList();
        if (requestedIds.stream().anyMatch(java.util.Objects::isNull)
                || requestedIds.size() != new java.util.HashSet<>(requestedIds).size()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Topic plan contains duplicate or invalid topics"));
        }
        if (request.stream().anyMatch(entry -> entry.weekNumber() < 1)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Every selected topic must have a positive planned week"));
        }
        List<org.example.veportal.entity.GroupTopicPlan> existingPlans = groupTopicPlanRepository.findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(groupId);
        if (!existingPlans.isEmpty() && (existingPlans.stream().anyMatch(org.example.veportal.entity.GroupTopicPlan::isLocked) || request.stream().anyMatch(GroupTopicRequest::locked))) {
            GroupTopicPlanVersion version = new GroupTopicPlanVersion();
            version.setGroup(group); version.setAuthor(user); version.setCreatedAtOverride(java.time.LocalDateTime.now());
            version.setVersionNumber(groupTopicPlanVersionRepository.findByGroupIdOrderByVersionNumberDesc(groupId).stream().findFirst().map(v -> v.getVersionNumber() + 1).orElse(1));
            version.setReason("Topic plan lock or authorized revision");
            version.setSnapshot(existingPlans.stream().map(p -> p.getChapter().getId() + ":" + p.getWeekNumber() + ":" + p.getDisplayOrder() + ":" + p.isLocked()).collect(java.util.stream.Collectors.joining(",")));
            groupTopicPlanVersionRepository.save(version);
        }
        java.util.Set<Long> requestedChapterIds = new java.util.HashSet<>(requestedIds);
        for (var existing : groupTopicPlanRepository.findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(groupId)) {
            if (!requestedChapterIds.contains(existing.getChapter().getId())) {
                if (existing.isLocked()) return ResponseEntity.status(409).body(ApiResponse.error("Locked topics cannot be removed. Unlock the plan before changing selected topics."));
                groupTopicPlanRepository.delete(existing);
            }
        }
        for (int sequence = 0; sequence < request.size(); sequence++) {
            GroupTopicRequest entry = request.get(sequence);
            Chapter chapter = chapterRepository.findById(entry.chapterId()).filter(ch -> ch.getCourse().getId().equals(group.getCourse().getId())).orElse(null);
            if (chapter == null) return ResponseEntity.ok(ApiResponse.error("Every topic must belong to the group's course"));
            var existing = groupTopicPlanRepository.findByGroupIdAndChapterId(groupId, entry.chapterId()).orElse(null);
            if (existing != null && existing.isLocked() && (!entry.locked() || !java.util.Objects.equals(existing.getWeekNumber(), entry.weekNumber()) || !java.util.Objects.equals(existing.getDisplayOrder(), entry.displayOrder()))) {
                return ResponseEntity.status(409).body(ApiResponse.error("Locked topics cannot be changed. Unlock the plan before editing it."));
            }
            if (existing == null) existing = new org.example.veportal.entity.GroupTopicPlan();
            existing.setGroup(group); existing.setChapter(chapter); existing.setWeekNumber(sequence + 1); existing.setDisplayOrder(sequence + 1); existing.setLocked(entry.locked());
            groupTopicPlanRepository.save(existing);
        }
        groupTopicPlanRepository.flush();
        return groupTopics(groupId);
    }

    @PostMapping("/groups/{groupId}/topics/unlock")
    @Transactional
    public ResponseEntity<ApiResponse<List<GroupTopicItem>>> unlockGroupTopics(@PathVariable Long groupId) {
        CourseGroup group = courseGroupRepository.findById(groupId).orElse(null);
        UserAccount user = authenticatedUserProvider.currentUser();
        if (group == null || !canAccessGroup(user, group)) return ResponseEntity.status(403).body(ApiResponse.error("Group not accessible"));
        if (user.getRole() == Role.TA) return ResponseEntity.status(403).body(ApiResponse.error("Teaching Assistants cannot unlock topic plans"));
        List<org.example.veportal.entity.GroupTopicPlan> existing = groupTopicPlanRepository.findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(groupId);
        if (existing.stream().anyMatch(org.example.veportal.entity.GroupTopicPlan::isLocked)) {
            GroupTopicPlanVersion version = new GroupTopicPlanVersion();
            version.setGroup(group); version.setAuthor(user); version.setCreatedAtOverride(java.time.LocalDateTime.now());
            version.setVersionNumber(groupTopicPlanVersionRepository.findByGroupIdOrderByVersionNumberDesc(groupId).stream().findFirst().map(v -> v.getVersionNumber() + 1).orElse(1));
            version.setReason("Authorized topic plan unlock");
            version.setSnapshot(existing.stream().map(p -> p.getChapter().getId() + ":" + p.getWeekNumber() + ":" + p.getDisplayOrder() + ":" + p.isLocked()).collect(java.util.stream.Collectors.joining(",")));
            groupTopicPlanVersionRepository.save(version);
            existing.forEach(plan -> plan.setLocked(false));
            groupTopicPlanRepository.saveAll(existing);
            groupTopicPlanRepository.flush();
        }
        return groupTopics(groupId);
    }

    @GetMapping("/groups/{groupId}/topic-plan-versions")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<TopicPlanVersionItem>>> topicPlanVersions(@PathVariable Long groupId) {
        CourseGroup group = courseGroupRepository.findById(groupId).orElse(null);
        if (group == null || !canAccessGroup(authenticatedUserProvider.currentUser(), group)) return ResponseEntity.status(403).body(ApiResponse.error("Group not accessible"));
        return ResponseEntity.ok(ApiResponse.success(groupTopicPlanVersionRepository.findByGroupIdOrderByVersionNumberDesc(groupId).stream().map(v -> new TopicPlanVersionItem(v.getId(), v.getVersionNumber(), v.getSnapshot(), v.getReason(), v.getAuthor().getFullName(), v.getCreatedAtOverride().toString())).toList(), "Topic plan history retrieved"));
    }

    private boolean canAccessGroup(UserAccount user, CourseGroup group) {
        return user.getRole().isCoordinator()
                || (group.getMentor() != null && group.getMentor().getId().equals(user.getId()))
                || (group.getTeachingAssistant() != null && group.getTeachingAssistant().getId().equals(user.getId()));
    }

    public record TopicPlanVersionItem(long id, int versionNumber, String snapshot, String reason, String author, String createdAt) {}

    private VeSummary veSummary(Student student) {
        var records = attendanceRepository.findStudentHistory(student.getId(), null).stream()
                .filter(record -> "VE1".equalsIgnoreCase(record.getSession().getCourse().getCourseType())
                        || "VE1".equalsIgnoreCase(record.getSession().getCourse().getCode())).toList();
        var published = studentCourseResultRepository.findByStudentId(student.getId()).stream()
                .filter(result -> result.getPublishedAt() != null)
                .filter(result -> "VE1".equalsIgnoreCase(result.getCourse().getCourseType())
                        || "VE1".equalsIgnoreCase(result.getCourse().getCode()))
                .findFirst().orElse(null);
        if (published != null && published.getFinalResult() != null) {
            boolean passed = published.getFinalResult() == org.example.veportal.entity.StudentCourseResult.FinalResult.PASS;
            return new VeSummary(passed, passed ? "PASS" : "FAIL", published.getPublishedAt().toLocalDate().toString());
        }
        if (records.isEmpty()) return new VeSummary(false, "NOT_EVALUATED", "");
        var first = records.stream().map(record -> record.getSession().getSessionDate()).min(java.time.LocalDate::compareTo).orElse(null);
        String year = records.get(0).getSession().getCourse().getAcademicYear() == null ? "" : records.get(0).getSession().getCourse().getAcademicYear().getName();
        return new VeSummary(false, "IN_PROGRESS", year + (first == null ? "" : " · " + first));
    }

    @GetMapping("/class-sessions")
    public ResponseEntity<ApiResponse<List<ClassSessionListItem>>> classSessions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PagedResult<org.example.veportal.dto.response.SessionListItemResponse> result =
                sessionService.list(null, null, page, size, authenticatedUserProvider.currentUser());

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
        if (currentUser.getRole() == Role.TA) {
            return ResponseEntity.status(403).body(ApiResponse.error("Teaching Assistants support sessions but cannot create them"));
        }
        Long courseId = body.courseId() != null ? body.courseId() : null;
        Course course = courseId != null
                ? courseRepository.findById(courseId)
                    .orElseThrow(() -> NotFoundException.resource("Course", courseId))
                : courseRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> NotFoundException.resource("Course", "none"));
        if (!canAccessCourse(currentUser, course.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Course not accessible"));
        }
        if (body.groupId() == null) {
            return ResponseEntity.ok(ApiResponse.error("Select the student group for this session"));
        }
        CourseGroup group = courseGroupRepository.findById(body.groupId())
                .filter(candidate -> candidate.getCourse().getId().equals(course.getId()))
                .orElseThrow(() -> new BusinessException("Selected group is invalid for this course"));
        if (!currentUser.getRole().isCoordinator() && !canAccessGroup(currentUser, group)) {
            return ResponseEntity.status(403).body(ApiResponse.error("You can only create sessions for your assigned group"));
        }
        String startTime = body.startTime() == null || body.startTime().isBlank() ? "11:00" : body.startTime();
        String endTime = body.endTime() == null || body.endTime().isBlank() ? "11:30" : body.endTime();
        validateTimeRange(startTime, endTime);
        if (body.sessionDate() == null || body.sessionDate().isBlank()) {
            throw new BusinessException("Session date is required");
        }
        LocalDate sessionDate;
        try {
            sessionDate = LocalDate.parse(body.sessionDate());
        } catch (java.time.format.DateTimeParseException ex) {
            throw new BusinessException("Session date must be in YYYY-MM-DD format");
        }
        if (sessionRepository.existsScheduleConflict(group.getId(), currentUser.getId(), sessionDate, startTime, endTime, null) > 0) {
            throw new BusinessException("This group or mentor already has a session scheduled in the selected time range");
        }

        ClassSession session = new ClassSession();
        session.setCourse(course);
        session.setGroup(group);
        session.setSessionNumber(sessionRepository.findMaxSessionNumber(course.getId()) + 1);
        if (body.chapterId() != null) {
            Chapter chapter = chapterRepository.findById(body.chapterId())
                    .filter(ch -> ch.getCourse().getId().equals(course.getId()))
                    .filter(ch -> !"ARCHIVED".equalsIgnoreCase(ch.getStatus()))
                    .orElseThrow(() -> new BusinessException("Selected chapter is invalid for this course"));
            if (!currentUser.getRole().isCoordinator()
                    && groupTopicPlanRepository.findByGroupIdAndChapterId(group.getId(), chapter.getId()).isEmpty()) {
                throw new BusinessException("Selected topic is not mapped to this group");
            }
            session.setChapter(chapter);
        }
        if (body.topicId() != null) {
            Topic topic = topicRepository.findById(body.topicId())
                    .filter(candidate -> body.chapterId() == null
                            || candidate.getChapter().getId().equals(body.chapterId()))
                    .orElseThrow(() -> new BusinessException("Selected topic is invalid for this chapter"));
            session.setTopicEntity(topic);
        }
        session.setTopic(body.topic() == null || body.topic().isBlank()
                ? "Class Session on " + sessionDate
                : body.topic().trim());
        session.setSessionDate(sessionDate);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        String room = body.room() == null || body.room().isBlank() ? "SH-1" : body.room().trim();
        roomRepository.findByCodeIgnoreCase(room).filter(Room::isActive)
                .orElseThrow(() -> new BusinessException("Select an active room from master data"));
        session.setRoom(room);
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

    @PutMapping("/class-sessions/{sessionId}")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> updateClassSession(@PathVariable Long sessionId,
                                                                    @RequestBody SessionCreateBodyRequest body) {
        UserAccount user = authenticatedUserProvider.currentUser();
        if (user.getRole() == Role.TA) return ResponseEntity.status(403).body(ApiResponse.error("Teaching Assistants cannot edit sessions"));
        ClassSession session = sessionRepository.findById(sessionId).orElseThrow(() -> NotFoundException.resource("Session", sessionId));
        CourseGroup group = session.getGroup();
        if (group == null || (!user.getRole().isCoordinator() && !canAccessGroup(user, group))) {
            return ResponseEntity.status(403).body(ApiResponse.error("You can only edit sessions for your assigned group"));
        }
        String start = body.startTime() == null || body.startTime().isBlank() ? session.getStartTime() : body.startTime();
        String end = body.endTime() == null || body.endTime().isBlank() ? session.getEndTime() : body.endTime();
        validateTimeRange(start, end);
        LocalDate date;
        try { date = LocalDate.parse(body.sessionDate()); } catch (Exception ex) { throw new BusinessException("Session date must be in YYYY-MM-DD format"); }
        if (sessionRepository.existsScheduleConflict(group.getId(), user.getId(), date, start, end, sessionId) > 0) {
            throw new BusinessException("This group or mentor already has a session scheduled in the selected time range");
        }
        if (body.chapterId() != null) {
            Chapter chapter = chapterRepository.findById(body.chapterId())
                    .filter(ch -> ch.getCourse().getId().equals(session.getCourse().getId()))
                    .filter(ch -> groupTopicPlanRepository.findByGroupIdAndChapterId(group.getId(), ch.getId()).isPresent())
                    .orElseThrow(() -> new BusinessException("Selected topic is not mapped to this group"));
            session.setChapter(chapter);
        }
        session.setTopic(body.topic() == null || body.topic().isBlank() ? session.getTopic() : body.topic().trim());
        session.setTitle(session.getTopic());
        session.setDescription(body.description());
        session.setSessionDate(date);
        session.setStartTime(start);
        session.setEndTime(end);
        if (body.room() != null && !body.room().isBlank()) {
            roomRepository.findByCodeIgnoreCase(body.room()).filter(Room::isActive)
                    .orElseThrow(() -> new BusinessException("Select an active room from master data"));
            session.setRoom(body.room().trim());
        }
        sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success(null, "Class session updated"));
    }

    @DeleteMapping("/class-sessions/{sessionId}")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deleteClassSession(@PathVariable Long sessionId) {
        UserAccount user = authenticatedUserProvider.currentUser();
        if (user.getRole() == Role.TA) return ResponseEntity.status(403).body(ApiResponse.error("Teaching Assistants cannot delete sessions"));
        ClassSession session = sessionRepository.findById(sessionId).orElseThrow(() -> NotFoundException.resource("Session", sessionId));
        if (session.getGroup() == null || (!user.getRole().isCoordinator() && !canAccessGroup(user, session.getGroup()))) {
            return ResponseEntity.status(403).body(ApiResponse.error("You can only delete sessions for your assigned group"));
        }
        if (!attendanceRepository.findWithStudentBySessionId(sessionId).isEmpty()
                || !sessionTaskRepository.findBySessionIdOrderByStartDateAscIdAsc(sessionId).isEmpty()) {
            return ResponseEntity.status(409).body(ApiResponse.error("This session has attendance or assignments and cannot be deleted. Cancel it instead to preserve academic history."));
        }
        try {
            sessionRepository.delete(session);
            sessionRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error("This session has related academic records and cannot be deleted. Cancel it instead."));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Class session deleted"));
    }

    private void validateTimeRange(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isBlank() || endTime.isBlank()) {
            throw new BusinessException("Start and end time are required");
        }
        if (!startTime.matches("\\d{2}:\\d{2}") || !endTime.matches("\\d{2}:\\d{2}")) {
            throw new BusinessException("Times must be valid 24-hour values in HH:mm format");
        }
        try {
            if (!LocalTime.parse(endTime).isAfter(LocalTime.parse(startTime))) {
                throw new BusinessException("End time must be later than start time");
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new BusinessException("Times must be valid 24-hour values in HH:mm format");
        }
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

    public record GroupItem(
            long id, String name, String groupCode, long courseId, String courseName,
            String courseCode, long academicYearId, String academicYear, String semester, Long mentorId,
            String mentorName, Long teachingAssistantId, String teachingAssistantName,
            long studentCount, int mappedTopicCount, long scheduledSessionCount,
            long completedSessionCount, String status) {
        public GroupItem(long id, String name, String mentorName) {
            this(id, name, "", 0, "", "", 0, "", "", null, mentorName, null, null,
                    0, 0, 0, 0, "ACTIVE");
        }
    }
    public record GroupTopicItem(long chapterId, int chapterNumber, String title, Integer weekNumber, Integer displayOrder, boolean locked, boolean selected) {}
    public record GroupTopicRequest(long chapterId, int weekNumber, int displayOrder, boolean locked) {}

    public record CourseStudentItem(
            long id, String name, String rollNumber, String programme
    ) {}

    public record FacultyStudentItem(long id, String rollNumber, String name, String email,
            String branchId, String branchName, String branchCode, Long academicYearId,
            String academicYearName, int semester, String status, String createdAt,
            double attendancePercentage, long totalClasses, long presentClasses,
            boolean vePassed, String ve1Status, String ve1Taken, Long groupId,
            String groupName, String courseName, String courseCode, int taskCount) {}

    private record VeSummary(boolean passed, String status, String taken) {}

    public record BranchItem(long id, String code, String name, boolean active, String createdAt) {}

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
            String remarks,
            String startTime,
            String endTime,
            String room,
            Long groupId
    ) {}
}
