package org.example.veportal.controller;

import org.example.veportal.dto.ApiResponse;
import org.example.veportal.dto.response.BootstrapResponse;
import org.example.veportal.security.AuthenticatedUserProvider;
import org.example.veportal.service.PortalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final PortalService portalService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public PortalController(PortalService portalService,
                            AuthenticatedUserProvider authenticatedUserProvider) {
        this.portalService = portalService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<ApiResponse<BootstrapResponse>> bootstrap() {
        BootstrapResponse response = portalService.bootstrap(authenticatedUserProvider.currentUser());
        return ResponseEntity.ok(ApiResponse.success(response, "Portal data loaded"));
    }
}
