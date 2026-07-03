package ${package}.adapter.facade.impl;

import ${package}.adapter.convertor.CourseAdapterConverter;
import ${package}.adapter.convertor.StudentAdapterConverter;
import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.facade.api.StudentManagementFacade;
import ${package}.facade.dto.AssignCourseRequest;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(
        interfaceClass = StudentManagementFacade.class,
        version = "1.0.0",
        group = "student-management"
)
@RequiredArgsConstructor
public class StudentManagementFacadeImpl implements StudentManagementFacade {
    @Qualifier("studentManage")
    private final StudentManage studentManage;

    @Qualifier("courseManage")
    private final CourseManage courseManage;

    @Qualifier("studentAdapterConverter")
    private final StudentAdapterConverter studentAdapterConverter;

    @Qualifier("courseAdapterConverter")
    private final CourseAdapterConverter courseAdapterConverter;

    @Override
    public StudentDTO registerStudent(RegisterStudentRequest request) {
        return studentAdapterConverter.toDto(studentManage.register(request.name(), request.email()));
    }

    @Override
    public StudentDTO getStudent(String studentId) {
        return studentAdapterConverter.toDto(studentManage.getById(studentId));
    }

    @Override
    public PageResponse<StudentDTO> getStudents(int currentPage, int pageSize) {
        return studentAdapterConverter.toPageResponse(studentManage.getPage(currentPage, pageSize));
    }

    @Override
    public CourseDTO createCourse(CreateCourseRequest request) {
        return courseAdapterConverter.toDto(courseManage.create(request.name(), request.description()));
    }

    @Override
    public void assignCourse(AssignCourseRequest request) {
        courseManage.assignCourse(request.studentId(), request.courseId());
    }
}
