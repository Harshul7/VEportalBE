package org.example.veportal.controller;

import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.response.TaskSearchResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TasksController {
    private final TaskService taskService;
    private final AuthenticatedUserProvider users;

    public TasksController(TaskService taskService, AuthenticatedUserProvider users) {
        this.taskService = taskService;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskSearchResponse>>> search(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(
                taskService.search(q, users.currentUser()), "Tasks retrieved"));
    }
}
