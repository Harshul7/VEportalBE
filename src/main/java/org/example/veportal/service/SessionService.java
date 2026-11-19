package org.example.veportal.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.PageMeta;
import org.example.veportal.dto.request.SessionCreateRequest;
import org.example.veportal.dto.response.SessionResponse;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseGroup;
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.entity.UserAccount;
import java.time.LocalTime;
import java.time.LocalDate;
import org.example.veportal.entity.Role;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.mapper.SessionMapper;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.CourseGroupRepository;
import org.example.veportal.repository.GroupTopicPlanRepository;
import org.example.veportal.repository.TeachingLogRepository;
import org.example.veportal.repository.RoomRepository;
import org.example.veportal.util.Labels;
import org.example.veportal.util.Percent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final CourseMaterialRepository materialRepository;
    private final TeachingLogRepository teachingLogRepository;
    private final CurrentCourseProvider currentCourseProvider;
    private final CourseRepository courseRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final GroupTopicPlanRepository groupTopicPlanRepository;
    private final CourseFacultyRepository courseFacultyRepository;
    private final SessionMapper sessionMapper;
    private final RoomRepository roomRepository;

    public SessionService(ClassSessionRepository sessionRepository,
                          AttendanceRecordRepository attendanceRepository,
                          CourseMaterialRepository materialRepository,
                          TeachingLogRepository teachingLogRepository,
                          CurrentCourseProvider currentCourseProvider,
                          CourseRepository courseRepository,
                          CourseGroupRepository courseGroupRepository,
                          GroupTopicPlanRepository groupTopicPlanRepository,
                          CourseFacultyRepository courseFacultyRepository,
                          SessionMapper sessionMapper,
                          RoomRepository roomRepository) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.materialRepository = materialRepository;
        this.teachingLogRepository = teachingLogRepository;
        this.currentCourseProvider = currentCourseProvider;
        this.courseRepository = courseRepository;
        this.courseGroupRepository = courseGroupRepository;
        this.groupTopicPlanRepository = groupTopicPlanRepository;
        this.courseFacultyRepository = courseFacultyRepository;
        this.sessionMapper = sessionMapper;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public PagedResult<org.example.veportal.dto.response.SessionListItemResponse> list(String search, String status, int page, int size) {
        return list(search, status, page, size, null);
    }

    @Transactional(readOnly = true)
    public PagedResult<org.example.veportal.dto.response.SessionListItemResponse> list(String search, String status, int page, int size, UserAccount currentUser) {
        return list(search, status, null, null, null, null, page, size, currentUser);
    }

    @Transactional(readOnly = true)
    public PagedResult<org.example.veportal.dto.response.SessionListItemResponse> list(String search, String status,
            Long groupId, LocalDate fromDate, LocalDate toDate, Integer weekNumber, int page, int size,
            UserAccount currentUser) {
        Specification<ClassSession> spec = (root, query, cb) -> cb.conjunction();
        if (currentUser != null && currentUser.getRole() == Role.TA) {
            spec = spec.and((root, query, cb) -> {
                // A TA's group assignment is the sole operational boundary.
                // Session-support rows must not grant access to another group's
                // students or sessions.
                return cb.equal(root.get("group").get("teachingAssistant").get("id"), currentUser.getId());
            });
        } else if (currentUser != null && !currentUser.getRole().isCoordinator()) {
            // The group assignment is the operational access boundary. Do not use
            // legacy course/faculty mappings here because they can expose sessions
            // from another group in the same course.
            List<Long> groupIds = courseGroupRepository.findAll().stream()
                    .filter(group -> (currentUser.getRole().isMentor()
                            && group.getMentor() != null
                            && group.getMentor().getId().equals(currentUser.getId()))
                            || (currentUser.getRole() == Role.TA
                            && group.getTeachingAssistant() != null
                            && group.getTeachingAssistant().getId().equals(currentUser.getId())))
                    .map(org.example.veportal.entity.CourseGroup::getId)
                    .toList();
            spec = spec.and((root, query, cb) -> !groupIds.isEmpty()
                    ? root.get("group").get("id").in(groupIds)
                    : cb.disjunction());
        }
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> {
                String pattern = "%" + term + "%";
                if (term.matches("\\d+")) {
                    return cb.or(
                            cb.like(cb.lower(root.get("topic")), pattern),
                            cb.equal(root.get("sessionNumber"), Integer.parseInt(term)));
                }
                return cb.like(cb.lower(root.get("topic")), pattern);
            });
        }
        if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status)) {
            SessionStatus statusEnum = Labels.toSessionStatus(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusEnum));
        }
        if (groupId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("group").get("id"), groupId));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("sessionDate"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("sessionDate"), toDate));
        }
        if (weekNumber != null && weekNumber >= 1 && weekNumber <= 53) {
            spec = spec.and((root, query, cb) -> cb.equal(
                    cb.function("week", Integer.class, root.get("sessionDate")), weekNumber));
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "sessionNumber"));
        Page<ClassSession> result = sessionRepository.findAll(spec, pageRequest);

        List<Long> ids = result.map(ClassSession::getId).getContent();
        Map<Long, Double> attendanceBySession = attendancePercentages(ids);
        Map<Long, Long> materialsBySession = materialCounts(ids);
        List<Long> sessionsWithLogs = ids.isEmpty() ? List.of() : teachingLogRepository.findSessionIdsWithLogs(ids);

        List<org.example.veportal.dto.response.SessionListItemResponse> items = result.map(session -> {
            Long id = session.getId();
                    return new org.example.veportal.dto.response.SessionListItemResponse(
                    id.toString(),
                    session.getSessionNumber(),
                    session.getCourse().getId(),
                    session.getCourse().getName(),
                    session.getChapter() == null ? null : session.getChapter().getId(),
                    session.getChapter() == null ? "" : session.getChapter().getTitle(),
                    session.getChapter() == null ? 0 : session.getChapter().getChapterNumber(),
                    session.getTopic(),
                    session.getSessionDate().toString(),
                    session.getStartTime(),
                    session.getEndTime(),
                    session.getRoom(),
                    sessionMapper.facultyName(session),
                    Labels.of(session.getStatus()),
                    attendanceBySession.get(id),
                    materialsBySession.getOrDefault(id, 0L) > 0,
                    sessionsWithLogs.contains(id),
                    session.getGroup() == null ? null : session.getGroup().getId(),
                    session.getGroup() == null ? "" : session.getGroup().getName(),
                    session.getGroup() == null ? "" : session.getCourse().getCode() + "-" + session.getGroup().getName().replaceAll("\\s+", "-"),
                    session.getCourse().getAcademicYear() == null ? "" : session.getCourse().getAcademicYear().getName(),
                    session.getCourse().getSemesterRef() == null ? (session.getCourse().getSemester() == null ? "" : "Semester " + session.getCourse().getSemester()) : session.getCourse().getSemesterRef().getName(),
                    session.getGroup() == null || session.getGroup().getTeachingAssistant() == null ? "" : session.getGroup().getTeachingAssistant().getFullName(),
                    academicWeek(session)
            );
        }).toList();

        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
        return new PagedResult<>(items, meta);
    }

    private int academicWeek(ClassSession session) {
        if (session.getGroup() != null && session.getChapter() != null) {
            return groupTopicPlanRepository.findByGroupIdAndChapterId(session.getGroup().getId(), session.getChapter().getId())
                    .map(plan -> plan.getWeekNumber() == null ? 1 : plan.getWeekNumber())
                    .orElseGet(() -> session.getSessionDate().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
        }
        return session.getSessionDate().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }

    @Transactional
    public SessionResponse create(SessionCreateRequest request, UserAccount currentUser) {
        if (currentUser.getRole() == Role.TA) {
            throw new org.example.veportal.exception.BusinessException(
                    "Teaching Assistants support sessions but cannot create them");
        }
        if (!currentUser.getRole().isCoordinator() && request.groupId() == null) {
            throw new org.example.veportal.exception.BusinessException(
                    "Select one of your assigned groups before creating a session");
        }
        validateTimeRange(request.startTime(), request.endTime());
        Course course;
        if (request.courseId() == null && !currentUser.getRole().isCoordinator()) {
            Long assignedCourseId = courseFacultyRepository.findByFacultyId(currentUser.getId()).stream()
                    .map(org.example.veportal.entity.CourseFaculty::getCourseId).findFirst()
                    .orElseThrow(() -> new org.example.veportal.exception.BusinessException(
                            "You are not assigned to any course"));
            course = courseRepository.findById(assignedCourseId)
                    .orElseThrow(() -> NotFoundException.resource("Course", assignedCourseId));
        } else {
            course = request.courseId() == null
                    ? currentCourseProvider.requireCurrentCourse()
                    : courseRepository.findById(request.courseId())
                        .orElseThrow(() -> NotFoundException.resource("Course", request.courseId()));
        }
        if (!currentUser.getRole().isCoordinator() && request.groupId() == null
                && !courseFacultyRepository.existsByCourseIdAndFacultyId(course.getId(), currentUser.getId())) {
            throw new org.example.veportal.exception.BusinessException("You are not assigned to this course");
        }
        CourseGroup group = null;
        if (request.groupId() != null) {
            group = courseGroupRepository.findById(request.groupId())
                    .orElseThrow(() -> NotFoundException.resource("Group", request.groupId()));
            if (!group.getCourse().getId().equals(course.getId())) {
                throw new BusinessException("Selected group does not belong to the selected course");
            }
            if (!currentUser.getRole().isCoordinator()
                    && (group.getMentor() == null || !group.getMentor().getId().equals(currentUser.getId()))) {
                throw new BusinessException("You can only create sessions for your assigned group");
            }
            if (sessionRepository.existsScheduleConflict(group.getId(), currentUser.getId(),
                    LocalDate.parse(request.date()), request.startTime(), request.endTime(), null) > 0) {
                throw new BusinessException("This group or mentor already has a session scheduled in the selected time range");
            }
        }
        ClassSession session = new ClassSession();
        session.setCourse(course);
        session.setGroup(group);
        session.setSessionNumber(sessionRepository.findMaxSessionNumber(course.getId()) + 1);
        session.setTopic(request.topic().trim());
        session.setTitle(request.title() == null || request.title().isBlank()
                ? request.topic().trim() : request.title().trim());
        session.setMode(request.mode());
        session.setSessionType(request.sessionType());
        session.setDescription(request.description());
        session.setSessionDate(java.time.LocalDate.parse(request.date()));
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        String room = request.room() == null || request.room().isBlank() ? "SH-1" : request.room().trim();
        validateRoom(room);
        session.setRoom(room);
        session.setFaculty(currentUser);
        session.setStatus(Labels.toSessionStatus(request.status()));
        ClassSession saved = sessionRepository.save(session);
        return sessionMapper.toResponse(saved);
    }

    private void validateTimeRange(String startTime, String endTime) {
        try {
            if (!startTime.matches("\\d{2}:\\d{2}") || !endTime.matches("\\d{2}:\\d{2}")) {
                throw new org.example.veportal.exception.BusinessException("Times must be valid 24-hour values in HH:mm format");
            }
            if (!LocalTime.parse(endTime).isAfter(LocalTime.parse(startTime))) {
                throw new org.example.veportal.exception.BusinessException("End time must be later than start time");
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new org.example.veportal.exception.BusinessException("Times must be valid 24-hour values in HH:mm format");
        }
    }

    private void validateRoom(String room) {
        roomRepository.findByCodeIgnoreCase(room).filter(org.example.veportal.entity.Room::isActive)
                .orElseThrow(() -> new BusinessException("Select an active room from master data"));
    }

    public ClassSession requireSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> NotFoundException.resource("Session", sessionId));
    }

    @Transactional(readOnly = true)
    public SessionResponse get(Long sessionId) {
        return sessionMapper.toResponse(requireSession(sessionId));
    }

    @Transactional
    public SessionResponse updateStatus(Long sessionId, String status) {
        ClassSession session = requireSession(sessionId);
        session.setStatus(Labels.toSessionStatus(status));
        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    private Map<Long, Double> attendancePercentages(Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> result = new HashMap<>();
        for (AttendanceRecordRepository.SessionAttendanceAggregation agg :
                attendanceRepository.aggregateBySessions(sessionIds)) {
            Double percent = Percent.of(agg.getPresent(), agg.getTotal());
            if (percent != null) {
                result.put(agg.getSessionId(), percent);
            }
        }
        return result;
    }

    private Map<Long, Long> materialCounts(Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (CourseMaterialRepository.SessionMaterialAggregation agg : materialRepository.countBySessionIds(sessionIds)) {
            result.put(agg.getSessionId(), agg.getMaterialCount());
        }
        return result;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
