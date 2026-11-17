package org.example.veportal.security;

import org.example.veportal.entity.Role;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.CourseGroupRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Central resource-level authorization used by method security expressions. */
@Component("resourceAuthorization")
public class ResourceAuthorizationService {

    private final AuthenticatedUserProvider users;
    private final ClassSessionRepository sessions;
    private final CourseFacultyRepository courseFaculty;
    private final CourseStudentRepository courseStudents;
    private final CourseGroupRepository groups;
    private final CourseMaterialRepository materials;

    public ResourceAuthorizationService(AuthenticatedUserProvider users,
                                        ClassSessionRepository sessions,
                                        CourseFacultyRepository courseFaculty,
                                        CourseStudentRepository courseStudents,
                                        CourseGroupRepository groups,
                                        CourseMaterialRepository materials) {
        this.users = users;
        this.sessions = sessions;
        this.courseFaculty = courseFaculty;
        this.courseStudents = courseStudents;
        this.groups = groups;
        this.materials = materials;
    }

    @Transactional(readOnly = true)
    public boolean canAccessSession(Long sessionId) {
        UserAccount user = users.currentUser();
        if (user.getRole().isCoordinator()) return true;
        if (user.getRole() == Role.TA) {
            return sessions.findById(sessionId).map(s -> (s.getGroup() != null
                    && s.getGroup().getTeachingAssistant() != null
                    && s.getGroup().getTeachingAssistant().getId().equals(user.getId()))).orElse(false);
        }
        return sessions.findById(sessionId)
                .map(s -> s.getGroup() != null
                        && user.getRole().isMentor()
                        && s.getGroup().getMentor() != null
                        && s.getGroup().getMentor().getId().equals(user.getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canAccessStudent(Long studentId, Long courseId) {
        UserAccount user = users.currentUser();
        if (user.getRole().isCoordinator()) return true;
        var authorizedGroupIds = groups.findAll().stream()
                .filter(group -> (user.getRole().isMentor() && group.getMentor() != null && group.getMentor().getId().equals(user.getId()))
                        || (user.getRole() == Role.TA && group.getTeachingAssistant() != null && group.getTeachingAssistant().getId().equals(user.getId())))
                .map(org.example.veportal.entity.CourseGroup::getId).toList();
        if (courseId != null) {
            return courseStudents.findByCourseId(courseId).stream()
                    .anyMatch(enrollment -> enrollment.getStudentId().equals(studentId)
                            && enrollment.getGroupId() != null && authorizedGroupIds.contains(enrollment.getGroupId()));
        }
        return courseStudents.findAll().stream()
                .anyMatch(enrollment -> enrollment.getStudentId().equals(studentId)
                        && enrollment.getGroupId() != null && authorizedGroupIds.contains(enrollment.getGroupId()));
    }

    public boolean canAccessMaterial(Long materialId) {
        UserAccount user = users.currentUser();
        if (user.getRole().isCoordinator()) return true;
        return materials.findById(materialId)
                .map(m -> {
            if (m.getSession() != null && m.getSession().getGroup() != null) {
                var group = m.getSession().getGroup();
                return (user.getRole().isMentor() && group.getMentor() != null
                        && group.getMentor().getId().equals(user.getId()))
                        || (user.getRole() == Role.TA && group.getTeachingAssistant() != null
                                && group.getTeachingAssistant().getId().equals(user.getId()));
                    }
                    // Preserve access to legacy course-level materials while new
                    // session materials use the group's assignment boundary.
                    return courseFaculty.existsByCourseIdAndFacultyId(m.getCourse().getId(), user.getId());
                })
                .orElse(false);
    }
}
