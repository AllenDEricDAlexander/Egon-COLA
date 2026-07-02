package ${package}.adapter.convertor;

import ${package}.application.manage.student.StudentView;
import ${package}.facade.dto.StudentDTO;

public final class StudentAdapterConverter {
    private StudentAdapterConverter() {
    }

    public static StudentDTO toDto(StudentView view) {
        return new StudentDTO(view.id(), view.name(), view.email(), view.status(), view.courseIds());
    }
}
