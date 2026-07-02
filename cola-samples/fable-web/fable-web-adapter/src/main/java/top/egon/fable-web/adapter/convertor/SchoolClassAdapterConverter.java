package top.egon.fable-web.adapter.convertor;

import top.egon.fable-web.application.manage.teaching.SchoolClassView;
import top.egon.fable-web.facade.dto.teaching.SchoolClassDTO;

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
