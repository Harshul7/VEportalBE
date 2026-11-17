package org.example.veportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "course_groups", uniqueConstraints = @UniqueConstraint(name = "uq_course_groups_course_name", columnNames = {"course_id", "name"}))
public class CourseGroup extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private UserAccount mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ta_id")
    private UserAccount teachingAssistant;

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UserAccount getMentor() { return mentor; }
    public void setMentor(UserAccount mentor) { this.mentor = mentor; }
    public UserAccount getTeachingAssistant() { return teachingAssistant; }
    public void setTeachingAssistant(UserAccount teachingAssistant) { this.teachingAssistant = teachingAssistant; }
}
