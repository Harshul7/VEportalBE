package org.example.veportal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.veportal.dto.response.SearchResultResponse;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseMaterial;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalSearchService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;

    private final UserAccountRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseMaterialRepository materialRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public GlobalSearchService(UserAccountRepository userRepository,
                               StudentRepository studentRepository,
                               CourseRepository courseRepository,
                               CourseMaterialRepository materialRepository,
                               AuthenticatedUserProvider authenticatedUserProvider) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.materialRepository = materialRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional(readOnly = true)
    public List<SearchResultResponse> search(String query, int requestedLimit) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (term.length() < 2) return List.of();
        int limit = Math.max(1, Math.min(requestedLimit <= 0 ? DEFAULT_LIMIT : requestedLimit, MAX_LIMIT));
        boolean admin = authenticatedUserProvider.currentUser().getRole() == Role.ADMIN;
        List<SearchResultResponse> matches = new ArrayList<>();

        for (UserAccount user : userRepository.findAll()) {
            if (contains(term, user.getFullName(), user.getEmail(), user.getStaffCode())) {
                matches.add(new SearchResultResponse("User", user.getId().toString(), user.getFullName(), user.getEmail(), admin ? "/admin/credentials" : "/profile"));
            }
        }
        for (Student student : studentRepository.findAll()) {
            if (contains(term, student.getFullName(), student.getEmail(), student.getStudentCode(), student.getProgramme())) {
                matches.add(new SearchResultResponse("Student", student.getId().toString(), student.getFullName(), student.getStudentCode() + " · " + student.getProgramme(), admin ? "/admin/students" : "/students/" + student.getId()));
            }
        }
        for (Course course : courseRepository.findAll()) {
            if (contains(term, course.getCode(), course.getName(), course.getTerm(), course.getSummary())) {
                matches.add(new SearchResultResponse("Course", course.getId().toString(), course.getCode() + " · " + course.getName(), nullToEmpty(course.getTerm()), admin ? "/admin/academic" : "/courses/" + course.getId()));
            }
        }
        for (CourseMaterial material : materialRepository.findAll()) {
            if (contains(term, material.getTitle(), material.getDescription(), material.getFileName())) {
                String courseName = material.getCourse() == null ? "" : material.getCourse().getName();
                matches.add(new SearchResultResponse("Material", material.getId().toString(), material.getTitle(), courseName, admin ? "/admin/academic" : "/materials"));
            }
        }
        return matches.stream().limit(limit).toList();
    }

    private boolean contains(String term, String... values) {
        for (String value : values) if (value != null && value.toLowerCase(Locale.ROOT).contains(term)) return true;
        return false;
    }

    private String nullToEmpty(String value) { return value == null ? "" : value; }
}
