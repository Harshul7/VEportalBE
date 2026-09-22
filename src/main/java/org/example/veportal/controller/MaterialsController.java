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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;

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
            @RequestParam(name = "chapterId", required = false) String chapterId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PagedResult<MaterialResponse> result = materialService.list(search, type, sessionId, chapterId, page, size,
                authenticatedUserProvider.currentUser());
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

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<MaterialResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) String chapterId,
            @RequestParam(required = false) String description) {
        MaterialResponse response = materialService.createUpload(file,
                new MaterialCreateRequest(title, type, sessionId, topicId, chapterId, description, file.getOriginalFilename(), null),
                authenticatedUserProvider.currentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Material uploaded successfully"));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@resourceAuthorization.canAccessMaterial(#id)")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var material = materialService.requireStored(id);
        Resource resource = new FileSystemResource(materialService.storedPath(material));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        (material.getFileName() == null ? "material-" + id : material.getFileName()).replace("\"", "") + "\"")
                .header(HttpHeaders.CONTENT_TYPE, material.getContentType() == null ? "application/octet-stream" : material.getContentType())
                .body(resource);
    }
}
