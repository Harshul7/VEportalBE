package org.example.veportal.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.PageMeta;
import org.example.veportal.dto.request.TestDataStudentRequest;
import org.example.veportal.dto.response.TestDataStudentFiltersResponse;
import org.example.veportal.dto.response.TestDataStudentResponse;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.TestDataStudent;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.exception.ConflictException;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.TestDataStudentRepository;
import org.example.veportal.util.Labels;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestDataStudentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final TestDataStudentRepository repository;

    public TestDataStudentService(TestDataStudentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PagedResult<TestDataStudentResponse> list(String search, String programme, String batch,
                                                     String status, int page, int size) {
        Specification<TestDataStudent> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("studentCode")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)));
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

        Page<TestDataStudent> result = repository.findAll(spec,
                PageRequest.of(Math.max(page, 0), normalizeSize(size), Sort.by(Sort.Direction.ASC, "id")));

        PageMeta meta = new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
        Page<TestDataStudentResponse> mapped = result.map(this::toResponse);
        return new PagedResult<>(mapped.getContent(), meta);
    }

    @Transactional(readOnly = true)
    public TestDataStudentFiltersResponse filters() {
        return new TestDataStudentFiltersResponse(
                repository.findDistinctProgrammes(),
                repository.findDistinctBatches());
    }

    @Transactional(readOnly = true)
    public TestDataStudentResponse get(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public TestDataStudentResponse create(TestDataStudentRequest request) {
        String code = request.studentCode().trim();
        if (repository.existsByStudentCodeIgnoreCase(code)) {
            throw new ConflictException("A test student with code " + code + " already exists");
        }
        TestDataStudent entity = new TestDataStudent();
        entity.setStudentCode(code);
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public TestDataStudentResponse update(Long id, TestDataStudentRequest request) {
        TestDataStudent entity = require(id);
        String code = request.studentCode().trim();
        repository.findByStudentCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("A test student with code " + code + " already exists");
                });
        entity.setStudentCode(code);
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        TestDataStudent entity = require(id);
        repository.delete(entity);
    }

    private void apply(TestDataStudent entity, TestDataStudentRequest request) {
        entity.setFullName(request.fullName().trim());
        entity.setEmail(request.email() == null || request.email().isBlank()
                ? null
                : request.email().trim());
        entity.setProgramme(request.programme().trim());
        entity.setBatch(request.batch() == null || request.batch().isBlank()
                ? null
                : request.batch().trim());
        entity.setGender(request.gender() == null || request.gender().isBlank()
                ? null
                : request.gender().trim());
        entity.setDateOfBirth(parseDate(request.dateOfBirth()));
        entity.setStatus(Labels.toAccountStatus(request.status()));
        entity.setMobile(request.mobile() == null || request.mobile().isBlank()
                ? null
                : request.mobile().trim());
        entity.setAddress(request.address() == null || request.address().isBlank()
                ? null
                : request.address().trim());
        entity.setCity(request.city() == null || request.city().isBlank()
                ? null
                : request.city().trim());
        entity.setState(request.state() == null || request.state().isBlank()
                ? null
                : request.state().trim());
        entity.setPincode(request.pincode() == null || request.pincode().isBlank()
                ? null
                : request.pincode().trim());
        entity.setFatherName(request.fatherName() == null || request.fatherName().isBlank()
                ? null
                : request.fatherName().trim());
        entity.setMotherName(request.motherName() == null || request.motherName().isBlank()
                ? null
                : request.motherName().trim());
        entity.setAdmissionDate(parseDate(request.admissionDate()));
        entity.setScholarship(request.scholarship() != null && request.scholarship());
        entity.setNotes(request.notes() == null || request.notes().isBlank()
                ? null
                : request.notes().trim());
        entity.setEmergencyContact(request.emergencyContact() == null || request.emergencyContact().isBlank()
                ? null
                : request.emergencyContact().trim());
        entity.setBloodGroup(request.bloodGroup() == null || request.bloodGroup().isBlank()
                ? null
                : request.bloodGroup().trim());
    }

    private TestDataStudentResponse toResponse(TestDataStudent entity) {
        return new TestDataStudentResponse(
                entity.getId(),
                entity.getStudentCode(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getProgramme(),
                entity.getBatch(),
                entity.getGender(),
                entity.getDateOfBirth() == null ? null : entity.getDateOfBirth().toString(),
                Labels.of(entity.getStatus()),
                entity.getMobile(),
                entity.getAddress(),
                entity.getCity(),
                entity.getState(),
                entity.getPincode(),
                entity.getFatherName(),
                entity.getMotherName(),
                entity.getAdmissionDate() == null ? null : entity.getAdmissionDate().toString(),
                entity.getScholarship(),
                entity.getNotes(),
                entity.getEmergencyContact(),
                entity.getBloodGroup(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toLocalDate().toString()
        );
    }

    private TestDataStudent require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> NotFoundException.resource("Test data student", id));
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Date must be in yyyy-MM-dd format");
        }
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 12;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
