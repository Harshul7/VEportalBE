package org.example.veportal.repository;

import java.util.List;
import org.example.veportal.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    boolean existsByStudentCodeIgnoreCase(String studentCode);

    java.util.Optional<Student> findByStudentCodeIgnoreCase(String studentCode);

    List<Student> findAllByOrderByIdAsc();

    long countByStatus(org.example.veportal.entity.AccountStatus status);

    @Query("select distinct s.programme from Student s order by s.programme")
    List<String> findDistinctProgrammes();

    @Query("select distinct s.batch from Student s order by s.batch desc")
    List<String> findDistinctBatches();
}
