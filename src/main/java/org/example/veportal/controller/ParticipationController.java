package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.request.ParticipationSaveRequest;
import org.example.veportal.dto.response.ParticipationRecordResponse;
import org.example.veportal.dto.response.ParticipationRosterRowResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.ParticipationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/participation")
public class ParticipationController {

    private final ParticipationService participationService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ParticipationController(ParticipationService participationService,
                                   AuthenticatedUserProvider authenticatedUserProvider) {
        this.participationService = participationService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParticipationRosterRowResponse>>> roster(
            @PathVariable Long sessionId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "level", required = false) String level) {
        List<ParticipationRosterRowResponse> roster =
                participationService.roster(sessionId, search, level);
        return ResponseEntity.ok(ApiResponse.success(roster, "Participation roster retrieved"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<ParticipationRecordResponse>>> save(
            @PathVariable Long sessionId,
            @Valid @RequestBody ParticipationSaveRequest request) {
        List<ParticipationRecordResponse> saved =
                participationService.save(sessionId, request, authenticatedUserProvider.currentUser());
        return ResponseEntity.ok(ApiResponse.success(saved, "Participation updated"));
    }
}
