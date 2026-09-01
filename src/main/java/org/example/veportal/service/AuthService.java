package org.example.veportal.service;

import org.example.veportal.dto.request.LoginRequest;
import org.example.veportal.dto.response.AuthResponse;
import org.example.veportal.dto.response.UserResponse;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.mapper.UserMapper;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.security.JwtService;
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

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Login attempt for inactive account: {}", account.getEmail());
            throw new DisabledException("Account is inactive");
        }
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            log.warn("Failed login attempt for account: {}", account.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
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
}
