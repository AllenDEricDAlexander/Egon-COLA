#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.converter;

import ${package}.domain.entities.course.Course;
import ${package}.domain.enums.CourseStatus;
import ${package}.infrastructure.repo.course.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("courseConverter")
@RequiredArgsConstructor
public class CourseConverter {

    @Qualifier("converter")
    private final Converter converter;

    public CoursePo toPo(Course course, LocalDateTime createdAt, LocalDateTime updatedAt) {
        CoursePo coursePo = converter.convert(course, CoursePo.class);
        return new CoursePo(
                coursePo.getId(),
                coursePo.getName(),
                coursePo.getCredit(),
                course.getStatus().name(),
                createdAt,
                updatedAt);
    }

    public Course toDomain(CoursePo coursePo) {
        Course course = converter.convert(coursePo, Course.class);
        course.setStatus(CourseStatus.valueOf(coursePo.getStatus()));
        return course;
    }

    @Mapper(componentModel = "spring")
    public interface CourseMapper extends BaseMapper<Course, CoursePo> {

        @Override
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "status", ignore = true)
        CoursePo convert(Course course);

        @Override
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "status", ignore = true)
        CoursePo convert(Course course, @MappingTarget CoursePo coursePo);
    }

    @Mapper(componentModel = "spring")
    public interface CourseDomainMapper extends BaseMapper<CoursePo, Course> {

        @Override
        @Mapping(target = "status", ignore = true)
        Course convert(CoursePo coursePo);

        @Override
        @Mapping(target = "status", ignore = true)
        Course convert(CoursePo coursePo, @MappingTarget Course course);
    }
}
