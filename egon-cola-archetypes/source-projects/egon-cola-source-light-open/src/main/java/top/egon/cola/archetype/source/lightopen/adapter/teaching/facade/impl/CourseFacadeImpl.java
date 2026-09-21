package top.egon.cola.archetype.source.lightopen.adapter.teaching.facade.impl;

import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.lightopen.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.lightopen.facade.teaching.CourseFacade;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CourseDTO;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.archetype.source.lightopen.common.exception.TeachingFacadeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;

@Component("courseFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class CourseFacadeImpl implements CourseFacade {
    @Qualifier("courseManageImpl")
    private final CourseManage courseManage;
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    public CourseDTO createCourse(CreateCourseDTO request) {
        validationUtils.validate(request);
        try {
            return toDto(courseManage.create(new CreateCourseCommand(
                    request.code(), request.name(), request.operatorId(), request.requestId())));
        } catch (TeachingUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    @Override
    public CourseDTO getCourse(Long courseId) {
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
        return new TeachingFacadeException(exception.getStatus(), exception.getMessage());
    }
}
