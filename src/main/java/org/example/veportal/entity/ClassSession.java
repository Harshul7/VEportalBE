package org.example.veportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(name = "class_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_class_sessions_course_number", columnNames = {"course_id", "session_number"})
})
public class ClassSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topicEntity;

    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "mode", length = 30)
    private String mode;

    @Column(name = "session_type", length = 40)
    private String sessionType;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false, length = 5)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 5)
    private String endTime;

    @Column(name = "room", length = 60)
    private String room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private UserAccount faculty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public Topic getTopicEntity() {
        return topicEntity;
    }

    public void setTopicEntity(Topic topicEntity) {
        this.topicEntity = topicEntity;
    }

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public String getTopic() {
        return topic;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public UserAccount getFaculty() {
        return faculty;
    }

    public void setFaculty(UserAccount faculty) {
        this.faculty = faculty;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }
}
