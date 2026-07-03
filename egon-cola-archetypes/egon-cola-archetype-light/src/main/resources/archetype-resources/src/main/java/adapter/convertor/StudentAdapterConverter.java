package ${package}.adapter.convertor;

import ${package}.domain.student.model.Student;
import ${package}.facade.dto.StudentDTO;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("studentAdapterConverter")
@RequiredArgsConstructor
public class StudentAdapterConverter {
    @Qualifier("converter")
    private final Converter converter;

    public StudentDTO toDto(Student student) {
        StudentDTO dto = converter.convert(student, StudentDTO.class);
        dto.setStatus(student.getStatus().name());
        return dto;
    }

    @Mapper(componentModel = "spring")
    public interface StudentMapper extends BaseMapper<Student, StudentDTO> {
        @Override
        @Mapping(target = "status", ignore = true)
        StudentDTO convert(Student student);
    }
}
