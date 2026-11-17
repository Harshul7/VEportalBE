package org.example.veportal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "student_course_results", uniqueConstraints = @UniqueConstraint(name = "uq_student_course_result", columnNames = {"student_id", "course_id"}))
public class StudentCourseResult extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id", nullable = false) private Course course;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id") private CourseGroup group;
    @Enumerated(EnumType.STRING) @Column(name = "evaluation_status", nullable = false, length = 20) private EvaluationStatus evaluationStatus = EvaluationStatus.NOT_EVALUATED;
    @Enumerated(EnumType.STRING) @Column(name = "final_result", length = 10) private FinalResult finalResult;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "evaluator_id") private UserAccount evaluator;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "publisher_id") private UserAccount publisher;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "remarks", length = 1000) private String remarks;
    public Student getStudent(){return student;} public void setStudent(Student v){student=v;}
    public Course getCourse(){return course;} public void setCourse(Course v){course=v;}
    public CourseGroup getGroup(){return group;} public void setGroup(CourseGroup v){group=v;}
    public EvaluationStatus getEvaluationStatus(){return evaluationStatus;} public void setEvaluationStatus(EvaluationStatus v){evaluationStatus=v;}
    public FinalResult getFinalResult(){return finalResult;} public void setFinalResult(FinalResult v){finalResult=v;}
    public UserAccount getEvaluator(){return evaluator;} public void setEvaluator(UserAccount v){evaluator=v;}
    public UserAccount getPublisher(){return publisher;} public void setPublisher(UserAccount v){publisher=v;}
    public LocalDateTime getPublishedAt(){return publishedAt;} public void setPublishedAt(LocalDateTime v){publishedAt=v;}
    public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;}
    public enum EvaluationStatus { NOT_EVALUATED, IN_PROGRESS, FINALIZED }
    public enum FinalResult { PASS, FAIL }
}
