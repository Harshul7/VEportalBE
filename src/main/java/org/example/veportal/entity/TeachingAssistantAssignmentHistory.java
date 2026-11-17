package org.example.veportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ta_group_assignment_history")
public class TeachingAssistantAssignmentHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false)
    private CourseGroup group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ta_user_id", nullable = false)
    private UserAccount teachingAssistant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assigned_by", nullable = false)
    private UserAccount assignedBy;
    @Column(name = "assigned_at", nullable = false) private LocalDateTime assignedAt;
    @Column(name = "ended_at") private LocalDateTime endedAt;
    @Column(name = "reason", length = 500) private String reason;
    @Column(name = "status", nullable = false, length = 20) private String status = "ACTIVE";
    public CourseGroup getGroup(){return group;} public void setGroup(CourseGroup v){group=v;}
    public UserAccount getTeachingAssistant(){return teachingAssistant;} public void setTeachingAssistant(UserAccount v){teachingAssistant=v;}
    public UserAccount getAssignedBy(){return assignedBy;} public void setAssignedBy(UserAccount v){assignedBy=v;}
    public LocalDateTime getAssignedAt(){return assignedAt;} public void setAssignedAt(LocalDateTime v){assignedAt=v;}
    public LocalDateTime getEndedAt(){return endedAt;} public void setEndedAt(LocalDateTime v){endedAt=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
