package org.example.veportal.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.example.veportal.config.AppProperties;
import org.example.veportal.dto.request.TaskCreateRequest;
import org.example.veportal.dto.response.TaskResponse;
import org.example.veportal.entity.SessionTask;
import org.example.veportal.entity.TaskDocument;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.SessionTaskRepository;
import org.example.veportal.repository.TaskDocumentRepository;
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

    public TaskService(SessionTaskRepository taskRepository, TaskDocumentRepository documentRepository,
                       SessionService sessionService, AppProperties properties) {
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.sessionService = sessionService;
        this.storageDirectory = Path.of(properties.storage().materialsDir()).toAbsolutePath().normalize().resolve("tasks");
        this.maxFileBytes = properties.storage().maxFileBytes();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long sessionId) {
        return taskRepository.findBySessionIdAndStatusOrderByDueDateAscIdAsc(sessionId, "ACTIVE")
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
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long sessionId, Long taskId, TaskCreateRequest request) {
        SessionTask task = requireForSession(sessionId, taskId);
        task.setTitle(request.title().trim());
        task.setTaskType(normalizeType(request.taskType()));
        task.setDescription(request.description() == null ? null : request.description().trim());
        task.setDueDate(request.dueDate());
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

    private TaskResponse toResponse(SessionTask task) {
        List<TaskResponse.TaskDocumentResponse> documents = documentRepository.findByTaskIdOrderByUploadedAtDesc(task.getId())
                .stream().map(d -> new TaskResponse.TaskDocumentResponse(d.getId(), d.getFileName(), d.getContentType(),
                        d.getSizeBytes(), d.getUploadedBy().getFullName(), d.getUploadedAt())).toList();
        return new TaskResponse(task.getId(), task.getSession().getId(), task.getTitle(), task.getTaskType(),
                task.getDescription(), task.getDueDate(), task.getStatus(), task.getCreatedBy(), task.getCreatedAt(), documents);
    }
}
