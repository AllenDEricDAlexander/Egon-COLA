package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("coursePoConverter")
@RequiredArgsConstructor
public class CoursePoConverter {
    @Qualifier("coursePoMapperImpl")
    private final CoursePoMapper coursePoMapper;

    @Qualifier("courseDomainMapperImpl")
    private final CourseDomainMapper courseDomainMapper;

    public CoursePo toPo(Course course) {
        CoursePo coursePo = coursePoMapper.convert(course);
        return new CoursePo(coursePo.getId(), coursePo.getName(), coursePo.getDescription(), LocalDateTime.now());
    }

    public Course toDomain(CoursePo coursePo) {
        return courseDomainMapper.convert(coursePo);
    }
}
