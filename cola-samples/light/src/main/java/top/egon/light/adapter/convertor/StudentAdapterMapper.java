package top.egon.light.adapter.convertor;

import top.egon.light.domain.student.model.Student;
import top.egon.light.facade.dto.StudentDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentAdapterMapper extends BaseMapper<Student, StudentDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    StudentDTO convert(Student student);
}
