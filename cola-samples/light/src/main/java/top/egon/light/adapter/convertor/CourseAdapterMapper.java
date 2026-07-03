package top.egon.light.adapter.convertor;

import top.egon.light.domain.teaching.model.Course;
import top.egon.light.facade.dto.CourseDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseAdapterMapper extends BaseMapper<Course, CourseDTO> {
}
