package top.egon.light.adapter.convertor;

import top.egon.light.application.manage.student.StudentView;
import top.egon.light.facade.dto.StudentDTO;

public final class StudentAdapterConverter {
    private StudentAdapterConverter() {
    }

    public static StudentDTO toDto(StudentView view) {
        return new StudentDTO(view.id(), view.name(), view.email(), view.status(), view.courseIds());
    }
}
