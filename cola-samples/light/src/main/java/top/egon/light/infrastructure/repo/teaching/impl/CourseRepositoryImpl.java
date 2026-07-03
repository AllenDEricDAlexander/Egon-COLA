package top.egon.light.infrastructure.repo.teaching.impl;

import top.egon.light.domain.teaching.model.Course;
import top.egon.light.domain.teaching.repos.CourseRepository;
import top.egon.light.infrastructure.repo.teaching.converter.CoursePoConverter;
import top.egon.light.infrastructure.repo.teaching.jpa.CourseJpaRepository;
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
    public Optional<Course> findById(String courseId) {
        return courseJpaRepository.findById(courseId).map(coursePoConverter::toDomain);
    }
}
