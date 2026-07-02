package ${package}.adapter.facade.teaching;

import ${package}.adapter.convertor.SchoolClassAdapterConverter;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.facade.dto.teaching.AssignUserToClassRequest;
import ${package}.facade.dto.teaching.CreateSchoolClassRequest;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import ${package}.facade.teaching.SchoolClassFacade;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class SchoolClassFacadeImpl implements SchoolClassFacade {
    private final SchoolClassManage schoolClassManage;

    public SchoolClassFacadeImpl(SchoolClassManage schoolClassManage) {
        this.schoolClassManage = schoolClassManage;
    }

    @Override
    public SchoolClassDTO createSchoolClass(CreateSchoolClassRequest request) {
        return SchoolClassAdapterConverter.toDto(schoolClassManage.create(request.name(), request.gradeName()));
    }

    @Override
    public void assignUser(AssignUserToClassRequest request) {
        schoolClassManage.assignUser(request.userId(), request.schoolClassId());
    }
}
