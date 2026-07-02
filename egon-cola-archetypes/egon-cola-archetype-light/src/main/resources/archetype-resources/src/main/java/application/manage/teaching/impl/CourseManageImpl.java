package ${package}.application.manage.teaching.impl;

import ${package}.application.manage.teaching.CourseManage;
import ${package}.application.manage.teaching.CourseView;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.domain.student.service.StudentDomainService;
import ${package}.domain.teaching.model.Course;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.service.CourseDomainService;
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
