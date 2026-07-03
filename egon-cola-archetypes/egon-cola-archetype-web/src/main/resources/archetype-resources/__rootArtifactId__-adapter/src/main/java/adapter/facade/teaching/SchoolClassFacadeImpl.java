package ${package}.adapter.facade.teaching;

import ${package}.adapter.convertor.SchoolClassAdapterConverter;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.facade.dto.teaching.AssignUserToClassRequest;
import ${package}.facade.dto.teaching.CreateSchoolClassRequest;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import ${package}.facade.teaching.SchoolClassFacade;
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
