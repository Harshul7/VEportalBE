package org.example.veportal.service;

import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.PageMeta;
import org.example.veportal.dto.request.MaterialCreateRequest;
import org.example.veportal.dto.response.MaterialResponse;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseMaterial;
import org.example.veportal.entity.MaterialType;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.mapper.AcademicMapper;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.TopicRepository;
import org.example.veportal.util.Labels;
import org.example.veportal.util.SizeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CourseMaterialRepository materialRepository;
    private final ClassSessionRepository sessionRepository;
    private final CurrentCourseProvider currentCourseProvider;
    private final AcademicMapper academicMapper;
    private final ActivityService activityService;
    private final TopicRepository topicRepository;

    public MaterialService(CourseMaterialRepository materialRepository,
                           ClassSessionRepository sessionRepository,
                           CurrentCourseProvider currentCourseProvider,
                           AcademicMapper academicMapper,
                           ActivityService activityService,
                           TopicRepository topicRepository) {
        this.materialRepository = materialRepository;
        this.sessionRepository = sessionRepository;
        this.currentCourseProvider = currentCourseProvider;
        this.academicMapper = academicMapper;
        this.activityService = activityService;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public PagedResult<MaterialResponse> list(String search, String type, String sessionId, int page, int size) {
        Specification<CourseMaterial> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern));
        }
        if (type != null && !type.isBlank() && !"All".equalsIgnoreCase(type)) {
            MaterialType typeEnum = Labels.toMaterialType(type);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), typeEnum));
        }
        if (sessionId != null && !"All".equalsIgnoreCase(sessionId) && !sessionId.isBlank()) {
            if ("none".equalsIgnoreCase(sessionId)) {
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("session")));
            } else {
                try {
                    Long parsed = Long.parseLong(sessionId);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("session").get("id"), parsed));
                } catch (NumberFormatException e) {
                    throw new BusinessException("Invalid session filter: " + sessionId);
                }
            }
        }

        Page<CourseMaterial> result = materialRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), normalizeSize(size),
                        Sort.by(Sort.Direction.DESC, "uploadedAt").and(Sort.by(Sort.Direction.DESC, "id"))));

        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
        return new PagedResult<>(result.map(academicMapper::toResponse).toList(), meta);
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    @Transactional
    public MaterialResponse create(MaterialCreateRequest request, UserAccount currentUser) {
        Course course = currentCourseProvider.requireCurrentCourse();
        MaterialType type = Labels.toMaterialType(request.type());
        ClassSession session = null;
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            Long parsedSessionId;
            try {
                parsedSessionId = Long.parseLong(request.sessionId().trim());
            } catch (NumberFormatException e) {
                throw new BusinessException("Invalid session reference: " + request.sessionId());
            }
            session = sessionRepository.findById(parsedSessionId)
                    .orElseThrow(() -> NotFoundException.resource("Session", request.sessionId()));
        }
        CourseMaterial material = new CourseMaterial();
        material.setCourse(course);
        material.setSession(session);
        if (request.topicId() != null && !request.topicId().isBlank()) {
            try {
                material.setTopic(topicRepository.findById(Long.parseLong(request.topicId().trim()))
                        .orElseThrow(() -> NotFoundException.resource("Topic", request.topicId())));
            } catch (NumberFormatException e) {
                throw new BusinessException("Invalid topic reference: " + request.topicId());
            }
        }
        material.setTitle(request.title().trim());
        material.setType(type);
        material.setDescription(request.description() == null || request.description().isBlank()
                ? "Uploaded from faculty workspace."
                : request.description().trim());
        material.setFileName(request.fileName());
        material.setSizeBytes(SizeFormat.parse(request.size()));
        material.setUploadedBy(currentUser);
        material.setUploadedAt(java.time.LocalDate.now());
        CourseMaterial saved = materialRepository.save(material);

        activityService.record(currentUser,
                org.example.veportal.entity.ActivityKind.MATERIAL,
                "New course material uploaded",
                AttendanceService.truncate(saved.getTitle() + " — " + Labels.of(type), 255),
                "/materials");
        activityService.notifyUser(currentUser,
                "New material uploaded",
                saved.getTitle() + " was added to Course Materials.",
                "/materials");

        return academicMapper.toResponse(saved);
    }
}
