package org.example.veportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "course_faculties")
@IdClass(CourseFaculty.CourseFacultyId.class)
public class CourseFaculty {

    @Id
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Id
    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", insertable = false, updatable = false)
    private UserAccount faculty;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    public CourseFaculty() {
    }

    public CourseFaculty(Long courseId, Long facultyId) {
        this.courseId = courseId;
        this.facultyId = facultyId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public UserAccount getFaculty() {
        return faculty;
    }

    public void setFaculty(UserAccount faculty) {
        this.faculty = faculty;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class CourseFacultyId implements Serializable {
        private Long courseId;
        private Long facultyId;

        public CourseFacultyId() {
        }

        public CourseFacultyId(Long courseId, Long facultyId) {
            this.courseId = courseId;
            this.facultyId = facultyId;
        }

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }

        public Long getFacultyId() {
            return facultyId;
        }

        public void setFacultyId(Long facultyId) {
            this.facultyId = facultyId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CourseFacultyId that)) {
                return false;
            }
            return Objects.equals(courseId, that.courseId) && Objects.equals(facultyId, that.facultyId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(courseId, facultyId);
        }
    }
}