package top.egon.fable-web.adapter.facade.teaching;

import top.egon.fable-web.adapter.convertor.SchoolClassAdapterConverter;
import top.egon.fable-web.application.manage.teaching.SchoolClassManage;
import top.egon.fable-web.facade.dto.teaching.AssignUserToClassRequest;
import top.egon.fable-web.facade.dto.teaching.CreateSchoolClassRequest;
import top.egon.fable-web.facade.dto.teaching.SchoolClassDTO;
import top.egon.fable-web.facade.teaching.SchoolClassFacade;
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
