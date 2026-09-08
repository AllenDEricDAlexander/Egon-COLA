package top.egon.cola.archetype.source.light.adapter.teaching.rpc;

import top.egon.cola.archetype.source.light.facade.teaching.CourseFacade;
import top.egon.cola.archetype.source.light.facade.teaching.exceptions.TeachingFacadeException;
import top.egon.cola.archetype.source.light.facade.rpc.CourseRpcService;
import top.egon.cola.archetype.source.light.facade.rpc.LightRpcConverter;
import top.egon.cola.archetype.source.light.facade.rpc.NativeRpcValidationGroup;
import top.egon.cola.archetype.source.light.facade.rpc.RpcIdQuery;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Objects;
import top.egon.cola.archetype.source.light.facade.rpc.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.light.facade.rpc.proto.GetCourseRpcRequest;

/** Native unary adapter for the existing Course facade. */
@EgonRpcProvider
@Component("courseRpcProvider")
@RequiredArgsConstructor
@Slf4j
public class CourseRpcProvider implements CourseRpcService {
    @Qualifier("courseFacadeImpl")
    private final CourseFacade delegate;
    @Qualifier("lightRpcConverter")
    private final LightRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public CourseRpcResponse createCourse(CreateCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.courseSuccess(Objects.requireNonNull(delegate.createCourse(input), "facade returned null"));
        } catch (TeachingFacadeException exception) {
            log.debug("createCourse rejected: {}", exception.getCode());
            return converter.courseFailure(exception.getCode(), exception.getMessage(), null);
        }
    }

    @Override
    public CourseRpcResponse getCourse(GetCourseRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasCourseId() ? request.getCourseId() : null));
        try {
            return converter.courseSuccess(Objects.requireNonNull(delegate.getCourse(input.id()), "facade returned null"));
        } catch (TeachingFacadeException exception) {
            log.debug("getCourse rejected: {}", exception.getCode());
            return converter.courseFailure(exception.getCode(), exception.getMessage(), null);
        }
    }
}
