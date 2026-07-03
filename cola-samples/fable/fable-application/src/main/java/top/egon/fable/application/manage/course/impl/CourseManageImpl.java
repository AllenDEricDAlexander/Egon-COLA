package top.egon.fable.application.manage.course.impl;

import top.egon.fable.application.manage.course.CourseManage;
import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.NotFoundException;
import top.egon.fable.common.util.IdGenerator;
import top.egon.fable.domain.common.Page;
import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.domain.repos.course.CourseRepository;
import top.egon.fable.domain.service.course.CourseDomainService;
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
