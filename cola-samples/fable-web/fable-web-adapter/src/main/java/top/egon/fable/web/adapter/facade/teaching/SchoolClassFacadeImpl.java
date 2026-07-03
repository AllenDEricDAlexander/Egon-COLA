package top.egon.fable.web.adapter.facade.teaching;

import top.egon.fable.web.adapter.convertor.SchoolClassAdapterConverter;
import top.egon.fable.web.application.manage.teaching.SchoolClassManage;
import top.egon.fable.web.facade.dto.teaching.AssignUserToClassRequest;
import top.egon.fable.web.facade.dto.teaching.CreateSchoolClassRequest;
import top.egon.fable.web.facade.dto.teaching.SchoolClassDTO;
import top.egon.fable.web.facade.teaching.SchoolClassFacade;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;

@DubboService(
        interfaceClass = SchoolClassFacade.class,
        version = "1.0.0",
        group = "school-class"
)
@Validated
@RequiredArgsConstructor
public class SchoolClassFacadeImpl implements SchoolClassFacade {
    @Qualifier("schoolClassManage")
    private final SchoolClassManage schoolClassManage;

    @Qualifier("schoolClassAdapterConverter")
    private final SchoolClassAdapterConverter schoolClassAdapterConverter;

    @Override
    public SchoolClassDTO createSchoolClass(CreateSchoolClassRequest request) {
        return schoolClassAdapterConverter.toDto(schoolClassManage.create(request.name(), request.gradeName()));
    }

    @Override
    public void assignUser(AssignUserToClassRequest request) {
        schoolClassManage.assignUser(request.userId(), request.schoolClassId());
    }
}
