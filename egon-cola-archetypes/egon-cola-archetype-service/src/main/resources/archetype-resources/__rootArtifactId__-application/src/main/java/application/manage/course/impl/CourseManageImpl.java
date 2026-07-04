#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.course.impl;

import ${package}.application.manage.course.CourseManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.BizException;
import ${package}.common.exception.NotFoundException;
import ${package}.common.util.IdGenerator;
import ${package}.domain.client.course.CourseClient;
import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service("courseManage")
@Validated
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {

    @Qualifier("courseClientImpl")
    private final CourseClient courseClient;

    @Override
    @Transactional
    public Course create(String name, int credit) {
        Course course = Course.create(IdGenerator.nextId(), name, credit);
        if (courseClient.existsByName(name)) {
            throw new BizException(ErrorCodes.COURSE_NAME_DUPLICATED, "course name already exists");
        }
        return courseClient.save(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Course getById(String courseId) {
        return courseClient.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Course> getPage(int currentPage, int pageSize) {
        return courseClient.findPage(currentPage, pageSize);
    }
}
