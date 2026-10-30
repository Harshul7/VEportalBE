package org.example.veportal.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "session_support_assignments", uniqueConstraints = @UniqueConstraint(name = "uq_session_support_ta", columnNames = {"session_id", "ta_id"}))
public class SessionSupportAssignment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ta_id", nullable = false)
    private UserAccount teachingAssistant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private UserAccount assignedBy;

    public ClassSession getSession() { return session; }
    public void setSession(ClassSession session) { this.session = session; }
    public UserAccount getTeachingAssistant() { return teachingAssistant; }
    public void setTeachingAssistant(UserAccount teachingAssistant) { this.teachingAssistant = teachingAssistant; }
    public UserAccount getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UserAccount assignedBy) { this.assignedBy = assignedBy; }
}
