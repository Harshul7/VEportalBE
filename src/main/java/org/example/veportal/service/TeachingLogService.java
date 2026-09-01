package org.example.veportal.service;

import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.PageMeta;
import org.example.veportal.dto.request.TeachingLogSaveRequest;
import org.example.veportal.dto.response.TeachingLogHistoryItemResponse;
import org.example.veportal.dto.response.TeachingLogResponse;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.TeachingLog;
import org.example.veportal.entity.TeachingLogConcept;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.mapper.AcademicMapper;
import org.example.veportal.mapper.SessionMapper;
import org.example.veportal.repository.TeachingLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeachingLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TeachingLogRepository teachingLogRepository;
    private final SessionService sessionService;
    private final AcademicMapper academicMapper;
    private final SessionMapper sessionMapper;
    private final ActivityService activityService;

    public TeachingLogService(TeachingLogRepository teachingLogRepository,
                              SessionService sessionService,
                              AcademicMapper academicMapper,
                              SessionMapper sessionMapper,
                              ActivityService activityService) {
        this.teachingLogRepository = teachingLogRepository;
        this.sessionService = sessionService;
        this.academicMapper = academicMapper;
        this.sessionMapper = sessionMapper;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public TeachingLogResponse forSession(Long sessionId) {
        return teachingLogRepository.findWithConceptsBySessionId(sessionId)
                .map(academicMapper::toResponse)
                .orElse(null);
    }

    @Transactional
    public TeachingLogResponse upsert(Long sessionId, TeachingLogSaveRequest request, UserAccount currentUser) {
        ClassSession session = sessionService.requireSession(sessionId);
        TeachingLog log = teachingLogRepository.findWithConceptsBySessionId(session.getId())
                .orElseGet(() -> {
                    TeachingLog created = new TeachingLog();
                    created.setSession(session);
                    return created;
                });
        log.setContent(request.content().trim());
        log.setRemarks(request.remarks() == null ? "" : request.remarks().trim());
        log.getConcepts().clear();
        if (request.concepts() != null) {
            int position = 0;
            for (String raw : request.concepts()) {
                String label = raw == null ? "" : raw.trim();
                if (label.isEmpty()) {
                    continue;
                }
                TeachingLogConcept concept = new TeachingLogConcept();
                concept.setLog(log);
                concept.setLabel(label);
                concept.setPosition(position++);
                log.getConcepts().add(concept);
            }
        }
        log.setUpdatedByUser(currentUser);
        teachingLogRepository.save(log);

        activityService.record(currentUser,
                org.example.veportal.entity.ActivityKind.LOG,
                "Teaching log updated for Session " + session.getSessionNumber(),
                AttendanceService.truncate(session.getTopic(), 255),
                "/teaching-log?session=" + session.getId());
        activityService.notifyUser(currentUser,
                "Teaching log saved",
                "Session " + session.getSessionNumber() + " — " + session.getTopic(),
                "/teaching-log?session=" + session.getId());

        return teachingLogRepository.findWithConceptsBySessionId(session.getId())
                .map(academicMapper::toResponse)
                .orElseThrow(() -> new IllegalStateException("Teaching log could not be reloaded"));
    }

    @Transactional(readOnly = true)
    public PagedResult<TeachingLogHistoryItemResponse> history(int page, int size) {
        Page<TeachingLog> result = teachingLogRepository.findPageOrderBySessionNumberDesc(
                PageRequest.of(Math.max(page, 0), normalizeSize(size)));
        var items = result.map(log -> new TeachingLogHistoryItemResponse(
                log.getId().toString(),
                log.getSession().getId().toString(),
                log.getSession().getSessionNumber(),
                log.getSession().getSessionDate().toString(),
                log.getSession().getTopic(),
                sessionMapper.formatTimestamp(log.getUpdatedAt())
        )).toList();
        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
        return new PagedResult<>(items, meta);
    }

    private int normalizeSize(int size) {
        return size <= 0 ? 10 : Math.min(size, MAX_PAGE_SIZE);
    }
}
