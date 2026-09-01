package org.example.veportal.mapper;

import org.example.veportal.dto.response.StudentResponse;
import org.example.veportal.entity.Student;
import org.example.veportal.util.Labels;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentResponse toResponse(Student student, Double attendancePercent) {
        return new StudentResponse(
                student.getStudentCode(),
                student.getFullName(),
                student.getProgramme(),
                student.getBatch(),
                Labels.of(student.getStatus()),
                student.getEmail(),
                student.getCreatedAt() == null ? null : student.getCreatedAt().toLocalDate().toString(),
                attendancePercent
        );
    }
}
