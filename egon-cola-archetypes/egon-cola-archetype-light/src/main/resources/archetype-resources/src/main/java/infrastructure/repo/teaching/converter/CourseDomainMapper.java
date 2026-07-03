package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.teaching.model.Course;
import ${package}.infrastructure.repo.teaching.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CourseDomainFactory.class)
public interface CourseDomainMapper extends BaseMapper<CoursePo, Course> {
}
