package ${package}.facade.api;

import ${package}.facade.dto.AssignCourseRequest;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.CreateCourseRequest;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface StudentManagementFacade {
    StudentDTO registerStudent(@Valid @NotNull RegisterStudentRequest request);

    StudentDTO getStudent(String studentId);

    PageResponse<StudentDTO> getStudents(int currentPage, int pageSize);

    CourseDTO createCourse(@Valid @NotNull CreateCourseRequest request);

    void assignCourse(@Valid @NotNull AssignCourseRequest request);
}
