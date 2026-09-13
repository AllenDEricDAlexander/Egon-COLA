package top.egon.cola.archetype.source.light.domain.teaching.service;

import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;

import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Persistence-owning course domain service contract. */
public interface CourseDomainService {
    Course createCourse( @Valid @NotNull CourseCode code, String name);

    Course save( @Valid @NotNull Course course);

    Optional<Course> findById(Long courseId);

    Optional<Course> findByCode( @Valid @NotNull CourseCode courseCode);
}
