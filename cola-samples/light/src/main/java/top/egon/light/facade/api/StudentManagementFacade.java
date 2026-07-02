package top.egon.light.facade.api;

import top.egon.light.facade.dto.AssignCourseRequest;
import top.egon.light.facade.dto.CourseDTO;
import top.egon.light.facade.dto.CreateCourseRequest;
import top.egon.light.facade.dto.RegisterStudentRequest;
import top.egon.light.facade.dto.StudentDTO;

public interface StudentManagementFacade {
    StudentDTO registerStudent(RegisterStudentRequest request);

    StudentDTO getStudent(String studentId);

    CourseDTO createCourse(CreateCourseRequest request);

    void assignCourse(AssignCourseRequest request);
}
