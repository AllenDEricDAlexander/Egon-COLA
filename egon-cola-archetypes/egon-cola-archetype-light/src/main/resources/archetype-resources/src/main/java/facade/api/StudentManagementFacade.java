package ${package}.facade.api;

import ${package}.facade.dto.AssignCourseRequest;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;

public interface StudentManagementFacade {
    StudentDTO registerStudent(RegisterStudentRequest request);

    StudentDTO getStudent(String studentId);

    CourseDTO createCourse(CreateCourseRequest request);

    void assignCourse(AssignCourseRequest request);
}
