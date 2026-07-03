package ${package}.adapter.convertor;

import ${package}.domain.student.model.Student;
import ${package}.facade.dto.StudentDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentAdapterMapper extends BaseMapper<Student, StudentDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    StudentDTO convert(Student student);
}
