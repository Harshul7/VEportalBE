package org.example.veportal.repository;

import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.TestDataStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TestDataStudentRepository
        extends JpaRepository<TestDataStudent, Long>, JpaSpecificationExecutor<TestDataStudent> {

    boolean existsByStudentCodeIgnoreCase(String studentCode);

    Optional<TestDataStudent> findByStudentCodeIgnoreCase(String studentCode);

    @Query("select distinct t.programme from TestDataStudent t order by t.programme")
    List<String> findDistinctProgrammes();

    @Query("select distinct t.batch from TestDataStudent t order by t.batch desc")
    List<String> findDistinctBatches();
}
