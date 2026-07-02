package top.egon.light.adapter.convertor;

import top.egon.light.application.manage.teaching.CourseView;
import top.egon.light.facade.dto.CourseDTO;

public final class CourseAdapterConverter {
    private CourseAdapterConverter() {
    }

    public static CourseDTO toDto(CourseView view) {
        return new CourseDTO(view.id(), view.name(), view.description());
    }
}
