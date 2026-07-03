package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.infrastructure.repo.student.po.StudentPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentPoMapper extends BaseMapper<Student, StudentPo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    StudentPo convert(Student student);
}
