package top.egon.fable.infrastructure.repo.course.converter;

import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.infrastructure.repo.course.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseDomainMapper extends BaseMapper<CoursePo, Course> {

    @Override
    @Mapping(target = "status", ignore = true)
    Course convert(CoursePo coursePo);

    @Override
    @Mapping(target = "status", ignore = true)
    Course convert(CoursePo coursePo, @MappingTarget Course course);
}
