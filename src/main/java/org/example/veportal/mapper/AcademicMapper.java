package org.example.veportal.mapper;

import org.example.veportal.dto.response.AttendanceRecordResponse;
import org.example.veportal.dto.response.MaterialResponse;
import org.example.veportal.dto.response.ParticipationRecordResponse;
import org.example.veportal.dto.response.RosterRowResponse;
import org.example.veportal.dto.response.TeachingLogResponse;
import org.example.veportal.entity.AttendanceRecord;
import org.example.veportal.entity.CourseMaterial;
import org.example.veportal.entity.ParticipationRecord;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.TeachingLog;
import org.example.veportal.util.Labels;
import org.example.veportal.util.SizeFormat;
import org.springframework.stereotype.Component;

@Component
public class AcademicMapper {

    public AttendanceRecordResponse toResponse(AttendanceRecord record) {
        return new AttendanceRecordResponse(
                record.getId().toString(),
                record.getSession().getId().toString(),
                record.getStudent().getStudentCode(),
                Labels.of(record.getStatus())
        );
    }

    public ParticipationRecordResponse toResponse(ParticipationRecord record) {
        return new ParticipationRecordResponse(
                record.getId().toString(),
                record.getSession().getId().toString(),
                record.getStudent().getStudentCode(),
                Labels.of(record.getLevel()),
                record.getNotes() == null ? "" : record.getNotes()
        );
    }

    public RosterRowResponse toRosterRow(Student student, String attendanceStatus) {
        return new RosterRowResponse(
                student.getStudentCode(),
                student.getFullName(),
                student.getProgramme(),
                student.getBatch(),
                attendanceStatus
        );
    }

    public MaterialResponse toResponse(CourseMaterial material) {
        return new MaterialResponse(
                material.getId().toString(),
                material.getTitle(),
                Labels.of(material.getType()),
                material.getSession() == null ? null : material.getSession().getId().toString(),
                material.getUploadedBy().getFullName(),
                material.getUploadedAt().toString(),
                SizeFormat.format(material.getSizeBytes()),
                material.getDescription()
        );
    }

    public TeachingLogResponse toResponse(TeachingLog log) {
        return new TeachingLogResponse(
                log.getId().toString(),
                log.getSession().getId().toString(),
                log.getContent(),
                log.getConcepts().stream().map(c -> c.getLabel()).toList(),
                log.getRemarks() == null ? "" : log.getRemarks(),
                log.getUpdatedAt() == null ? null : log.getUpdatedAt().toString()
        );
    }
}
