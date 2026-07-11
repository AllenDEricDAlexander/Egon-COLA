#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.converter;

import ${package}.domain.entities.course.Course;
import ${package}.domain.enums.CourseStatus;
import ${package}.domain.vos.course.CourseCode;
import ${package}.infrastructure.repo.course.po.CoursePo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("courseConverter")
@RequiredArgsConstructor
public class CourseConverter {

    @Qualifier("coursePoMapperImpl")
    private final CoursePoMapper coursePoMapper;

    @Qualifier("courseDomainMapperImpl")
    private final CourseDomainMapper courseDomainMapper;

    public CoursePo toPo(Course course, LocalDateTime createdAt, LocalDateTime updatedAt) {
        CoursePo coursePo = coursePoMapper.convert(course);
        return new CoursePo(
                coursePo.getId(),
                course.getCode() == null ? "LEGACY-" + course.getId() : course.getCode().value(),
                coursePo.getName(),
                coursePo.getCredit(),
                course.getStatus().name(),
                createdAt,
                updatedAt);
    }

    public Course toDomain(CoursePo coursePo) {
        Course course = courseDomainMapper.convert(coursePo);
        course.setCode(new CourseCode(coursePo.getCode()));
        course.setStatus(CourseStatus.valueOf(coursePo.getStatus()));
        return course;
    }
}
