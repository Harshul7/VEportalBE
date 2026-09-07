package org.example.veportal.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.veportal.dto.request.AttendanceSaveRequest;
import org.example.veportal.dto.response.AttendanceRecordResponse;
import org.example.veportal.dto.response.RosterRowResponse;
import org.example.veportal.entity.AttendanceRecord;
import org.example.veportal.entity.AttendanceStatus;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.mapper.AcademicMapper;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.util.Labels;
import org.example.veportal.util.Percent;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final SessionService sessionService;
    private final AcademicMapper academicMapper;
    private final ActivityService activityService;
    private final org.example.veportal.repository.CourseStudentRepository courseStudentRepository;

    public AttendanceService(AttendanceRecordRepository attendanceRepository,
                             StudentRepository studentRepository,
                             SessionService sessionService,
                             AcademicMapper academicMapper,
                             ActivityService activityService,
                             org.example.veportal.repository.CourseStudentRepository courseStudentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.sessionService = sessionService;
        this.academicMapper = academicMapper;
        this.activityService = activityService;
        this.courseStudentRepository = courseStudentRepository;
    }

    @Transactional(readOnly = true)
    public List<RosterRowResponse> roster(Long sessionId, String search, String statusFilter, String sortBy) {
        ClassSession session = sessionService.requireSession(sessionId);
        Map<Long, String> statusByStudentId = new HashMap<>();
        for (AttendanceRecord record : attendanceRepository.findWithStudentBySessionId(session.getId())) {
            statusByStudentId.put(record.getStudent().getId(), Labels.of(record.getStatus()));
        }
        Sort sort = "id".equalsIgnoreCase(sortBy)
                ? Sort.by(Sort.Direction.ASC, "studentCode")
                : Sort.by(Sort.Direction.ASC, "fullName");
        List<Student> students = studentRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), org.example.veportal.entity.AccountStatus.ACTIVE),
                sort);

        List<Long> enrolledIds = courseStudentRepository.findByCourseId(session.getCourse().getId())
                .stream().map(org.example.veportal.entity.CourseStudent::getStudentId).toList();

        String term = search == null ? "" : search.trim().toLowerCase();
        return students.stream()
                .filter(student -> enrolledIds.isEmpty() || enrolledIds.contains(student.getId()))
                .filter(student -> term.isEmpty()
                        || student.getFullName().toLowerCase().contains(term)
                        || student.getStudentCode().toLowerCase().contains(term)
                        || student.getProgramme().toLowerCase().contains(term))
                .filter(student -> statusFilter == null || statusFilter.isBlank() || "All".equalsIgnoreCase(statusFilter)
                        || statusFilter.equals(statusByStudentId.get(student.getId())))
                .map(student -> academicMapper.toRosterRow(student, statusByStudentId.get(student.getId())))
                .toList();
    }

    @Transactional
    public List<AttendanceRecordResponse> save(Long sessionId, AttendanceSaveRequest request, UserAccount currentUser) {
        ClassSession session = sessionService.requireSession(sessionId);
        Map<Long, AttendanceRecord> existingByStudent = new HashMap<>();
        for (AttendanceRecord record : attendanceRepository.findWithStudentBySessionId(session.getId())) {
            existingByStudent.put(record.getStudent().getId(), record);
        }
        for (var entry : request.entries()) {
            Student student = studentRepository.findByStudentCodeIgnoreCase(entry.studentId().trim())
                    .orElseThrow(() -> NotFoundException.resource("Student", entry.studentId()));
            AttendanceStatus status = Labels.toAttendanceStatus(entry.status());
            AttendanceRecord record = existingByStudent.get(student.getId());
            if (record == null) {
                record = new AttendanceRecord();
                record.setSession(session);
                record.setStudent(student);
            }
            record.setStatus(status);
            record.setMarkedBy(currentUser);
            attendanceRepository.save(record);
        }

        List<AttendanceRecord> allRecords = attendanceRepository.findWithStudentBySessionId(session.getId());
        long present = allRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        Double percent = Percent.of(present, allRecords.size());

        activityService.record(currentUser,
                org.example.veportal.entity.ActivityKind.ATTENDANCE,
                "Attendance recorded for Session " + session.getSessionNumber(),
                truncate(session.getTopic() + " · " + (percent == null ? "—" : percent + "% present"), 255),
                "/attendance?session=" + session.getId());
        activityService.notifyUser(currentUser,
                "Session record updated",
                "Attendance for Session " + session.getSessionNumber() + " was saved.",
                "/attendance?session=" + session.getId());

        return allRecords.stream().map(academicMapper::toResponse).toList();
    }

    static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }
}
