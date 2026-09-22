package org.example.veportal.service;

import java.util.List;
import java.util.Locale;
import org.example.veportal.dto.PageMeta;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.response.AdminActivityResponse;
import org.example.veportal.entity.ActivityKind;
import org.example.veportal.entity.ActivityLog;
import org.example.veportal.repository.ActivityLogRepository;
import org.example.veportal.util.Labels;
import org.example.veportal.util.RelativeTime;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminActivityService {

    private final ActivityLogRepository repository;

    public AdminActivityService(ActivityLogRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PagedResult<AdminActivityResponse> list(String search, String kind, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        ActivityKind requestedKind = parseKind(kind);
        List<ActivityLog> all = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AdminActivityResponse> filtered = all.stream()
                .filter(item -> requestedKind == null || item.getKind() == requestedKind)
                .filter(item -> term.isBlank() || contains(item.getTitle(), term) || contains(item.getContextText(), term)
                        || (item.getUser() != null && (contains(item.getUser().getFullName(), term) || contains(item.getUser().getEmail(), term))))
                .map(this::toResponse)
                .toList();
        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        List<AdminActivityResponse> content = filtered.subList(from, to);
        int totalPages = (int) Math.ceil(filtered.size() / (double) safeSize);
        return new PagedResult<>(content, new PageMeta(safePage, safeSize, filtered.size(), totalPages));
    }

    private AdminActivityResponse toResponse(ActivityLog item) {
        return new AdminActivityResponse(item.getId().toString(), item.getTitle(), item.getContextText() == null ? "" : item.getContextText(), RelativeTime.format(item.getCreatedAt()), Labels.of(item.getKind()), item.getHref(), item.getUser() == null ? null : item.getUser().getFullName(), item.getUser() == null ? null : item.getUser().getEmail());
    }

    private ActivityKind parseKind(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ActivityKind.valueOf(value.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) { return null; }
    }

    private boolean contains(String value, String term) { return value != null && value.toLowerCase(Locale.ROOT).contains(term); }
}
