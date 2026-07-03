package top.egon.light.infrastructure.repo.student.converter;

import top.egon.light.domain.student.model.Student;
import top.egon.light.infrastructure.repo.student.po.StudentPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = StudentDomainFactory.class)
public interface StudentDomainMapper extends BaseMapper<StudentPo, Student> {
    @Override
    @Mapping(target = "courseIds", ignore = true)
    Student convert(StudentPo studentPo);
}
