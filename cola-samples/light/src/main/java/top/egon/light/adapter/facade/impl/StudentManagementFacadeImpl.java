package top.egon.light.adapter.facade.impl;

import org.springframework.stereotype.Service;
import top.egon.light.adapter.convertor.CourseAdapterConverter;
import top.egon.light.adapter.convertor.StudentAdapterConverter;
import top.egon.light.application.manage.student.StudentManage;
import top.egon.light.application.manage.teaching.CourseManage;
import top.egon.light.facade.api.StudentManagementFacade;
import top.egon.light.facade.dto.AssignCourseRequest;
import top.egon.light.facade.dto.CourseDTO;
import top.egon.light.facade.dto.CreateCourseRequest;
import top.egon.light.facade.dto.RegisterStudentRequest;
import top.egon.light.facade.dto.StudentDTO;

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
