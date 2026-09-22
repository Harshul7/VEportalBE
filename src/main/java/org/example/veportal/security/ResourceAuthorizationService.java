package org.example.veportal.security;

import org.example.veportal.entity.Role;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.springframework.stereotype.Component;

/** Central resource-level authorization used by method security expressions. */
@Component("resourceAuthorization")
public class ResourceAuthorizationService {

    private final AuthenticatedUserProvider users;
    private final ClassSessionRepository sessions;
    private final CourseFacultyRepository courseFaculty;
    private final CourseStudentRepository courseStudents;
    private final CourseMaterialRepository materials;

    public ResourceAuthorizationService(AuthenticatedUserProvider users,
                                        ClassSessionRepository sessions,
                                        CourseFacultyRepository courseFaculty,
                                        CourseStudentRepository courseStudents,
                                        CourseMaterialRepository materials) {
        this.users = users;
        this.sessions = sessions;
        this.courseFaculty = courseFaculty;
        this.courseStudents = courseStudents;
        this.materials = materials;
    }

    public boolean canAccessSession(Long sessionId) {
        UserAccount user = users.currentUser();
        if (user.getRole() == Role.ADMIN) return true;
        return sessions.findById(sessionId)
                .map(s -> courseFaculty.existsByCourseIdAndFacultyId(s.getCourse().getId(), user.getId()))
                .orElse(false);
    }

    public boolean canAccessStudent(Long studentId, Long courseId) {
        UserAccount user = users.currentUser();
        if (user.getRole() == Role.ADMIN) return true;
        if (courseId != null) {
            return courseFaculty.existsByCourseIdAndFacultyId(courseId, user.getId())
                    && courseStudents.existsByCourseIdAndStudentId(courseId, studentId);
        }
        return courseFaculty.findByFacultyId(user.getId()).stream()
                .anyMatch(a -> courseStudents.existsByCourseIdAndStudentId(a.getCourseId(), studentId));
    }

    public boolean canAccessMaterial(Long materialId) {
        UserAccount user = users.currentUser();
        if (user.getRole() == Role.ADMIN) return true;
        return materials.findById(materialId)
                .map(m -> courseFaculty.existsByCourseIdAndFacultyId(m.getCourse().getId(), user.getId()))
                .orElse(false);
    }
}
