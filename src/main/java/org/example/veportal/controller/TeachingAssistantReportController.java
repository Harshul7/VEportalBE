package org.example.veportal.controller;

import java.util.*;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.entity.*;
import org.example.veportal.repository.*;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/ta") @PreAuthorize("hasRole('TA')")
public class TeachingAssistantReportController {
    private final AuthenticatedUserProvider users; private final CourseGroupRepository groups; private final CourseStudentRepository students;
    private final ClassSessionRepository sessions; private final AttendanceRecordRepository attendance; private final SessionTaskRepository tasks;
    public TeachingAssistantReportController(AuthenticatedUserProvider u, CourseGroupRepository g, CourseStudentRepository s, ClassSessionRepository c, AttendanceRecordRepository a, SessionTaskRepository t){users=u;groups=g;students=s;sessions=c;attendance=a;tasks=t;}
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<TaReport>> report(@RequestParam(required=false) Long groupId){
        UserAccount user=users.currentUser(); List<CourseGroup> assigned=groups.findAll().stream().filter(g->g.getTeachingAssistant()!=null&&g.getTeachingAssistant().getId().equals(user.getId())).toList();
        if(groupId!=null) assigned=assigned.stream().filter(g->g.getId().equals(groupId)).toList();
        if(assigned.isEmpty()) return ResponseEntity.ok(ApiResponse.success(new TaReport(0,0,0,0,0,0,0,0,0,0),"TA report retrieved"));
        Set<Long> ids=assigned.stream().map(CourseGroup::getId).collect(java.util.stream.Collectors.toSet());
        List<ClassSession> sessionList=sessions.findAll().stream().filter(s->s.getGroup()!=null&&ids.contains(s.getGroup().getId())).toList();
        List<Long> sessionIds=sessionList.stream().map(ClassSession::getId).toList();
        long totalAttendance=sessionIds.isEmpty()?0:attendance.aggregateBySessions(sessionIds).stream().mapToLong(AttendanceRecordRepository.SessionAttendanceAggregation::getTotal).sum();
        long present=sessionIds.isEmpty()?0:attendance.aggregateBySessions(sessionIds).stream().mapToLong(AttendanceRecordRepository.SessionAttendanceAggregation::getPresent).sum();
        var groupTasks=tasks.findAll().stream().filter(t->t.getSession().getGroup()!=null&&ids.contains(t.getSession().getGroup().getId())).toList();
        long activeTasks=groupTasks.stream().filter(t->"ACTIVE".equalsIgnoreCase(t.getStatus())).count();
        long studentCount=assigned.stream().mapToLong(g->students.findByGroupId(g.getId()).size()).sum();
        long expectedAttendance=sessionList.stream().mapToLong(s->s.getGroup()==null?0:students.findByGroupId(s.getGroup().getId()).size()).sum();
        long pendingAttendance=Math.max(0, expectedAttendance-totalAttendance);
        long overdueTasks=groupTasks.stream().filter(t->t.getEndDate()!=null&&t.getEndDate().isBefore(java.time.LocalDate.now())&&!"COMPLETED".equalsIgnoreCase(t.getStatus())).count();
        double attendancePercentage=totalAttendance==0?0:(present*100.0/totalAttendance);
        return ResponseEntity.ok(ApiResponse.success(new TaReport(assigned.size(),studentCount,sessionList.size(),sessionList.stream().filter(s->s.getStatus()==SessionStatus.COMPLETED).count(),activeTasks,totalAttendance,present,pendingAttendance,overdueTasks,attendancePercentage),"TA report retrieved"));
    }
    public record TaReport(long groups,long students,long sessions,long completedSessions,long activeTasks,long attendanceEntries,long presentEntries,long pendingAttendance,long overdueTasks,double attendancePercentage){}
}
