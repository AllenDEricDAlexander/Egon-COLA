package top.egon.light.application.manage.teaching.impl;

import top.egon.light.application.manage.teaching.CourseManage;
import top.egon.light.common.constants.ErrorCodes;
import top.egon.light.common.exceptions.NotFoundException;
import top.egon.light.common.utils.IdGenerator;
import top.egon.light.domain.student.model.Student;
import top.egon.light.domain.student.repos.StudentRepository;
import top.egon.light.domain.student.service.StudentDomainService;
import top.egon.light.domain.teaching.model.Course;
import top.egon.light.domain.teaching.repos.CourseRepository;
import top.egon.light.domain.teaching.service.CourseDomainService;
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
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
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
}
