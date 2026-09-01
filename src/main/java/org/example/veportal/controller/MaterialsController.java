package org.example.veportal.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.PagedResult;
import org.example.veportal.dto.request.MaterialCreateRequest;
import org.example.veportal.dto.response.MaterialResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/materials")
public class MaterialsController {

    private final MaterialService materialService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public MaterialsController(MaterialService materialService,
                               AuthenticatedUserProvider authenticatedUserProvider) {
        this.materialService = materialService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "sessionId", required = false) String sessionId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PagedResult<MaterialResponse> result = materialService.list(search, type, sessionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.content(), "Materials retrieved",
                result.pagination()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialResponse>> create(
            @Valid @RequestBody MaterialCreateRequest request) {
        MaterialResponse response =
                materialService.create(request, authenticatedUserProvider.currentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Material uploaded successfully"));
    }
}
