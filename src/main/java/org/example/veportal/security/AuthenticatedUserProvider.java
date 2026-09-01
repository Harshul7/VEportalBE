package org.example.veportal.security;

import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    private final UserAccountRepository userAccountRepository;

    public AuthenticatedUserProvider(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userAccountRepository.findByEmailIgnoreCase(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        }
        throw new IllegalStateException("No authenticated user available");
    }
}
