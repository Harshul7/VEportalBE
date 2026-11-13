package org.example.veportal.service;

import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import org.example.veportal.entity.Course;
import org.example.veportal.dto.PageMeta;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.response.AdminActivityResponse;
import org.example.veportal.entity.ActivityKind;
import org.example.veportal.entity.ActivityLog;
import org.example.veportal.repository.ActivityLogRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.util.Labels;
import org.example.veportal.util.RelativeTime;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Stream;

@Service
public class AdminActivityService {

    private final ActivityLogRepository repository;
    private final CourseRepository courseRepository;

    public AdminActivityService(ActivityLogRepository repository, CourseRepository courseRepository) {
        this.repository = repository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminActivityResponse> list(String search, String kind, String groupName, Long academicYearId, Long courseId,
                                                  String fromDate, String toDate, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        ActivityKind requestedKind = parseKind(kind);
        String requestedGroup = groupName == null ? "" : groupName.trim();
        LocalDateTime fromDateTime = parseDate(fromDate, false);
        LocalDateTime toDateTime = parseDate(toDate, true);
        List<Course> courses = courseRepository.findAll();
        List<ActivityLog> all = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AdminActivityResponse> filtered = all.stream()
                .filter(item -> requestedKind == null || item.getKind() == requestedKind)
                .filter(item -> requestedGroup.isBlank() || requestedGroup.equalsIgnoreCase(item.getGroupName()))
                .filter(item -> courseId == null || matchesCourse(item, courseId, courses))
                .filter(item -> academicYearId == null || matchesAcademicYear(item, academicYearId, courses))
                .filter(item -> fromDateTime == null || !item.getCreatedAt().isBefore(fromDateTime))
                .filter(item -> toDateTime == null || item.getCreatedAt().isBefore(toDateTime))
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

    @Transactional(readOnly = true)
    public List<String> groups() {
        Stream<String> activityGroups = repository.findAll().stream().map(ActivityLog::getGroupName);
        Stream<String> courseGroups = courseRepository.findAll().stream().map(course -> course.getGroupName());
        return Stream.concat(activityGroups, courseGroups)
                .filter(value -> value != null && !value.isBlank())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private AdminActivityResponse toResponse(ActivityLog item) {
        Course course = item.getCourse();
        return new AdminActivityResponse(item.getId().toString(), item.getTitle(), item.getContextText() == null ? "" : item.getContextText(), RelativeTime.format(item.getCreatedAt()), Labels.of(item.getKind()), item.getHref(), item.getGroupName(), course == null ? null : course.getName(), course == null || course.getAcademicYear() == null ? null : course.getAcademicYear().getName(), item.getUser() == null ? null : item.getUser().getFullName(), item.getUser() == null ? null : item.getUser().getEmail());
    }

    private boolean matchesCourse(ActivityLog item, Long courseId, List<Course> courses) {
        if (item.getCourse() != null) return courseId.equals(item.getCourse().getId());
        Course requested = courses.stream().filter(course -> courseId.equals(course.getId())).findFirst().orElse(null);
        return requested != null && sameGroup(item, requested);
    }

    private boolean matchesAcademicYear(ActivityLog item, Long academicYearId, List<Course> courses) {
        if (item.getCourse() != null && item.getCourse().getAcademicYear() != null) {
            return academicYearId.equals(item.getCourse().getAcademicYear().getId());
        }
        return courses.stream().anyMatch(course -> course.getAcademicYear() != null
                && academicYearId.equals(course.getAcademicYear().getId()) && sameGroup(item, course));
    }

    private boolean sameGroup(ActivityLog item, Course course) {
        String activityGroup = item.getGroupName();
        return activityGroup != null && (activityGroup.equalsIgnoreCase(course.getGroupName())
                || activityGroup.equalsIgnoreCase(course.getName()));
    }

    private LocalDateTime parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return endOfDay ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
        } catch (DateTimeParseException ignored) { return null; }
    }

    private ActivityKind parseKind(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ActivityKind.valueOf(value.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) { return null; }
    }

    private boolean contains(String value, String term) { return value != null && value.toLowerCase(Locale.ROOT).contains(term); }
}
