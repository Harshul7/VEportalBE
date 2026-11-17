package org.example.veportal.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.entity.AttendanceRecord;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.CourseGroup;
import org.example.veportal.entity.CourseStudent;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.SessionTask;
import org.example.veportal.entity.StudentCourseResult;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseGroupRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.GroupTopicPlanRepository;
import org.example.veportal.repository.SessionTaskRepository;
import org.example.veportal.repository.StudentCourseResultRepository;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/** Group-scoped reporting endpoint. Course is context; the group is the access boundary. */
@RestController
@RequestMapping("/api/group-mentor/reports")
@PreAuthorize("hasAnyRole('GROUP_MENTOR','FACULTY','FACULTY_COORDINATOR','ADMIN','TA')")
public class GroupReportController {
    private final AuthenticatedUserProvider users;
    private final CourseGroupRepository groups;
    private final CourseStudentRepository enrollments;
    private final ClassSessionRepository sessions;
    private final AttendanceRecordRepository attendance;
    private final SessionTaskRepository tasks;
    private final GroupTopicPlanRepository topicPlans;
    private final StudentCourseResultRepository results;
    private final ExportService exports;

    public GroupReportController(AuthenticatedUserProvider users, CourseGroupRepository groups,
            CourseStudentRepository enrollments, ClassSessionRepository sessions,
            AttendanceRecordRepository attendance, SessionTaskRepository tasks,
            GroupTopicPlanRepository topicPlans, StudentCourseResultRepository results,
            ExportService exports) {
        this.users = users; this.groups = groups; this.enrollments = enrollments; this.sessions = sessions;
        this.attendance = attendance; this.tasks = tasks; this.topicPlans = topicPlans;
        this.results = results; this.exports = exports;
    }

    @GetMapping("/filters")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<GroupOption>>> filters() {
        UserAccount user = users.currentUser();
        List<GroupOption> values = groups.findAll().stream()
                .filter(g -> canUse(user, g))
                .sorted(Comparator.comparing(CourseGroup::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::option).toList();
        return ResponseEntity.ok(ApiResponse.success(values, "Report filters retrieved"));
    }

    @GetMapping("/preview")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<GroupReport>> preview(@RequestParam Long groupId,
            @RequestParam(defaultValue = "STUDENT_PERFORMANCE") String reportType,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) String status) {
        GroupReport report = build(groupId, reportType, studentId, fromDate, toDate, weekNumber, status);
        return ResponseEntity.ok(ApiResponse.success(report, "Report generated"));
    }

