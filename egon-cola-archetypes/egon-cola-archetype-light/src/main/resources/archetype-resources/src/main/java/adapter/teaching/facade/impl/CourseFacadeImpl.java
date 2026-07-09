package ${package}.adapter.teaching.facade.impl;

import ${package}.application.teaching.command.CreateCourseCommand;
import ${package}.application.teaching.manage.CourseManage;
import ${package}.application.teaching.manage.TeachingUseCaseException;
import ${package}.application.teaching.query.GetCourseQuery;
import ${package}.application.teaching.result.CourseResult;
import ${package}.facade.teaching.CourseFacade;
import ${package}.facade.teaching.dto.CourseDTO;
import ${package}.facade.teaching.dto.CreateCourseDTO;
import ${package}.facade.teaching.exceptions.TeachingFacadeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("courseFacadeImpl")
@RequiredArgsConstructor
public class CourseFacadeImpl implements CourseFacade {
    private final CourseManage courseManage;

    @Override
    public CourseDTO createCourse(CreateCourseDTO request) {
        try {
            return toDto(courseManage.create(new CreateCourseCommand(
                    request.code(), request.name(), request.operatorId(), request.requestId())));
        } catch (TeachingUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    @Override
    public CourseDTO getCourse(String courseId) {
        try {
            return toDto(courseManage.get(new GetCourseQuery(courseId)));
        } catch (TeachingUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    private static CourseDTO toDto(CourseResult result) {
        return new CourseDTO(result.id(), result.code(), result.name(), result.status());
    }

    private static TeachingFacadeException publicFailure(TeachingUseCaseException exception) {
        return new TeachingFacadeException(exception.getCode(), exception.getMessage());
    }
}
