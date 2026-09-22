package org.example.veportal.service;

import org.example.veportal.dto.request.LoginRequest;
import org.example.veportal.dto.response.AuthResponse;
import org.example.veportal.dto.response.UserResponse;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.entity.PasswordResetToken;
import org.example.veportal.repository.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.mapper.UserMapper;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.security.JwtService;
import org.example.veportal.security.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       MailService mailService,
                       LoginAttemptService loginAttemptService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailService = mailService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String loginKey = request.email() == null ? "" : request.email().trim();
        if (loginAttemptService.isBlocked(loginKey)) {
            throw new BadCredentialsException("Invalid email or password");
        }
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> {
                    loginAttemptService.failed(loginKey);
                    return new BadCredentialsException("Invalid email or password");
                });
        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Login attempt for inactive account: {}", account.getEmail());
            throw new DisabledException("Account is inactive");
        }
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            loginAttemptService.failed(loginKey);
            log.warn("Failed login attempt for account: {}", account.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
        loginAttemptService.succeeded(loginKey);
        String token = jwtService.generate(account);
        UserResponse user = userMapper.toResponse(account);
        return new AuthResponse(token, user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUserProfile(String email) {
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> NotFoundException.resource("User", email));
        return userMapper.toResponse(account);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setMustChangePassword(false);
        userAccountRepository.save(account);
        log.info("Password changed for account: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken reset = passwordResetTokenRepository.findByTokenHash(hashToken(token))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadCredentialsException("Invalid reset token"));
        UserAccount account = reset.getUser();
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setMustChangePassword(false);
        userAccountRepository.save(account);
        reset.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(reset);
        log.info("Password reset for account: {}", account.getEmail());
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userAccountRepository.findByEmailIgnoreCase(email.trim()).ifPresent(account -> {
            issuePasswordSetup(account);
        });
    }

    @Transactional
    public void issuePasswordSetup(UserAccount account) {
        passwordResetTokenRepository.deleteByUserId(account.getId());
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUser(account);
        reset.setTokenHash(hashToken(raw));
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(reset);
        mailService.sendPasswordReset(account.getEmail(), account.getFullName(), raw);
    }

    private String hashToken(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash reset token", e);
        }
    }
}
