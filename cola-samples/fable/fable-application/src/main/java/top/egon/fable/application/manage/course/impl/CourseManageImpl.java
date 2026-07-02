package top.egon.fable.application.manage.course.impl;

import top.egon.fable.application.manage.course.CourseManage;
import top.egon.fable.application.view.course.CourseView;
import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.NotFoundException;
import top.egon.fable.common.util.IdGenerator;
import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.domain.repos.course.CourseRepository;
import top.egon.fable.domain.service.course.CourseDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseManageImpl implements CourseManage {

    private final CourseRepository courseRepository;

    private final CourseDomainService courseDomainService;

    public CourseManageImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
        this.courseDomainService = new CourseDomainService(courseRepository);
    }

    @Override
    @Transactional
    public CourseView create(String name, int credit) {
        Course course = Course.create(IdGenerator.nextId(), name, credit);
        courseDomainService.ensureCourseNameAvailable(name);
        return toView(courseRepository.save(course));
    }

    @Override
    public CourseView getById(String courseId) {
        return courseRepository.findById(courseId)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
    }

    private CourseView toView(Course course) {
        return new CourseView(course.getId(), course.getName(), course.getCredit(), course.getStatus().name());
    }
}
