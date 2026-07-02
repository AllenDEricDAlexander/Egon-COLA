package ${package}.adapter.convertor;

import ${package}.application.manage.teaching.SchoolClassView;
import ${package}.facade.dto.teaching.SchoolClassDTO;

public final class SchoolClassAdapterConverter {
    private SchoolClassAdapterConverter() {
    }

    public static SchoolClassDTO toDto(SchoolClassView schoolClassView) {
        return new SchoolClassDTO(
                schoolClassView.id(),
                schoolClassView.name(),
                schoolClassView.gradeName(),
                schoolClassView.userIds());
    }
}
