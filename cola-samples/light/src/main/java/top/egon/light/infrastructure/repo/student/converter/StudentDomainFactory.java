package top.egon.light.infrastructure.repo.student.converter;

import top.egon.light.domain.student.model.Student;
import top.egon.light.domain.student.model.StudentStatus;
import top.egon.light.infrastructure.repo.student.po.StudentPo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("studentDomainFactory")
public class StudentDomainFactory {
    @ObjectFactory
    public Student create(StudentPo studentPo) {
        return Student.restore(studentPo.getId(), studentPo.getName(), studentPo.getEmail(), StudentStatus.valueOf(studentPo.getStatus()), List.of());
    }
}
