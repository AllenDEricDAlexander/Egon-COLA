package ${package}.infrastructure.teaching.repo.impl;

import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.infrastructure.teaching.repo.converter.CoursePOConverter;
import ${package}.infrastructure.teaching.repo.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("courseRepository")
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {
    private final CourseMapper courseMapper;
    private final CoursePOConverter converter;

    @Override
    public Course save(Course course) {
        var po = converter.toPO(course);
        int affected = courseMapper.selectById(po.getId()) == null
                ? courseMapper.insert(po)
                : courseMapper.updateById(po);
        if (affected != 1) {
            throw new IllegalStateException("course persistence affected " + affected + " rows");
        }
        return converter.toDomain(po);
    }

    @Override
    public Optional<Course> findById(long courseId) {
        return Optional.ofNullable(courseMapper.selectById(courseId)).map(converter::toDomain);
    }

    @Override
    public Optional<Course> findByCode(CourseCode courseCode) {
        return Optional.ofNullable(courseMapper.findByCourseCode(courseCode.value()))
                .map(converter::toDomain);
    }
}
