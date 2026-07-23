package ${package}.adapter.teaching.rpc;

import ${package}.facade.teaching.CourseFacade;
import ${package}.facade.teaching.dto.CourseDTO;
import ${package}.facade.teaching.dto.CreateCourseDTO;
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
    public CourseDTO getCourse(String courseId) {
        return delegate.getCourse(courseId);
    }
}
