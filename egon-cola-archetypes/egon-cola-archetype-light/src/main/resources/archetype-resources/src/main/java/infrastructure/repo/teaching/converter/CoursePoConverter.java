package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("coursePoConverter")
@RequiredArgsConstructor
public class CoursePoConverter {
    @Qualifier("converter")
    private final Converter converter;

    public CoursePo toPo(Course course) {
        CoursePo coursePo = converter.convert(course, CoursePo.class);
        return new CoursePo(coursePo.getId(), coursePo.getName(), coursePo.getDescription(), LocalDateTime.now());
    }

    public Course toDomain(CoursePo coursePo) {
        return converter.convert(coursePo, Course.class);
    }

    @Mapper(componentModel = "spring")
    public interface CourseMapper extends BaseMapper<Course, CoursePo> {
        @Override
        @Mapping(target = "createdAt", ignore = true)
        CoursePo convert(Course course);
    }

    @Mapper(componentModel = "spring", uses = CourseDomainFactory.class)
    public interface CourseDomainMapper extends BaseMapper<CoursePo, Course> {
    }

    @Component("courseDomainFactory")
    @RequiredArgsConstructor
    public static class CourseDomainFactory {
        @ObjectFactory
        public Course create(CoursePo coursePo) {
            return Course.create(coursePo.getId(), coursePo.getName(), coursePo.getDescription());
        }
    }
}
