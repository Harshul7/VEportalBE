package org.example.veportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_transfer_history")
public class GroupTransferHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id", nullable = false) private Course course;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "previous_group_id") private CourseGroup previousGroup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "new_group_id", nullable = false) private CourseGroup newGroup;
    @Column(name = "transferred_at", nullable = false) private LocalDateTime transferredAt;
    @Column(name = "reason", nullable = false, length = 500) private String reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "performed_by", nullable = false) private UserAccount performedBy;
    public Student getStudent() { return student; } public void setStudent(Student v) { student = v; }
    public Course getCourse() { return course; } public void setCourse(Course v) { course = v; }
    public CourseGroup getPreviousGroup() { return previousGroup; } public void setPreviousGroup(CourseGroup v) { previousGroup = v; }
    public CourseGroup getNewGroup() { return newGroup; } public void setNewGroup(CourseGroup v) { newGroup = v; }
    public LocalDateTime getTransferredAt() { return transferredAt; } public void setTransferredAt(LocalDateTime v) { transferredAt = v; }
    public String getReason() { return reason; } public void setReason(String v) { reason = v; }
    public UserAccount getPerformedBy() { return performedBy; } public void setPerformedBy(UserAccount v) { performedBy = v; }
}
