package ${package}.domain.student.service;

import ${package}.common.exceptions.BizException;
import ${package}.domain.student.model.Student;

public class StudentDomainService {
    public Student register(String studentId, String name, String email) {
        return Student.register(studentId, name, email);
    }

    public Student assignCourse(Student student, String courseId) {
        if (student.hasCourse(courseId)) {
            throw new BizException("STUDENT_COURSE_DUPLICATED", "student already assigned to course");
        }
        student.assignCourse(courseId);
        return student;
    }
}
