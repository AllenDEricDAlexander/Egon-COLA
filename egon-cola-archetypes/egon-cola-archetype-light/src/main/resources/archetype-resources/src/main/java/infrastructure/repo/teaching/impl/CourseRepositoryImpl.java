package ${package}.infrastructure.repo.teaching.impl;

import ${package}.domain.teaching.model.Course;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.infrastructure.repo.teaching.converter.CoursePoConverter;
import ${package}.infrastructure.repo.teaching.jpa.CourseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CourseRepositoryImpl implements CourseRepository {
    private final CourseJpaRepository courseJpaRepository;

    public CourseRepositoryImpl(CourseJpaRepository courseJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
    }

    @Override
    public Course save(Course course) {
        return CoursePoConverter.toDomain(courseJpaRepository.save(CoursePoConverter.toPo(course)));
    }

    @Override
    public Optional<Course> findById(String courseId) {
        return courseJpaRepository.findById(courseId).map(CoursePoConverter::toDomain);
    }
}
