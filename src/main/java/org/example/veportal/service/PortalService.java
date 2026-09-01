package org.example.veportal.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.veportal.dto.response.ActivityItemResponse;
import org.example.veportal.dto.response.AttendanceRecordResponse;
import org.example.veportal.dto.response.BootstrapResponse;
import org.example.veportal.dto.response.CourseInfoResponse;
import org.example.veportal.dto.response.MaterialResponse;
import org.example.veportal.dto.response.NotificationResponse;
import org.example.veportal.dto.response.ParticipationRecordResponse;
import org.example.veportal.dto.response.SessionResponse;
import org.example.veportal.dto.response.StudentResponse;
import org.example.veportal.dto.response.SummaryResponse;
import org.example.veportal.dto.response.TeachingLogResponse;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.Notification;
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.entity.TeachingLog;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.mapper.AcademicMapper;
import org.example.veportal.mapper.SessionMapper;
import org.example.veportal.mapper.StudentMapper;
import org.example.veportal.mapper.UserMapper;
import org.example.veportal.repository.ActivityLogRepository;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.ParticipationRecordRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.repository.TeachingLogConceptRepository;
import org.example.veportal.repository.TeachingLogRepository;
import org.example.veportal.util.Labels;
import org.example.veportal.util.Percent;
import org.example.veportal.util.RelativeTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CurrentCourseProvider currentCourseProvider;
    private final ClassSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final ParticipationRecordRepository participationRepository;
    private final CourseMaterialRepository materialRepository;
    private final TeachingLogRepository teachingLogRepository;
    private final TeachingLogConceptRepository conceptRepository;
    private final ActivityLogRepository activityLogRepository;

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final AcademicMapper academicMapper;
    private final SessionMapper sessionMapper;
    private final NotificationService notificationService;

    public PortalService(CurrentCourseProvider currentCourseProvider,
                         ClassSessionRepository sessionRepository,
                         StudentRepository studentRepository,
                         AttendanceRecordRepository attendanceRepository,
                         ParticipationRecordRepository participationRepository,
                         CourseMaterialRepository materialRepository,
                         TeachingLogRepository teachingLogRepository,
                         TeachingLogConceptRepository conceptRepository,
                         ActivityLogRepository activityLogRepository,
                         UserMapper userMapper,
                         StudentMapper studentMapper,
                         AcademicMapper academicMapper,
                         SessionMapper sessionMapper,
                         NotificationService notificationService) {
        this.currentCourseProvider = currentCourseProvider;
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.participationRepository = participationRepository;
        this.materialRepository = materialRepository;
        this.teachingLogRepository = teachingLogRepository;
        this.conceptRepository = conceptRepository;
        this.activityLogRepository = activityLogRepository;
        this.userMapper = userMapper;
        this.studentMapper = studentMapper;
        this.academicMapper = academicMapper;
        this.sessionMapper = sessionMapper;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public BootstrapResponse bootstrap(UserAccount currentUser) {
        Course course = currentCourseProvider.requireCurrentCourse();

        List<org.example.veportal.entity.ClassSession> sessions =
                sessionRepository.findAllWithFacultyByCourse(course.getId());
        List<StudentResponse> students = studentRepository.findAllByOrderByIdAsc().stream()
                .map(student -> studentMapper.toResponse(student, null))
                .toList();
        List<AttendanceRecordResponse> attendance = attendanceRepository.findAllWithStudentAndSession()
                .stream()
                .map(academicMapper::toResponse)
                .toList();
        List<ParticipationRecordResponse> participation = participationRepository.findAllWithStudentAndSession()
                .stream()
                .map(academicMapper::toResponse)
                .toList();
        List<MaterialResponse> materials = materialRepository.findAllWithUploaderOrderByIdAsc()
                .stream()
                .map(academicMapper::toResponse)
                .toList();

        List<TeachingLog> logs = teachingLogRepository.findAllOrderBySessionNumberDesc();
        Map<Long, List<String>> conceptsByLogId = new HashMap<>();
        if (!logs.isEmpty()) {
            for (org.example.veportal.entity.TeachingLogConcept concept :
                    conceptRepository.findByLogIds(logs.stream().map(TeachingLog::getId).toList())) {
                conceptsByLogId.computeIfAbsent(concept.getLog().getId(), k -> new java.util.ArrayList<>())
                        .add(concept.getLabel());
            }
        }
        List<TeachingLogResponse> teachingLogs = logs.stream()
                .map(log -> new TeachingLogResponse(
                        log.getId().toString(),
                        log.getSession().getId().toString(),
                        log.getContent(),
                        conceptsByLogId.getOrDefault(log.getId(), List.of()),
                        log.getRemarks() == null ? "" : log.getRemarks(),
                        sessionMapper.formatTimestamp(log.getUpdatedAt())
                ))
                .toList();

        List<NotificationResponse> notifications = notificationService.listFor(currentUser);

        List<ActivityItemResponse> activity = activityLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .stream()
                .map(item -> new ActivityItemResponse(
                        item.getId().toString(),
                        item.getTitle(),
                        item.getContextText() == null ? "" : item.getContextText(),
                        RelativeTime.format(item.getCreatedAt()),
                        Labels.of(item.getKind()),
                        item.getHref()
                ))
                .toList();

        SummaryResponse summary = buildSummary(course.getId());

        return new BootstrapResponse(
                userMapper.toResponse(currentUser),
                toCourseInfo(course),
                students,
                sessions.stream().map(sessionMapper::toResponse).toList(),
                attendance,
                participation,
                materials,
                teachingLogs,
                notifications,
                activity,
                summary
        );
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary() {
        Course course = currentCourseProvider.requireCurrentCourse();
        return buildSummary(course.getId());
    }

    private SummaryResponse buildSummary(Long courseId) {
        long activeStudents = studentRepository.countByStatus(AccountStatus.ACTIVE);
        long completedSessions = sessionRepository.countByCourseIdAndStatus(courseId, SessionStatus.COMPLETED);
        long totalAttendance = attendanceRepository.countForCompletedSessions();
        long presentAttendance = attendanceRepository.countPresentForCompletedSessions();
        long materialsCount = materialRepository.count();
        long savedLogs = teachingLogRepository.count();
        long sessionsWithParticipation = participationRepository.countSessionsWithRecords();
        Double overallPercent = Percent.of(presentAttendance, totalAttendance);
        return new SummaryResponse(
                activeStudents,
                completedSessions,
                overallPercent == null ? 0 : overallPercent,
                materialsCount,
                completedSessions,
                savedLogs,
                sessionsWithParticipation
        );
    }

    private CourseInfoResponse toCourseInfo(Course course) {
        return new CourseInfoResponse(
                course.getName(),
                course.getCode(),
                course.getTerm(),
                course.getFaculty() == null ? null : course.getFaculty().getFullName(),
                course.getStatus(),
                course.getSummary()
        );
    }
}
