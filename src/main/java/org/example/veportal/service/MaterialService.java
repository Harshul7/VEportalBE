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
import org.example.veportal.entity.Role;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.mapper.AcademicMapper;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.TopicRepository;
import org.example.veportal.repository.ChapterRepository;
import org.example.veportal.util.Labels;
import org.example.veportal.util.SizeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.example.veportal.config.AppProperties;

@Service
public class MaterialService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CourseMaterialRepository materialRepository;
    private final ClassSessionRepository sessionRepository;
    private final CurrentCourseProvider currentCourseProvider;
    private final AcademicMapper academicMapper;
    private final ActivityService activityService;
    private final TopicRepository topicRepository;
    private final ChapterRepository chapterRepository;
    private final CourseFacultyRepository courseFacultyRepository;
    private final Path storageDirectory;
    private final long maxFileBytes;

    public MaterialService(CourseMaterialRepository materialRepository,
                           ClassSessionRepository sessionRepository,
                           CurrentCourseProvider currentCourseProvider,
                           AcademicMapper academicMapper,
                           ActivityService activityService,
                           TopicRepository topicRepository,
                           ChapterRepository chapterRepository,
                           CourseFacultyRepository courseFacultyRepository,
                           AppProperties properties) {
        this.materialRepository = materialRepository;
        this.sessionRepository = sessionRepository;
        this.currentCourseProvider = currentCourseProvider;
        this.academicMapper = academicMapper;
        this.activityService = activityService;
        this.topicRepository = topicRepository;
        this.chapterRepository = chapterRepository;
        this.courseFacultyRepository = courseFacultyRepository;
        this.storageDirectory = Path.of(properties.storage().materialsDir()).toAbsolutePath().normalize();
        this.maxFileBytes = properties.storage().maxFileBytes();
    }

    @Transactional(readOnly = true)
    public PagedResult<MaterialResponse> list(String search, String type, String sessionId, String chapterId, int page, int size, UserAccount currentUser) {
        Specification<CourseMaterial> spec = (root, query, cb) -> cb.conjunction();
        if (currentUser.getRole() != Role.ADMIN) {
            var courseIds = courseFacultyRepository.findByFacultyId(currentUser.getId()).stream()
                    .map(org.example.veportal.entity.CourseFaculty::getCourseId).toList();
            spec = spec.and((root, query, cb) -> courseIds.isEmpty()
                    ? cb.disjunction() : root.get("course").get("id").in(courseIds));
        }
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
        if (chapterId != null && !chapterId.isBlank() && !"All".equalsIgnoreCase(chapterId)) {
            try {
                Long parsed = Long.parseLong(chapterId);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("chapter").get("id"), parsed));
            } catch (NumberFormatException e) {
                throw new BusinessException("Invalid chapter filter: " + chapterId);
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
                var topic = topicRepository.findById(Long.parseLong(request.topicId().trim()))
                        .orElseThrow(() -> NotFoundException.resource("Topic", request.topicId()));
                material.setTopic(topic);
                material.setChapter(topic.getChapter());
            } catch (NumberFormatException e) {
                throw new BusinessException("Invalid topic reference: " + request.topicId());
            }
        }
        if (request.chapterId() != null && !request.chapterId().isBlank() && material.getChapter() == null) {
            try {
                material.setChapter(chapterRepository.findById(Long.parseLong(request.chapterId().trim()))
                        .orElseThrow(() -> NotFoundException.resource("Chapter", request.chapterId())));
            } catch (NumberFormatException e) {
                throw new BusinessException("Invalid chapter reference: " + request.chapterId());
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

    @Transactional
    public MaterialResponse createUpload(MultipartFile file, MaterialCreateRequest request, UserAccount currentUser) {
        if (file == null || file.isEmpty()) throw new BusinessException("A non-empty file is required");
        if (file.getSize() > maxFileBytes) throw new BusinessException("File exceeds the configured size limit");
        String original = file.getOriginalFilename() == null ? "material" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String lower = original.toLowerCase();
        if (!(lower.endsWith(".pdf") || lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
            throw new BusinessException("Unsupported material file type");
        }
        MaterialResponse response = create(new MaterialCreateRequest(
                request.title(), request.type(), request.sessionId(), request.topicId(), request.chapterId(), request.description(), original,
                SizeFormat.format(file.getSize())), currentUser);
        Long id = Long.valueOf(response.id());
        CourseMaterial material = materialRepository.findById(id).orElseThrow();
        String key = UUID.randomUUID() + lower.substring(lower.lastIndexOf('.'));
        try {
            Files.createDirectories(storageDirectory);
            Files.copy(file.getInputStream(), storageDirectory.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            materialRepository.delete(material);
            throw new BusinessException("Unable to store material file");
        }
        material.setStorageKey(key);
        material.setContentType(file.getContentType());
        materialRepository.save(material);
        return academicMapper.toResponse(material);
    }

    @Transactional(readOnly = true)
    public CourseMaterial requireStored(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> NotFoundException.resource("Material", id));
    }

    public Path storedPath(CourseMaterial material) {
        if (material.getStorageKey() == null || material.getStorageKey().isBlank()) throw new BusinessException("Material file is not available");
        Path path = storageDirectory.resolve(material.getStorageKey()).normalize();
        if (!path.startsWith(storageDirectory)) throw new BusinessException("Invalid material storage reference");
        return path;
    }
}
