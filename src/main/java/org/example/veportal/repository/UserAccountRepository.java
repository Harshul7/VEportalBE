package org.example.veportal.repository;

import java.util.Optional;
import java.util.List;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByStaffCodeIgnoreCase(String staffCode);

    Optional<UserAccount> findByStaffCodeIgnoreCase(String staffCode);

    List<UserAccount> findAllByRoleOrderByFullNameAsc(Role role);
}
