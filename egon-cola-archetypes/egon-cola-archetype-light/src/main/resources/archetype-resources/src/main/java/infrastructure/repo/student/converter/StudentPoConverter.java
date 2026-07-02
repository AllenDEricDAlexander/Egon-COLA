package ${package}.infrastructure.repo.student.converter;

import ${package}.domain.student.model.Student;
import ${package}.domain.student.model.StudentStatus;
import ${package}.infrastructure.repo.student.po.StudentCoursePo;
import ${package}.infrastructure.repo.student.po.StudentPo;

import java.time.LocalDateTime;
import java.util.List;

public final class StudentPoConverter {
    private StudentPoConverter() {
    }

    public static StudentPo toPo(Student student) {
        return new StudentPo(student.getId(), student.getName(), student.getEmail(), student.getStatus().name(), LocalDateTime.now());
    }

    public static Student toDomain(StudentPo studentPo, List<StudentCoursePo> coursePos) {
        List<String> courseIds = coursePos.stream()
                .map(StudentCoursePo::getCourseId)
                .toList();
        return Student.restore(studentPo.getId(), studentPo.getName(), studentPo.getEmail(), StudentStatus.valueOf(studentPo.getStatus()), courseIds);
    }
}
