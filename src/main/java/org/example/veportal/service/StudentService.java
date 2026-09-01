package org.example.veportal.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.PageMeta;
import org.example.veportal.dto.request.StudentCreateRequest;
import org.example.veportal.dto.request.StudentUpdateRequest;
import org.example.veportal.dto.response.StudentFiltersResponse;
import org.example.veportal.dto.response.StudentResponse;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.Student;
import org.example.veportal.exception.ConflictException;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.mapper.StudentMapper;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.util.Labels;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final StudentMapper studentMapper;

    public StudentService(StudentRepository studentRepository,
                          AttendanceRecordRepository attendanceRepository,
                          StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentMapper = studentMapper;
    }

    @Transactional(readOnly = true)
    public PagedResult<StudentResponse> list(String search, String programme, String batch, String status,
                                             int page, int size) {
        Specification<Student> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("studentCode")), pattern)));
        }
        if (programme != null && !programme.isBlank() && !"All".equalsIgnoreCase(programme)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("programme"), programme));
        }
        if (batch != null && !batch.isBlank() && !"All".equalsIgnoreCase(batch)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("batch"), batch));
        }
        if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status)) {
            AccountStatus statusEnum = Labels.toAccountStatus(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusEnum));
        }

        Page<Student> result = studentRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), normalizeSize(size), Sort.by(Sort.Direction.ASC, "id")));

        List<Long> ids = result.map(Student::getId).getContent();
        Map<Long, Double> percents = attendancePercentages(result.getContent());

        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
        List<StudentResponse> items = result.map(student ->
                studentMapper.toResponse(student, percents.get(student.getId()))).toList();
        return new PagedResult<>(items, meta);
    }

    @Transactional(readOnly = true)
    public StudentFiltersResponse filters() {
        return new StudentFiltersResponse(
                studentRepository.findDistinctProgrammes(),
                studentRepository.findDistinctBatches());
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id) {
        Student student = requireStudent(id);
        Map<Long, Double> percents = attendancePercentages(List.of(student));
        return studentMapper.toResponse(student, percents.get(student.getId()));
    }

    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        String code = request.studentCode().trim();
        if (studentRepository.existsByStudentCodeIgnoreCase(code)) {
            throw new ConflictException("A student with ID " + code + " already exists");
        }
        Student student = new Student();
        student.setStudentCode(code);
        applyCommonFields(student, request.name(), request.programme(), request.batch(), request.status());
        student.setEmail(request.email() == null || request.email().isBlank()
                ? deriveEmail(request.name())
                : request.email().trim());
        Student saved = studentRepository.save(student);
        return studentMapper.toResponse(saved, null);
    }

    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student student = requireStudent(id);
        String code = request.studentCode().trim();
        studentRepository.findByStudentCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("A student with ID " + code + " already exists");
                });
        student.setStudentCode(code);
        applyCommonFields(student, request.name(), request.programme(), request.batch(), request.status());
        Student saved = studentRepository.save(student);
        Map<Long, Double> percents = attendancePercentages(List.of(saved));
        return studentMapper.toResponse(saved, percents.get(saved.getId()));
    }

    private void applyCommonFields(Student student, String name, String programme, String batch, String statusLabel) {
        student.setFullName(name.trim());
        student.setProgramme(programme.trim());
        student.setBatch(batch.trim());
        student.setStatus(Labels.toAccountStatus(statusLabel));
    }

    private Student requireStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> NotFoundException.resource("Student", id));
    }

    private static String deriveEmail(String name) {
        String base = name.trim().toLowerCase().replaceAll("\\s+", ".");
        return base + "@students.iiit.ac.in";
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 12;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Map<Long, Double> attendancePercentages(List<Student> students) {
        if (students.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = students.stream().map(Student::getId).toList();
        Map<Long, Double> result = new HashMap<>();
        for (AttendanceRecordRepository.StudentAttendanceAggregation agg :
                attendanceRepository.aggregateByStudents(ids)) {
            Double percent = org.example.veportal.util.Percent.of(agg.getPresent(), agg.getTotal());
            if (percent != null) {
                result.put(agg.getStudentId(), percent);
            }
        }
        return result;
    }
}
