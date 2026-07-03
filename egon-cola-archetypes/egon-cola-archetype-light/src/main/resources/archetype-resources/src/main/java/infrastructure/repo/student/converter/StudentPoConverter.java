package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import ${package}.infrastructure.repo.student.po.StudentPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("studentPoConverter")
@RequiredArgsConstructor
public class StudentPoConverter {
    @Qualifier("studentPoMapperImpl")
    private final StudentPoMapper studentPoMapper;

    @Qualifier("studentDomainMapperImpl")
    private final StudentDomainMapper studentDomainMapper;

    public StudentPo toPo(Student student) {
        StudentPo studentPo = studentPoMapper.convert(student);
        return new StudentPo(studentPo.getId(), studentPo.getName(), studentPo.getEmail(), student.getStatus().name(), LocalDateTime.now());
    }

    public Student toDomain(StudentPo studentPo, List<StudentCoursePo> coursePos) {
        List<String> courseIds = coursePos.stream()
                .map(StudentCoursePo::getCourseId)
                .toList();
        Student student = studentDomainMapper.convert(studentPo);
        return Student.restore(student.getId(), student.getName(), student.getEmail(), student.getStatus(), courseIds);
    }
}
