package org.example.veportal.repository;

import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByCourseIdOrderByDisplayOrderAscChapterNumberAsc(Long courseId);

    Optional<Chapter> findByCourseIdAndChapterNumber(Long courseId, Integer chapterNumber);

    long countByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);
}