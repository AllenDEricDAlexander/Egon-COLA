#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application;

import ${package}.adapter.convertor.CourseAdapterConverter;
import ${package}.adapter.convertor.StudentAdapterConverter;
import ${package}.application.manage.student.StudentManage;
import ${package}.application.manage.teaching.CourseManage;
import ${package}.domain.student.model.Student;
import ${package}.domain.teaching.model.Course;
import ${package}.facade.dto.CourseDTO;
import ${package}.facade.dto.StudentDTO;
import ${package}.start.StudentManagementApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
    }
}
