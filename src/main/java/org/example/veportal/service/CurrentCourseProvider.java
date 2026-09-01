package org.example.veportal.service;

import org.example.veportal.entity.Course;
import org.example.veportal.exception.NotFoundException;
import org.example.veportal.repository.CourseRepository;
import org.springframework.stereotype.Component;

@Component
public class CurrentCourseProvider {

    private final CourseRepository courseRepository;

    public CurrentCourseProvider(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public Course requireCurrentCourse() {
        return courseRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No course has been configured yet"));
    }
}
