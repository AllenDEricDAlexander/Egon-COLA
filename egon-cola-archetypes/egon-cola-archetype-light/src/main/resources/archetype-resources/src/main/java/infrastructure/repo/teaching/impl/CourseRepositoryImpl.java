package ${package}.infrastructure.repo.teaching.impl;

import ${package}.domain.teaching.model.Course;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.infrastructure.repo.teaching.converter.CoursePoConverter;
import ${package}.infrastructure.repo.teaching.jpa.CourseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("courseRepositoryImpl")
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {
    @Qualifier("courseJpaRepository")
    private final CourseJpaRepository courseJpaRepository;

    @Qualifier("coursePoConverter")
    private final CoursePoConverter coursePoConverter;

    @Override
    public Course save(Course course) {
        return coursePoConverter.toDomain(courseJpaRepository.save(coursePoConverter.toPo(course)));
    }

    @Override
    public Optional<Course> findLegacyById(String courseId) {
        return courseJpaRepository.findById(courseId).map(coursePoConverter::toDomain);
    }
}
