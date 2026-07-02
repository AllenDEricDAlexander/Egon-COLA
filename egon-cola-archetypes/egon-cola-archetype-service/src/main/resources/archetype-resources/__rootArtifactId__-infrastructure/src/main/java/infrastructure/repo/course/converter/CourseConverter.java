#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.converter;

import java.time.LocalDateTime;

import ${package}.domain.entities.course.Course;
import ${package}.domain.enums.CourseStatus;
import ${package}.infrastructure.repo.course.po.CoursePo;
import org.springframework.stereotype.Component;

@Component
public class CourseConverter {

    public CoursePo toPo(Course course, LocalDateTime createdAt, LocalDateTime updatedAt) {
        CoursePo coursePo = new CoursePo();
        coursePo.setId(course.getId());
        coursePo.setName(course.getName());
        coursePo.setCredit(course.getCredit());
        coursePo.setStatus(course.getStatus().name());
        coursePo.setCreatedAt(createdAt);
        coursePo.setUpdatedAt(updatedAt);
        return coursePo;
    }

    public Course toDomain(CoursePo coursePo) {
        Course course = new Course();
        course.setId(coursePo.getId());
        course.setName(coursePo.getName());
        course.setCredit(coursePo.getCredit());
        course.setStatus(CourseStatus.valueOf(coursePo.getStatus()));
        return course;
    }
}
