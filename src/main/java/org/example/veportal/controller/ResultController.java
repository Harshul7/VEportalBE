package org.example.veportal.controller;

import java.time.LocalDateTime;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.entity.*;
import org.example.veportal.repository.*;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/results")
public class ResultController {
    private final StudentCourseResultRepository results; private final StudentRepository students; private final CourseRepository courses;
    private final CourseStudentRepository enrollments; private final CourseGroupRepository groups; private final AuthenticatedUserProvider users;
    public ResultController(StudentCourseResultRepository r, StudentRepository s, CourseRepository c, CourseStudentRepository e, CourseGroupRepository g, AuthenticatedUserProvider u){results=r;students=s;courses=c;enrollments=e;groups=g;users=u;}
    @PutMapping("/students/{studentId}/courses/{courseId}") @Transactional @PreAuthorize("hasAnyRole('FACULTY_COORDINATOR','ADMIN','GROUP_MENTOR','FACULTY')")
    public ResponseEntity<ApiResponse<ResultItem>> save(@PathVariable Long studentId,@PathVariable Long courseId,@RequestBody ResultRequest request){
        UserAccount actor=users.currentUser(); CourseStudent enrollment=enrollments.findByCourseId(courseId).stream().filter(v->v.getStudentId().equals(studentId)).findFirst().orElse(null);
        Course course=courses.findById(courseId).orElse(null); Student student=students.findById(studentId).orElse(null);
        if(enrollment==null||course==null||student==null)return ResponseEntity.ok(ApiResponse.error("Student is not enrolled in this course offering"));
        CourseGroup group=enrollment.getGroupId()==null?null:groups.findById(enrollment.getGroupId()).orElse(null);
        if(!actor.getRole().isCoordinator() && (group==null||group.getMentor()==null||!group.getMentor().getId().equals(actor.getId()))) return ResponseEntity.status(403).body(ApiResponse.error("You can only evaluate students in your assigned group"));
        StudentCourseResult result=results.findByStudentIdAndCourseId(studentId,courseId).orElseGet(StudentCourseResult::new);
        if(result.getEvaluationStatus()==StudentCourseResult.EvaluationStatus.FINALIZED && actor.getRole()!=Role.FACULTY_COORDINATOR && actor.getRole()!=Role.ADMIN)return ResponseEntity.status(403).body(ApiResponse.error("Published results require coordinator correction workflow"));
        result.setStudent(student);result.setCourse(course);result.setGroup(group);result.setEvaluationStatus(StudentCourseResult.EvaluationStatus.valueOf(request.evaluationStatus()));
        result.setFinalResult(request.finalResult()==null?null:StudentCourseResult.FinalResult.valueOf(request.finalResult()));result.setRemarks(request.remarks());result.setEvaluator(actor);
        if(result.getEvaluationStatus()==StudentCourseResult.EvaluationStatus.FINALIZED){result.setPublisher(actor);result.setPublishedAt(LocalDateTime.now());}
        return ResponseEntity.ok(ApiResponse.success(toItem(results.save(result)),"Result saved"));
    }
    @GetMapping("/students/{studentId}/courses/{courseId}") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResultItem>> get(@PathVariable Long studentId,@PathVariable Long courseId){return results.findByStudentIdAndCourseId(studentId,courseId).map(v->ResponseEntity.ok(ApiResponse.success(toItem(v),"Result retrieved"))).orElseGet(()->ResponseEntity.ok(ApiResponse.error("Result not found")));}
    private ResultItem toItem(StudentCourseResult r){return new ResultItem(r.getId(),r.getStudent().getId(),r.getCourse().getId(),r.getEvaluationStatus().name(),r.getFinalResult()==null?null:r.getFinalResult().name(),r.getPublishedAt()==null?null:r.getPublishedAt().toString(),r.getEvaluator()==null?null:r.getEvaluator().getFullName(),r.getPublisher()==null?null:r.getPublisher().getFullName(),r.getRemarks());}
    public record ResultRequest(String evaluationStatus,String finalResult,String remarks){}
    public record ResultItem(long id,long studentId,long courseId,String evaluationStatus,String finalResult,String publishedAt,String evaluator,String publisher,String remarks){}
}
