package org.example.veportal.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="group_topic_plan_versions")
public class GroupTopicPlanVersion extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="group_id",nullable=false) private CourseGroup group;
 @Column(name="version_number",nullable=false) private Integer versionNumber;
 @Column(name="snapshot",nullable=false,length=10000) private String snapshot;
 @Column(name="reason",nullable=false,length=500) private String reason;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="author_id",nullable=false) private UserAccount author;
 @Column(name="created_at_override",nullable=false) private LocalDateTime createdAtOverride;
 public CourseGroup getGroup(){return group;} public void setGroup(CourseGroup v){group=v;} public Integer getVersionNumber(){return versionNumber;} public void setVersionNumber(Integer v){versionNumber=v;} public String getSnapshot(){return snapshot;} public void setSnapshot(String v){snapshot=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public UserAccount getAuthor(){return author;} public void setAuthor(UserAccount v){author=v;} public LocalDateTime getCreatedAtOverride(){return createdAtOverride;} public void setCreatedAtOverride(LocalDateTime v){createdAtOverride=v;}
}
