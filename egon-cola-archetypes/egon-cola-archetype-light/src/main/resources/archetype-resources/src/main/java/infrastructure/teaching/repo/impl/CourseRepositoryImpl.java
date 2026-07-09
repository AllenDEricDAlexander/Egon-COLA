package ${package}.infrastructure.teaching.repo.impl;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.infrastructure.teaching.repo.converter.CoursePOConverter;
import ${package}.infrastructure.teaching.repo.jpa.CourseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("courseRepository")
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {
    private final CourseJpaRepository courseJpaRepository;
    private final CoursePOConverter converter;

    @Override
    public Course save(Course course) {
        return converter.toDomain(courseJpaRepository.save(converter.toPO(course)));
    }

    @Override
    public Optional<Course> findById(String courseId) {
        return courseJpaRepository.findById(courseId).map(converter::toDomain);
    }

    @Override
    public Optional<Course> findByCode(CourseCode courseCode) {
        return courseJpaRepository.findByCourseCode(courseCode.value()).map(converter::toDomain);
    }
}
