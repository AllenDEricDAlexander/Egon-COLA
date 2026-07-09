package ${package}.adapter.teaching.rpc;

import ${package}.facade.teaching.CourseFacade;
import ${package}.facade.teaching.dto.CourseDTO;
import ${package}.facade.teaching.dto.CreateCourseDTO;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(interfaceClass = CourseFacade.class, version = "1.0.0", group = "teaching")
public class CourseRpcProvider implements CourseFacade {
    private final CourseFacade delegate;

    public CourseRpcProvider(@Qualifier("courseFacadeImpl") CourseFacade delegate) {
        this.delegate = delegate;
    }

    @Override
    public CourseDTO createCourse(CreateCourseDTO request) {
        return delegate.createCourse(request);
    }

    @Override
    public CourseDTO getCourse(String courseId) {
        return delegate.getCourse(courseId);
    }
}
