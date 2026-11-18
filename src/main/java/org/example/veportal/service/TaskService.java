package org.example.veportal.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.veportal.config.AppProperties;
import org.example.veportal.dto.request.TaskCreateRequest;
import org.example.veportal.dto.response.TaskResponse;
import org.example.veportal.dto.response.TaskSearchResponse;
import org.example.veportal.entity.SessionTask;
import org.example.veportal.entity.TaskDocument;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.SessionTaskRepository;
import org.example.veportal.repository.TaskDocumentRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.security.ResourceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaskService {
    private final SessionTaskRepository taskRepository;
    private final TaskDocumentRepository documentRepository;
    private final SessionService sessionService;
    private final Path storageDirectory;
    private final long maxFileBytes;
    private final ResourceAuthorizationService authorization;
    private final ActivityService activityService;
    private final CourseStudentRepository courseStudentRepository;
    private final StudentRepository studentRepository;

    public TaskService(SessionTaskRepository taskRepository, TaskDocumentRepository documentRepository,
                       SessionService sessionService, AppProperties properties,
                       ResourceAuthorizationService authorization, ActivityService activityService,
                       CourseStudentRepository courseStudentRepository, StudentRepository studentRepository) {
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.sessionService = sessionService;
        this.storageDirectory = Path.of(properties.storage().materialsDir()).toAbsolutePath().normalize().resolve("tasks");
        this.maxFileBytes = properties.storage().maxFileBytes();
        this.authorization = authorization;
        this.activityService = activityService;
        this.courseStudentRepository = courseStudentRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskSearchResponse> search(String query, UserAccount user) {
        String term = query == null ? "" : query.trim().toLowerCase();
        return taskRepository.findByStatusOrderByStartDateAscIdAsc("ACTIVE").stream()
                .filter(task -> authorization.canAccessSession(task.getSession().getId()))
                .filter(task -> term.isBlank() || matches(task, term))
                .map(task -> new TaskSearchResponse(
                        task.getId(), task.getSession().getId(), task.getTitle(), task.getTaskType(),
                        task.getDescription(), task.getDueDate(), task.getStartDate(), task.getEndDate(),
                        task.getStatus(), task.getCreatedBy(), task.getCreatedAt(),
                        task.getSession().getCourse().getName(), task.getSession().getSessionNumber(),
                        task.getSession().getTopic(), task.getSession().getSessionDate()))
                .toList();
    }

    private boolean matches(SessionTask task, String term) {
        ClassSession session = task.getSession();
        return contains(task.getTitle(), term) || contains(task.getDescription(), term)
                || contains(task.getTaskType(), term) || contains(session.getTopic(), term)
                || contains(session.getCourse().getName(), term)
                || String.valueOf(session.getSessionNumber()).contains(term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long sessionId) {
        return taskRepository.findBySessionIdAndStatusOrderByStartDateAscIdAsc(sessionId, "ACTIVE")
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public TaskResponse create(Long sessionId, TaskCreateRequest request) {
        SessionTask task = new SessionTask();
        task.setSession(sessionService.requireSession(sessionId));
        task.setTitle(request.title().trim());
        task.setTaskType(normalizeType(request.taskType()));
        task.setDescription(request.description() == null ? null : request.description().trim());
        task.setDueDate(request.dueDate());
        applyDates(task, request);
        SessionTask saved = taskRepository.save(task);
        if (saved.getSession().getGroup() != null) {
            var group = saved.getSession().getGroup();
            if (group.getMentor() != null) activityService.notifyUser(group.getMentor(), "New group assignment", saved.getTitle(), "/class-sessions/" + sessionId);
            if (group.getTeachingAssistant() != null) activityService.notifyUser(group.getTeachingAssistant(), "New group assignment", saved.getTitle(), "/class-sessions/" + sessionId);
        }
        return toResponse(saved);
    }

    @Transactional
    public TaskResponse update(Long sessionId, Long taskId, TaskCreateRequest request) {
        SessionTask task = requireForSession(sessionId, taskId);
        task.setTitle(request.title().trim());
        task.setTaskType(normalizeType(request.taskType()));
        task.setDescription(request.description() == null ? null : request.description().trim());
        task.setDueDate(request.dueDate());
        applyDates(task, request);
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void archive(Long sessionId, Long taskId) {
        SessionTask task = requireForSession(sessionId, taskId);
        task.setStatus("ARCHIVED");
        taskRepository.save(task);
    }

    @Transactional
    public TaskResponse upload(Long sessionId, Long taskId, MultipartFile file, UserAccount user) {
        return upload(sessionId, taskId, file, null, user);
    }

    @Transactional
    public TaskResponse upload(Long sessionId, Long taskId, MultipartFile file, Long studentId, UserAccount user) {
        if (file == null || file.isEmpty()) throw new BusinessException("A non-empty task document is required");
        if (file.getSize() > maxFileBytes) throw new BusinessException("File exceeds the configured size limit");
        SessionTask task = requireForSession(sessionId, taskId);
        String original = file.getOriginalFilename() == null ? "task-document" :
                Path.of(file.getOriginalFilename()).getFileName().toString();
        String lower = original.toLowerCase();
        if (!(lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") ||
                lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".ppt") ||
                lower.endsWith(".pptx") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
            throw new BusinessException("Unsupported task document type");
        }
        String key = UUID.randomUUID() + (lower.contains(".") ? lower.substring(lower.lastIndexOf('.')) : "");
        try {
            Files.createDirectories(storageDirectory);
            Files.copy(file.getInputStream(), storageDirectory.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Unable to store task document");
        }
        TaskDocument document = new TaskDocument();
        document.setTask(task);
        if (studentId != null) {
            if (task.getSession().getGroup() == null
                    || !courseStudentRepository.existsByGroupIdAndStudentId(task.getSession().getGroup().getId(), studentId)) {
                throw new BusinessException("Selected student is not enrolled in this session's group");
            }
            document.setStudent(studentRepository.findById(studentId)
                    .orElseThrow(() -> NotFoundException.resource("Student", studentId)));
        }
        document.setFileName(original);
        document.setStorageKey(key);
        document.setContentType(file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setUploadedBy(user);
        document.setUploadedAt(LocalDateTime.now());
        documentRepository.save(document);
        return toResponse(task);
    }

    @Transactional
    public void deleteDocument(Long sessionId, Long taskId, Long documentId) {
        requireForSession(sessionId, taskId);
        TaskDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> NotFoundException.resource("Task document", documentId));
        if (!document.getTask().getId().equals(taskId)) throw NotFoundException.resource("Task document", documentId);
        try { Files.deleteIfExists(storedPath(document)); } catch (IOException e) { throw new BusinessException("Unable to remove task document"); }
        documentRepository.delete(document);
    }

    @Transactional(readOnly = true)
    public TaskDocument requireDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> NotFoundException.resource("Task document", documentId));
    }

    public Path storedPath(TaskDocument document) {
        Path path = storageDirectory.resolve(document.getStorageKey()).normalize();
        if (!path.startsWith(storageDirectory)) throw new BusinessException("Invalid task document reference");
        return path;
    }

    private SessionTask requireForSession(Long sessionId, Long taskId) {
        SessionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> NotFoundException.resource("Task", taskId));
        if (!task.getSession().getId().equals(sessionId)) throw NotFoundException.resource("Task", taskId);
        return task;
    }

    private String normalizeType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase().replace(' ', '_');
        if (!List.of("ASSIGNMENT", "HOMEWORK", "WRITTEN_ASSISTANCE", "OTHER").contains(value)) {
            throw new BusinessException("Task type must be Assignment, Homework, Written Assistance, or Other");
        }
        return value;
    }

    private void applyDates(SessionTask task, TaskCreateRequest request) {
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException("Task end date must be on or after the start date");
        }
        task.setStartDate(start);
        task.setEndDate(end);
        if (task.getDueDate() == null) task.setDueDate(end);
    }

    private TaskResponse toResponse(SessionTask task) {
        List<TaskResponse.TaskDocumentResponse> documents = documentRepository.findByTaskIdOrderByUploadedAtDesc(task.getId())
                .stream().map(d -> new TaskResponse.TaskDocumentResponse(d.getId(), d.getFileName(), d.getContentType(),
                        d.getSizeBytes(), d.getUploadedBy().getFullName(), d.getUploadedAt(),
                        d.getStudent() == null ? null : d.getStudent().getId(),
                        d.getStudent() == null ? null : d.getStudent().getFullName())).toList();
        return new TaskResponse(task.getId(), task.getSession().getId(), task.getTitle(), task.getTaskType(),
                task.getDescription(), task.getDueDate(), task.getStartDate(), task.getEndDate(), task.getStatus(),
                task.getCreatedBy(), task.getCreatedAt(), documents);
    }
}
