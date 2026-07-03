#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application;

import ${package}.adapter.convertor.CourseAdapterConverter;
import ${package}.adapter.convertor.StudentAdapterConverter;
import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.domain.common.Page;
import ${package}.domain.student.model.Student;
import ${package}.domain.teaching.model.Course;
import ${package}.facade.api.StudentManagementFacade;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.RegisterStudentRequest;
import ${package}.facade.dto.StudentDTO;
import ${package}.start.StudentManagementApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StudentManagementApplication.class)
class StudentManagementFlowTest {
    @Autowired
    private StudentManage studentManage;

    @Autowired
    private CourseManage courseManage;

    @Autowired
    private StudentAdapterConverter studentAdapterConverter;

    @Autowired
    private CourseAdapterConverter courseAdapterConverter;

    @Autowired
    private StudentManagementFacade studentManagementFacade;

    @Test
    void register_student_and_assign_course() {
        Student student = studentManage.register("Mario", "mario@example.com");
        Course course = courseManage.create("Architecture", "Large monolith light domain architecture");

        courseManage.assignCourse(student.getId(), course.getId());

        Student saved = studentManage.getById(student.getId());
        assertThat(saved.getEmail()).isEqualTo("mario@example.com");
        assertThat(saved.getCourseIds()).containsExactly(course.getId());

        StudentDTO studentDTO = studentAdapterConverter.toDto(saved);
        CourseDTO courseDTO = courseAdapterConverter.toDto(course);
        assertThat(studentDTO.getStatus()).isEqualTo(saved.getStatus().name());
        assertThat(studentDTO.getCourseIds()).containsExactly(course.getId());
        assertThat(courseDTO.getId()).isEqualTo(course.getId());

        StudentDTO facadeStudent = studentManagementFacade.registerStudent(
                new RegisterStudentRequest("Luigi", "luigi@example.com"));
        assertThat(facadeStudent.getEmail()).isEqualTo("luigi@example.com");
    }

    @Test
    void get_student_page_returns_domain_page_and_facade_page() {
        String suffix = UUID.randomUUID().toString();
        String marioEmail = "mario-" + suffix + "@example.com";
        String luigiEmail = "luigi-" + suffix + "@example.com";

        studentManage.register("Mario", marioEmail);
        studentManagementFacade.registerStudent(new RegisterStudentRequest("Luigi", luigiEmail));

        Page<Student> studentPage = studentManage.getPage(1, 10);
        assertThat(studentPage.records()).extracting(Student::getEmail)
                .contains(marioEmail, luigiEmail);
        assertThat(studentPage.currentPage()).isEqualTo(1);
        assertThat(studentPage.pageSize()).isEqualTo(10);
        assertThat(studentPage.totalCount()).isGreaterThanOrEqualTo(2);

        PageResponse<StudentDTO> facadePage = studentManagementFacade.getStudents(1, 10);
        assertThat(facadePage.records()).extracting(StudentDTO::getEmail)
                .contains(marioEmail, luigiEmail);
        assertThat(facadePage.currentPage()).isEqualTo(1);
        assertThat(facadePage.pageSize()).isEqualTo(10);
        assertThat(facadePage.totalCount()).isGreaterThanOrEqualTo(2);
    }
}
