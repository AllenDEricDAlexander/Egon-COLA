package ${package}.adapter.teaching.rpc;

import ${package}.facade.teaching.SchoolClassFacade;
import ${package}.facade.teaching.dto.CreateSchoolClassDTO;
import ${package}.facade.teaching.dto.ScheduleCourseDTO;
import ${package}.facade.teaching.dto.SchoolClassDetailDTO;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(interfaceClass = SchoolClassFacade.class, version = "1.0.0", group = "teaching")
public class SchoolClassRpcProvider implements SchoolClassFacade {
    private final SchoolClassFacade delegate;

    public SchoolClassRpcProvider(
            @Qualifier("schoolClassFacadeImpl") SchoolClassFacade delegate) {
        this.delegate = delegate;
    }

    @Override
    public SchoolClassDetailDTO createSchoolClass(CreateSchoolClassDTO request) {
        return delegate.createSchoolClass(request);
    }

    @Override
    public SchoolClassDetailDTO scheduleCourse(ScheduleCourseDTO request) {
        return delegate.scheduleCourse(request);
    }

    @Override
    public SchoolClassDetailDTO getSchoolClass(String schoolClassId) {
        return delegate.getSchoolClass(schoolClassId);
    }
}
