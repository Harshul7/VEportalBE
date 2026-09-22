package org.example.veportal.controller;

import java.util.List;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.response.SearchResultResponse;
import org.example.veportal.service.GlobalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class GlobalSearchController {
    private final GlobalSearchService searchService;

    public GlobalSearchController(GlobalSearchService searchService) { this.searchService = searchService; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SearchResultResponse>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success(searchService.search(q, limit), "Search results retrieved"));
    }
}
