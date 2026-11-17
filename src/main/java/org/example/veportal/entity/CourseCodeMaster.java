package org.example.veportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "course_code_master", uniqueConstraints = @UniqueConstraint(name = "uq_course_code_master_code", columnNames = "code"))
public class CourseCodeMaster extends BaseEntity {
    @Column(nullable = false, length = 40)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
