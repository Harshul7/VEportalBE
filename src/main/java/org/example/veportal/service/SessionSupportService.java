package org.example.veportal.service;

import java.util.List;
import org.example.veportal.dto.request.SessionSupportRequest;
import org.example.veportal.dto.response.SessionSupportResponse;
import org.example.veportal.dto.response.SupportStaffResponse;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.SessionSupportAssignment;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.SessionSupportAssignmentRepository;
import org.example.veportal.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionSupportService {
    private final SessionSupportAssignmentRepository assignments;
    private final SessionService sessions;
    private final UserAccountRepository users;
    private final CourseFacultyRepository courseFaculty;

    public SessionSupportService(SessionSupportAssignmentRepository assignments, SessionService sessions,
                                 UserAccountRepository users, CourseFacultyRepository courseFaculty) {
        this.assignments = assignments; this.sessions = sessions; this.users = users; this.courseFaculty = courseFaculty;
    }

    @Transactional(readOnly = true)
    public List<SessionSupportResponse> list(Long sessionId) {
        return assignments.findBySessionIdOrderByCreatedAtAsc(sessionId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportStaffResponse> staff(Long sessionId, UserAccount actor) {
        var session = sessions.requireSession(sessionId);
        List<UserAccount> candidates = users.findAllByRoleOrderByFullNameAsc(Role.TA);
        if (actor.getRole().isCoordinator()) return candidates.stream().map(this::toStaff).toList();
        return courseFaculty.findByCourseId(session.getCourse().getId()).stream()
                .map(item -> item.getFaculty())
                .filter(user -> user != null && user.getRole() == Role.TA)
                .map(this::toStaff).toList();
    }

    @Transactional
    public SessionSupportResponse assign(Long sessionId, SessionSupportRequest request, UserAccount actor) {
        var session = sessions.requireSession(sessionId);
        UserAccount ta = users.findById(request.teachingAssistantId())
                .orElseThrow(() -> NotFoundException.resource("Teaching Assistant", request.teachingAssistantId()));
        if (ta.getRole() != Role.TA) throw new BusinessException("Only a Teaching Assistant can be assigned as session support");
        if (!actor.getRole().isCoordinator() && !courseFaculty.existsByCourseIdAndFacultyId(session.getCourse().getId(), actor.getId())) {
            throw new BusinessException("You are not assigned to this course");
        }
        if (!actor.getRole().isCoordinator() && !courseFaculty.existsByCourseIdAndFacultyId(session.getCourse().getId(), ta.getId())) {
            throw new BusinessException("The Teaching Assistant must be mapped to this course first");
        }
        if (assignments.existsBySessionIdAndTeachingAssistantId(sessionId, ta.getId())) {
            return list(sessionId).stream().filter(item -> item.teachingAssistantId().equals(ta.getId())).findFirst().orElseThrow();
        }
        SessionSupportAssignment assignment = new SessionSupportAssignment();
        assignment.setSession(session); assignment.setTeachingAssistant(ta); assignment.setAssignedBy(actor);
        return toResponse(assignments.save(assignment));
    }

    @Transactional
    public void remove(Long sessionId, Long taId, UserAccount actor) {
        var session = sessions.requireSession(sessionId);
        if (!actor.getRole().isCoordinator() && !courseFaculty.existsByCourseIdAndFacultyId(session.getCourse().getId(), actor.getId())) {
            throw new BusinessException("You are not assigned to this course");
        }
        assignments.deleteBySessionIdAndTeachingAssistantId(sessionId, taId);
    }

    private SessionSupportResponse toResponse(SessionSupportAssignment item) {
        return new SessionSupportResponse(item.getId(), item.getTeachingAssistant().getId(), item.getTeachingAssistant().getFullName(), item.getTeachingAssistant().getEmail(), item.getAssignedBy().getFullName());
    }

    private SupportStaffResponse toStaff(UserAccount user) { return new SupportStaffResponse(user.getId(), user.getFullName(), user.getEmail()); }
}
