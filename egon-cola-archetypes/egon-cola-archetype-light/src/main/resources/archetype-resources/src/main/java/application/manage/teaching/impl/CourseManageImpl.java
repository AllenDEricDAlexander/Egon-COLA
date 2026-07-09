package ${package}.application.manage.teaching.impl;

import ${package}.application.manage.teaching.CourseManage;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exceptions.NotFoundException;
import ${package}.common.utils.IdGenerator;
import ${package}.domain.student.model.Student;
import ${package}.domain.student.repos.StudentRepository;
import ${package}.domain.student.service.StudentDomainService;
import ${package}.domain.teaching.model.Course;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.service.CourseDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("courseManage")
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {
    @Qualifier("courseRepositoryImpl")
    private final CourseRepository courseRepository;

    @Qualifier("studentRepositoryImpl")
    private final StudentRepository studentRepository;

    @Qualifier("courseDomainService")
    private final CourseDomainService courseDomainService;

    @Qualifier("studentDomainService")
    private final StudentDomainService studentDomainService;

    @Override
    @Transactional
    public Course create(String name, String description) {
        Course course = courseDomainService.create(IdGenerator.nextId(), name, description);
        return courseRepository.save(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Course getById(String courseId) {
        return courseRepository.findLegacyById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
    }

    @Override
    @Transactional
    public void assignCourse(String studentId, String courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.STUDENT_NOT_FOUND, "student not found"));
        courseRepository.findLegacyById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
        studentRepository.save(studentDomainService.assignCourse(student, courseId));
    }
}
