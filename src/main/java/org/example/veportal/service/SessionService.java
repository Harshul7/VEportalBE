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
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.mapper.SessionMapper;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.TeachingLogRepository;
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
    private final SessionMapper sessionMapper;

    public SessionService(ClassSessionRepository sessionRepository,
                          AttendanceRecordRepository attendanceRepository,
                          CourseMaterialRepository materialRepository,
                          TeachingLogRepository teachingLogRepository,
                          CurrentCourseProvider currentCourseProvider,
                          SessionMapper sessionMapper) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.materialRepository = materialRepository;
        this.teachingLogRepository = teachingLogRepository;
        this.currentCourseProvider = currentCourseProvider;
        this.sessionMapper = sessionMapper;
    }

    @Transactional(readOnly = true)
    public PagedResult<org.example.veportal.dto.response.SessionListItemResponse> list(String search, String status, int page, int size) {
        Specification<ClassSession> spec = (root, query, cb) -> cb.conjunction();
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
                    session.getTopic(),
                    session.getSessionDate().toString(),
                    session.getStartTime(),
                    session.getEndTime(),
                    session.getRoom(),
                    sessionMapper.facultyName(session),
                    Labels.of(session.getStatus()),
                    attendanceBySession.get(id),
                    materialsBySession.getOrDefault(id, 0L) > 0,
                    sessionsWithLogs.contains(id)
            );
        }).toList();

        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
        return new PagedResult<>(items, meta);
    }

    @Transactional
    public SessionResponse create(SessionCreateRequest request, UserAccount currentUser) {
        Course course = currentCourseProvider.requireCurrentCourse();
        ClassSession session = new ClassSession();
        session.setCourse(course);
        session.setSessionNumber(sessionRepository.findMaxSessionNumber(course.getId()) + 1);
        session.setTopic(request.topic().trim());
        session.setSessionDate(java.time.LocalDate.parse(request.date()));
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setRoom(request.room());
        session.setFaculty(currentUser);
        session.setStatus(Labels.toSessionStatus(request.status()));
        ClassSession saved = sessionRepository.save(session);
        return sessionMapper.toResponse(saved);
    }

    public ClassSession requireSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> NotFoundException.resource("Session", sessionId));
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
