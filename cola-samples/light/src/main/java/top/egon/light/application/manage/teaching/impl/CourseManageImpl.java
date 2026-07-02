package top.egon.light.application.manage.teaching.impl;

import top.egon.light.application.manage.teaching.CourseManage;
import top.egon.light.application.manage.teaching.CourseView;
import top.egon.light.common.constants.ErrorCodes;
import top.egon.light.common.exceptions.NotFoundException;
import top.egon.light.common.utils.IdGenerator;
import top.egon.light.domain.student.model.Student;
import top.egon.light.domain.student.repos.StudentRepository;
import top.egon.light.domain.student.service.StudentDomainService;
import top.egon.light.domain.teaching.model.Course;
import top.egon.light.domain.teaching.repos.CourseRepository;
import top.egon.light.domain.teaching.service.CourseDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseManageImpl implements CourseManage {
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CourseDomainService courseDomainService;
    private final StudentDomainService studentDomainService;

    public CourseManageImpl(CourseRepository courseRepository, StudentRepository studentRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.courseDomainService = new CourseDomainService();
        this.studentDomainService = new StudentDomainService();
    }

    @Override
    @Transactional
    public CourseView create(String name, String description) {
        Course course = courseDomainService.create(IdGenerator.nextId(), name, description);
        return toView(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseView getById(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
        return toView(course);
    }

    @Override
    @Transactional
    public void assignCourse(String studentId, String courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.STUDENT_NOT_FOUND, "student not found"));
        courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
        studentRepository.save(studentDomainService.assignCourse(student, courseId));
    }

    private CourseView toView(Course course) {
        return new CourseView(course.getId(), course.getName(), course.getDescription());
    }
}
