package org.example.veportal.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.example.veportal.dto.PageMeta;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.response.AdminTaskResponse;
import org.example.veportal.entity.SessionTask;
import org.example.veportal.repository.SessionTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTaskService {
    private final SessionTaskRepository repository;

    public AdminTaskService(SessionTaskRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PagedResult<AdminTaskResponse> list(String search, String status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<AdminTaskResponse> filtered = repository.findAll().stream()
                .filter(task -> status == null || status.isBlank() || status.equalsIgnoreCase(task.getStatus()))
                .map(this::toResponse)
                .filter(item -> term.isBlank() || contains(item.title(), term) || contains(item.groupName(), term)
                        || contains(item.mentorName(), term) || contains(item.mentorEmail(), term)
                        || contains(item.courseName(), term) || contains(item.courseCode(), term))
                .sorted(Comparator.comparing(AdminTaskResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        int totalPages = (int) Math.ceil(filtered.size() / (double) safeSize);
        return new PagedResult<>(filtered.subList(from, to), new PageMeta(safePage, safeSize, filtered.size(), totalPages));
    }

    private AdminTaskResponse toResponse(SessionTask task) {
        var session = task.getSession();
        var group = session.getGroup();
        var course = session.getCourse();
        var mentor = group == null ? null : group.getMentor();
        return new AdminTaskResponse(task.getId(), task.getTitle(), task.getTaskType(), task.getDescription(),
                task.getStartDate(), task.getEndDate(), task.getStatus(), task.getCreatedAt(), task.getCreatedBy(),
                session.getId(), session.getSessionNumber(), session.getTopic(), session.getSessionDate(),
                group == null ? null : group.getName(), mentor == null ? null : mentor.getId(),
                mentor == null ? null : mentor.getFullName(), mentor == null ? null : mentor.getEmail(),
                course.getCode(), course.getName(), course.getAcademicYear() == null ? null : course.getAcademicYear().getName());
    }

    private boolean contains(String value, String term) { return value != null && value.toLowerCase(Locale.ROOT).contains(term); }
}
