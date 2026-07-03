package top.egon.fable.infrastructure.repo.course.converter;

import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.domain.enums.CourseStatus;
import top.egon.fable.infrastructure.repo.course.po.CoursePo;
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
                coursePo.getName(),
                coursePo.getCredit(),
                course.getStatus().name(),
                createdAt,
                updatedAt);
    }

    public Course toDomain(CoursePo coursePo) {
        Course course = courseDomainMapper.convert(coursePo);
        course.setStatus(CourseStatus.valueOf(coursePo.getStatus()));
        return course;
    }
}
