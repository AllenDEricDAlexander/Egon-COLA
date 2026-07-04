#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.impl.course;

import java.util.Optional;

import ${package}.domain.client.course.CourseClient;
import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import ${package}.domain.repos.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component("courseClientImpl")
@Validated
@RequiredArgsConstructor
public class CourseClientImpl implements CourseClient {

    @Qualifier("courseRepositoryImpl")
    private final CourseRepository courseRepository;

    @Override
    public Course save(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public Optional<Course> findById(String courseId) {
        return courseRepository.findById(courseId);
    }

    @Override
    public Page<Course> findPage(int currentPage, int pageSize) {
        return courseRepository.findPage(currentPage, pageSize);
    }

    @Override
    public boolean existsByName(String name) {
        return courseRepository.existsByName(name);
    }
}
