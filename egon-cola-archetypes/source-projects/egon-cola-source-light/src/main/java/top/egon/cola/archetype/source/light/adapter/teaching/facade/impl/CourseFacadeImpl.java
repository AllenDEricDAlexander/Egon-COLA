package top.egon.cola.archetype.source.light.adapter.teaching.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.pojo.dto.RpcIdQuery;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.common.exception.TeachingFacadeException;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.teaching.CourseFacade;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.archetype.source.light.facade.teaching.utils.TeachingFacadeAssert;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

/** Native unary provider of the Course facade; maps Protobuf onto the teaching use cases. */
@EgonRpcProvider
@Component("courseFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class CourseFacadeImpl implements CourseFacade {

    private static final String EMPTY_RESULT = "FACADE_EMPTY_RESULT";

    @Qualifier("courseManageImpl")
    private final CourseManage courseManage;
    @Qualifier("lightFacadeConverter")
    private final LightFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public CourseRpcResponse createCourse(CreateCourseRpcRequest request) {
        CreateCourseDTO input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.courseSuccess(create(input));
        } catch (TeachingFacadeException exception) {
            log.debug("createCourse rejected: {}", exception.getStatus());
            return converter.courseFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    @Override
    public CourseRpcResponse getCourse(GetCourseRpcRequest request) {
        RpcIdQuery input = validation.validate(new RpcIdQuery(request.hasCourseId() ? request.getCourseId() : null));
        try {
            return converter.courseSuccess(get(input.id()));
        } catch (TeachingFacadeException exception) {
            log.debug("getCourse rejected: {}", exception.getStatus());
            return converter.courseFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    private CourseDTO create(CreateCourseDTO request) {
        try {
            CourseResult result = courseManage.create(new CreateCourseCommand(
                    request.code(), request.name(), request.operatorId(), request.requestId()));
            return toDto(TeachingFacadeAssert.notNull(result, EMPTY_RESULT, "facade returned null"));
        } catch (TeachingUseCaseException exception) {
            throw new TeachingFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private CourseDTO get(Long courseId) {
        try {
            CourseResult result = courseManage.get(new GetCourseQuery(courseId));
            return toDto(TeachingFacadeAssert.notNull(result, EMPTY_RESULT, "facade returned null"));
        } catch (TeachingUseCaseException exception) {
            throw new TeachingFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private static CourseDTO toDto(CourseResult result) {
        return new CourseDTO(result.id(), result.code(), result.name(), result.status());
    }
}
