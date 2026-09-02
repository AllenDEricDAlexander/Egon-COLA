package top.egon.cola.archetype.source.light.adapter.teaching.rpc;

import top.egon.cola.archetype.source.light.facade.teaching.CourseFacade;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(interfaceClass = CourseFacade.class, version = "1.0.0", group = "teaching")
@RequiredArgsConstructor
public class CourseRpcProvider implements CourseFacade {
    @Qualifier("courseFacadeImpl")
    private final CourseFacade delegate;

    @Override
    public CourseDTO createCourse(CreateCourseDTO request) {
        return delegate.createCourse(request);
    }

    @Override
    public CourseDTO getCourse(Long courseId) {
        return delegate.getCourse(courseId);
    }
}
