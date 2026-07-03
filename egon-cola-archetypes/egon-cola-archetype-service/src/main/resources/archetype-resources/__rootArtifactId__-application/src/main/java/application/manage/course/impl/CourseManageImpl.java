#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.course.impl;

import ${package}.application.manage.course.CourseManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.NotFoundException;
import ${package}.common.util.IdGenerator;
import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import ${package}.domain.repos.course.CourseRepository;
import ${package}.domain.service.course.CourseDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("courseManage")
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {

    @Qualifier("courseRepositoryImpl")
    private final CourseRepository courseRepository;

    @Qualifier("courseDomainService")
    private final CourseDomainService courseDomainService;

    @Override
    @Transactional
    public Course create(String name, int credit) {
        Course course = Course.create(IdGenerator.nextId(), name, credit);
        courseDomainService.ensureCourseNameAvailable(name);
        return courseRepository.save(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Course getById(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Course> getPage(int currentPage, int pageSize) {
        return courseRepository.findPage(currentPage, pageSize);
    }
}
