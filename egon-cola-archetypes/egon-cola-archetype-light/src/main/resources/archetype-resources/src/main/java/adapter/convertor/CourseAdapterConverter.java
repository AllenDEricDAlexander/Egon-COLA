package ${package}.adapter.convertor;

import ${package}.application.manage.teaching.CourseView;
import ${package}.facade.dto.CourseDTO;

public final class CourseAdapterConverter {
    private CourseAdapterConverter() {
    }

    public static CourseDTO toDto(CourseView view) {
        return new CourseDTO(view.id(), view.name(), view.description());
    }
}