    @GetMapping("/export")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> export(@RequestParam Long groupId,
            @RequestParam(defaultValue = "STUDENT_PERFORMANCE") String reportType,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "xlsx") String format) {
        GroupReport report = build(groupId, reportType, studentId, fromDate, toDate, weekNumber, status);
        List<String> headers = report.rows().isEmpty() ? List.of("Metric", "Value")
                : new ArrayList<>(report.rows().get(0).keySet());
        List<List<String>> rows = report.rows().stream().map(row -> headers.stream()
                .map(header -> String.valueOf(row.getOrDefault(header, ""))).toList()).toList();
        String normalized = format.equalsIgnoreCase("pdf") ? "pdf" : "xlsx";
        byte[] body = normalized.equals("pdf")
                ? exports.pdf(report.reportType() + " - " + report.group().groupName(), headers, rows)
                : exports.xlsx(report.reportType() + " - " + report.group().groupName(), headers, rows);
        MediaType type = normalized.equals("pdf") ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"group-report-" + report.group().groupName().replaceAll("[^A-Za-z0-9_-]", "-") + "." + normalized + "\"")
                .contentType(type).body(body);
    }

    private GroupReport build(Long groupId, String requestedType, Long studentId, LocalDate fromDate,
            LocalDate toDate, Integer weekNumber, String status) {
        CourseGroup group = groups.findById(groupId).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Group not found"));
        if (!canUse(users.currentUser(), group)) throw new ResponseStatusException(FORBIDDEN, "You are not assigned to this group");
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate))
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "End date must not precede start date");
        String type = requestedType == null ? "STUDENT_PERFORMANCE" : requestedType.toUpperCase();
        if (users.currentUser().getRole() == Role.TA && "RESULTS".equals(type)) {
            throw new ResponseStatusException(FORBIDDEN, "Teaching Assistants cannot access official result reports");
        }
        Set<Long> studentIds = enrollments.findByGroupId(groupId).stream().map(CourseStudent::getStudentId).collect(Collectors.toSet());
        if (studentId != null && !studentIds.contains(studentId)) throw new ResponseStatusException(FORBIDDEN, "Student is not enrolled in this group");
        List<ClassSession> groupSessions = sessions.findAll().stream().filter(s -> s.getGroup() != null && groupId.equals(s.getGroup().getId()))
                .filter(s -> fromDate == null || !s.getSessionDate().isBefore(fromDate))
                .filter(s -> toDate == null || !s.getSessionDate().isAfter(toDate))
                .filter(s -> weekNumber == null || week(s.getSessionDate()) == weekNumber)
                .filter(s -> status == null || status.isBlank() || s.getStatus().name().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(ClassSession::getSessionDate).thenComparing(ClassSession::getSessionNumber)).toList();
        List<AttendanceRecord> attendanceRows = attendance.findAll().stream().filter(a -> a.getSession() != null && groupId.equals(a.getSession().getGroup() == null ? null : a.getSession().getGroup().getId()))
                .filter(a -> groupSessions.stream().anyMatch(s -> s.getId().equals(a.getSession().getId()))).toList();
        Map<Long, List<AttendanceRecord>> byStudent = attendanceRows.stream().collect(Collectors.groupingBy(a -> a.getStudent().getId()));
        List<SessionTask> groupTasks = tasks.findAll().stream().filter(t -> t.getSession() != null && t.getSession().getGroup() != null && groupId.equals(t.getSession().getGroup().getId()))
                .filter(t -> fromDate == null || t.getStartDate() == null || !t.getStartDate().isBefore(fromDate))
                .filter(t -> toDate == null || t.getEndDate() == null || !t.getEndDate().isAfter(toDate)).toList();
        List<Map<String, Object>> rows = switch (type) {
            case "ATTENDANCE" -> attendanceReport(enrollments.findByGroupId(groupId), byStudent);
            case "SESSIONS" -> groupSessions.stream().map(this::sessionRow).toList();
            case "TOPICS" -> topicPlans.findByGroupIdOrderByWeekNumberAscDisplayOrderAscIdAsc(groupId).stream().map(p -> {
                Map<String,Object> r = new LinkedHashMap<>(); r.put("Week", p.getWeekNumber()); r.put("Order", p.getDisplayOrder()); r.put("Topic", p.getChapter().getTitle()); r.put("Locked", p.isLocked()); return r;
            }).toList();
            case "TASKS" -> groupTasks.stream().map(t -> { Map<String,Object> r = new LinkedHashMap<>(); r.put("Task", t.getTitle()); r.put("Session", t.getSession().getTitle() == null ? t.getSession().getTopic() : t.getSession().getTitle()); r.put("Start", t.getStartDate()); r.put("End", t.getEndDate()); r.put("Status", t.getStatus()); return r; }).toList();
            case "RESULTS" -> resultReport(enrollments.findByGroupId(groupId));
            case "STUDENT_PERFORMANCE" -> performanceReport(enrollments.findByGroupId(groupId), byStudent, studentId);
            default -> throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported report type");
        };
        Map<String,Object> summary = new LinkedHashMap<>(); summary.put("students", studentIds.size()); summary.put("sessions", groupSessions.size()); summary.put("tasks", groupTasks.size()); summary.put("attendanceRecords", attendanceRows.size());
        return new GroupReport(type, option(group), LocalDateTime.now(), summary, rows);
    }

    private List<Map<String,Object>> attendanceReport(List<CourseStudent> roster, Map<Long,List<AttendanceRecord>> byStudent) {
        return roster.stream().map(e -> { List<AttendanceRecord> rs = byStudent.getOrDefault(e.getStudentId(), List.of()); long present = rs.stream().filter(a -> "PRESENT".equals(a.getStatus().name())).count(); Map<String,Object> r = new LinkedHashMap<>(); r.put("Roll Number", e.getStudent().getStudentCode()); r.put("Student", e.getStudent().getFullName()); r.put("Present", present); r.put("Recorded", rs.size()); r.put("Attendance %", rs.isEmpty() ? 0 : Math.round(present * 10000.0 / rs.size()) / 100.0); return r; }).toList();
    }
    private List<Map<String,Object>> performanceReport(List<CourseStudent> roster, Map<Long,List<AttendanceRecord>> byStudent, Long selected) { return roster.stream().filter(e -> selected == null || e.getStudentId().equals(selected)).map(e -> { List<AttendanceRecord> rs=byStudent.getOrDefault(e.getStudentId(),List.of()); long p=rs.stream().filter(a->"PRESENT".equals(a.getStatus().name())).count(); Map<String,Object> r=new LinkedHashMap<>(); r.put("Roll Number",e.getStudent().getStudentCode()); r.put("Student",e.getStudent().getFullName()); r.put("Attendance %",rs.isEmpty()?"N/A":Math.round(p*10000.0/rs.size())/100.0); r.put("Result", results.findByStudentId(e.getStudentId()).stream().filter(x->x.getGroup()!=null&&x.getGroup().getId().equals(e.getGroupId())).filter(x->x.getPublishedAt()!=null).map(x->x.getFinalResult()==null?"NOT EVALUATED":x.getFinalResult().name()).findFirst().orElse("NOT EVALUATED")); return r; }).toList(); }
    private List<Map<String,Object>> resultReport(List<CourseStudent> roster) { return roster.stream().map(e -> { Map<String,Object> r=new LinkedHashMap<>(); r.put("Roll Number",e.getStudent().getStudentCode()); r.put("Student",e.getStudent().getFullName()); r.put("Result", results.findByStudentId(e.getStudentId()).stream().filter(x->x.getGroup()!=null&&x.getGroup().getId().equals(e.getGroupId())).filter(x->x.getPublishedAt()!=null).map(x->x.getFinalResult()==null?"NOT EVALUATED":x.getFinalResult().name()).findFirst().orElse("NOT EVALUATED")); return r; }).toList(); }
    private Map<String,Object> sessionRow(ClassSession s) { Map<String,Object> r=new LinkedHashMap<>(); r.put("Date",s.getSessionDate()); r.put("Week",week(s.getSessionDate())); r.put("Title",s.getTitle()==null?s.getTopic():s.getTitle()); r.put("Topic",s.getTopic()); r.put("Status",s.getStatus()); r.put("Start",s.getStartTime()); r.put("End",s.getEndTime()); return r; }
    private GroupOption option(CourseGroup g) { return new GroupOption(g.getId(), g.getName(), g.getName(), g.getCourse().getCode(), g.getCourse().getName(), g.getCourse().getAcademicYear()==null?"":g.getCourse().getAcademicYear().getName(), g.getMentor()==null?null:g.getMentor().getFullName(), g.getTeachingAssistant()==null?null:g.getTeachingAssistant().getFullName()); }
    private boolean canUse(UserAccount user, CourseGroup g) { return user.getRole().isCoordinator() || (user.getRole().isMentor() && g.getMentor()!=null && g.getMentor().getId().equals(user.getId())) || (user.getRole()==Role.TA && g.getTeachingAssistant()!=null && g.getTeachingAssistant().getId().equals(user.getId())); }
    private int week(LocalDate date) { return date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()); }

    public record GroupOption(Long groupId, String groupName, String groupCode, String courseCode, String courseName, String academicYear, String mentorName, String teachingAssistantName) {}
    public record GroupReport(String reportType, GroupOption group, LocalDateTime generatedAt, Map<String,Object> summary, List<Map<String,Object>> rows) {}
}
