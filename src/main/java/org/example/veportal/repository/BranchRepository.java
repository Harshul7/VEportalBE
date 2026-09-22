package org.example.veportal.repository;

import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByActiveTrueOrderByCodeAsc();
    Optional<Branch> findByCodeIgnoreCase(String code);
}
