package org.example.veportal.security;

import java.util.List;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public AppUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new UsernameNotFoundException("User is inactive: " + email);
        }
        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())))
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}
