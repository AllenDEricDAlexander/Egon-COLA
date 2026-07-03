package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.domain.student.model.StudentStatus;
import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import ${package}.infrastructure.repo.student.po.StudentPo;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("studentPoConverter")
@RequiredArgsConstructor
public class StudentPoConverter {
    @Qualifier("converter")
    private final Converter converter;

    public StudentPo toPo(Student student) {
        StudentPo studentPo = converter.convert(student, StudentPo.class);
        return new StudentPo(studentPo.getId(), studentPo.getName(), studentPo.getEmail(), student.getStatus().name(), LocalDateTime.now());
    }

    public Student toDomain(StudentPo studentPo, List<StudentCoursePo> coursePos) {
        List<String> courseIds = coursePos.stream()
                .map(StudentCoursePo::getCourseId)
                .toList();
        Student student = converter.convert(studentPo, Student.class);
        return Student.restore(student.getId(), student.getName(), student.getEmail(), student.getStatus(), courseIds);
    }

    @Mapper(componentModel = "spring")
    public interface StudentMapper extends BaseMapper<Student, StudentPo> {
        @Override
        @Mapping(target = "createdAt", ignore = true)
        StudentPo convert(Student student);
    }

    @Mapper(componentModel = "spring", uses = StudentDomainFactory.class)
    public interface StudentDomainMapper extends BaseMapper<StudentPo, Student> {
        @Override
        @Mapping(target = "courseIds", ignore = true)
        Student convert(StudentPo studentPo);

        @Override
        @Mapping(target = "courseIds", ignore = true)
        Student convert(StudentPo studentPo, Student student);
    }

    @Component("studentDomainFactory")
    @RequiredArgsConstructor
    public static class StudentDomainFactory {
        @ObjectFactory
        public Student create(StudentPo studentPo) {
            return Student.restore(studentPo.getId(), studentPo.getName(), studentPo.getEmail(), StudentStatus.valueOf(studentPo.getStatus()), List.of());
        }
    }
}
