package top.egon.light.infrastructure.repo.teaching.converter;

import top.egon.light.domain.teaching.model.Course;
import top.egon.light.infrastructure.repo.teaching.po.CoursePo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CourseDomainFactory.class)
public interface CourseDomainMapper extends BaseMapper<CoursePo, Course> {
}
