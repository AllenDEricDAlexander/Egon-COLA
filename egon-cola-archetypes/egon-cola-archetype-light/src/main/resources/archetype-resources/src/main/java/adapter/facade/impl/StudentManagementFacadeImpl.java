package ${package}.adapter.facade.impl;

import ${package}.adapter.convertor.CourseAdapterConverter;
import ${package}.adapter.convertor.StudentAdapterConverter;
import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.facade.api.StudentManagementFacade;
import ${package}.facade.dto.AssignCourseRequest;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;
import org.springframework.stereotype.Service;

@Service
public class StudentManagementFacadeImpl implements StudentManagementFacade {
    private final StudentManage studentManage;
    private final CourseManage courseManage;

    public StudentManagementFacadeImpl(StudentManage studentManage, CourseManage courseManage) {
        this.studentManage = studentManage;
        this.courseManage = courseManage;
    }

    @Override
    public StudentDTO registerStudent(RegisterStudentRequest request) {
        return StudentAdapterConverter.toDto(studentManage.register(request.name(), request.email()));
    }

    @Override
    public StudentDTO getStudent(String studentId) {
        return StudentAdapterConverter.toDto(studentManage.getById(studentId));
    }

    @Override
    public CourseDTO createCourse(CreateCourseRequest request) {
        return CourseAdapterConverter.toDto(courseManage.create(request.name(), request.description()));
    }

    @Override
    public void assignCourse(AssignCourseRequest request) {
        courseManage.assignCourse(request.studentId(), request.courseId());
    }
}
