package org.example.veportal.controller;

import java.security.SecureRandom;
import java.util.List;
import org.example.veportal.config.AppProperties;
import org.example.veportal.dto.ApiResponse;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/credentials")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCredentialsController {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AppProperties appProperties;

    public AdminCredentialsController(UserAccountRepository userAccountRepository,
                                      PasswordEncoder passwordEncoder,
                                      AuthService authService,
                                      AppProperties appProperties) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.appProperties = appProperties;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CredentialUser>>> listCredentials() {
        List<CredentialUser> users = userAccountRepository.findAll().stream()
                .sorted((left, right) -> left.getFullName().compareToIgnoreCase(right.getFullName()))
                .map(this::toCredentialUser)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(users, "Credentials users retrieved"));
    }

    @PostMapping("/{id}/temporary-password")
    public ResponseEntity<ApiResponse<TemporaryPasswordResponse>> issueTemporaryPassword(@PathVariable Long id) {
        if (!appProperties.security().allowTemporaryPasswordDisplay()) {
            return ResponseEntity.status(403).body(ApiResponse.error(
                    "Temporary password display is disabled. Use the password setup email workflow."));
        }
        UserAccount user = userAccountRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setStatus(AccountStatus.ACTIVE);
        UserAccount saved = userAccountRepository.save(user);
        authService.issuePasswordSetup(saved);
        return ResponseEntity.ok(ApiResponse.success(
                new TemporaryPasswordResponse(saved.getEmail(), temporaryPassword),
                "Temporary password generated. It will not be shown again."));
    }

    private CredentialUser toCredentialUser(UserAccount user) {
        return new CredentialUser(user.getId(), user.getFullName(), user.getEmail(),
                user.getStaffCode(), user.getRole().name(), user.getStatus().name(),
                user.isMustChangePassword(), user.getCreatedAt() == null ? "" : user.getCreatedAt().toString());
    }

    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String symbols = "!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder result = new StringBuilder();
        result.append(upper.charAt(random.nextInt(upper.length())));
        result.append(lower.charAt(random.nextInt(lower.length())));
        result.append(digits.charAt(random.nextInt(digits.length())));
        result.append(symbols.charAt(random.nextInt(symbols.length())));
        String pool = upper + lower + digits + symbols;
        for (int i = 0; i < 6; i++) result.append(pool.charAt(random.nextInt(pool.length())));
        char[] chars = result.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char swap = chars[i];
            chars[i] = chars[j];
            chars[j] = swap;
        }
        return new String(chars);
    }

    public record CredentialUser(long id, String name, String email, String employeeId,
                                 String role, String status, boolean mustChangePassword,
                                 String createdAt) {}

    public record TemporaryPasswordResponse(String email, String temporaryPassword) {}
}
