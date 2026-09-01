package org.example.veportal.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.veportal.dto.request.ParticipationSaveRequest;
import org.example.veportal.dto.response.ParticipationRecordResponse;
import org.example.veportal.dto.response.ParticipationRosterRowResponse;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.ParticipationLevel;
import org.example.veportal.entity.ParticipationRecord;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.mapper.AcademicMapper;
import org.example.veportal.repository.ParticipationRecordRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.util.Labels;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipationService {

    private final ParticipationRecordRepository participationRepository;
    private final StudentRepository studentRepository;
    private final SessionService sessionService;
    private final AcademicMapper academicMapper;
    private final ActivityService activityService;

    public ParticipationService(ParticipationRecordRepository participationRepository,
                                StudentRepository studentRepository,
                                SessionService sessionService,
                                AcademicMapper academicMapper,
                                ActivityService activityService) {
        this.participationRepository = participationRepository;
        this.studentRepository = studentRepository;
        this.sessionService = sessionService;
        this.academicMapper = academicMapper;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<ParticipationRosterRowResponse> roster(Long sessionId, String search, String levelFilter) {
        ClassSession session = sessionService.requireSession(sessionId);
        Map<Long, ParticipationRecord> byStudentId = new HashMap<>();
        for (ParticipationRecord record : participationRepository.findWithStudentBySessionId(session.getId())) {
            byStudentId.put(record.getStudent().getId(), record);
        }
        List<Student> students = studentRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), org.example.veportal.entity.AccountStatus.ACTIVE),
                Sort.by(Sort.Direction.ASC, "fullName"));

        String term = search == null ? "" : search.trim().toLowerCase();
        return students.stream()
                .filter(student -> term.isEmpty()
                        || student.getFullName().toLowerCase().contains(term)
                        || student.getStudentCode().toLowerCase().contains(term))
                .map(student -> {
                    ParticipationRecord record = byStudentId.get(student.getId());
                    String level = record == null ? "Not Recorded" : Labels.of(record.getLevel());
                    String notes = record == null || record.getNotes() == null ? "" : record.getNotes();
                    if (levelFilter != null && !levelFilter.isBlank() && !"All".equalsIgnoreCase(levelFilter)
                            && !levelFilter.equals(level)) {
                        return null;
                    }
                    return new ParticipationRosterRowResponse(
                            student.getStudentCode(),
                            student.getFullName(),
                            student.getProgramme(),
                            student.getBatch(),
                            level,
                            notes);
                })
                .filter(row -> row != null)
                .toList();
    }

    @Transactional
    public List<ParticipationRecordResponse> save(Long sessionId, ParticipationSaveRequest request,
                                                  UserAccount currentUser) {
        ClassSession session = sessionService.requireSession(sessionId);
        Map<Long, ParticipationRecord> existingByStudent = new HashMap<>();
        for (ParticipationRecord record : participationRepository.findWithStudentBySessionId(session.getId())) {
            existingByStudent.put(record.getStudent().getId(), record);
        }
        for (var entry : request.entries()) {
            Student student = studentRepository.findByStudentCodeIgnoreCase(entry.studentId().trim())
                    .orElseThrow(() -> NotFoundException.resource("Student", entry.studentId()));
            ParticipationLevel level = Labels.toParticipationLevel(entry.level());
            ParticipationRecord record = existingByStudent.get(student.getId());
            if (level == ParticipationLevel.NOT_RECORDED) {
                if (record != null) {
                    participationRepository.delete(record);
                    existingByStudent.remove(student.getId());
                }
                continue;
            }
            if (record == null) {
                record = new ParticipationRecord();
                record.setSession(session);
                record.setStudent(student);
                existingByStudent.put(student.getId(), record);
            }
            record.setLevel(level);
            record.setNotes(entry.notes());
            participationRepository.save(record);
        }

        activityService.record(currentUser,
                org.example.veportal.entity.ActivityKind.PARTICIPATION,
                "Participation updated for Session " + session.getSessionNumber(),
                truncate(session.getTopic(), 255),
                "/participation?session=" + session.getId());

        List<ParticipationRecord> allRecords = participationRepository.findWithStudentBySessionId(session.getId());
        return allRecords.stream().map(academicMapper::toResponse).toList();
    }

    static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }
}
