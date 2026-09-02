package top.egon.cola.archetype.source.light.adapter.teaching.rpc;

import top.egon.cola.archetype.source.light.facade.teaching.SchoolClassFacade;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.ScheduleCourseDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.SchoolClassDetailDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(interfaceClass = SchoolClassFacade.class, version = "1.0.0", group = "teaching")
@RequiredArgsConstructor
public class SchoolClassRpcProvider implements SchoolClassFacade {
    @Qualifier("schoolClassFacadeImpl")
    private final SchoolClassFacade delegate;

    @Override
    public SchoolClassDetailDTO createSchoolClass(CreateSchoolClassDTO request) {
        return delegate.createSchoolClass(request);
    }

    @Override
    public SchoolClassDetailDTO scheduleCourse(ScheduleCourseDTO request) {
        return delegate.scheduleCourse(request);
    }

    @Override
    public SchoolClassDetailDTO getSchoolClass(Long schoolClassId) {
        return delegate.getSchoolClass(schoolClassId);
    }
}
