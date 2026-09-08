package top.egon.cola.archetype.source.light.adapter.teaching.rpc;

import top.egon.cola.archetype.source.light.facade.teaching.SchoolClassFacade;
import top.egon.cola.archetype.source.light.facade.teaching.exceptions.TeachingFacadeException;
import top.egon.cola.archetype.source.light.facade.rpc.SchoolClassRpcService;
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
import top.egon.cola.archetype.source.light.facade.rpc.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.SchoolClassRpcResponse;
import top.egon.cola.archetype.source.light.facade.rpc.proto.ScheduleCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.GetSchoolClassRpcRequest;

/** Native unary adapter for the existing SchoolClass facade. */
@EgonRpcProvider
@Component("schoolClassRpcProvider")
@RequiredArgsConstructor
@Slf4j
public class SchoolClassRpcProvider implements SchoolClassRpcService {
    @Qualifier("schoolClassFacadeImpl")
    private final SchoolClassFacade delegate;
    @Qualifier("lightRpcConverter")
    private final LightRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public SchoolClassRpcResponse createSchoolClass(CreateSchoolClassRpcRequest request) {
        var input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.schoolClassSuccess(Objects.requireNonNull(delegate.createSchoolClass(input), "facade returned null"));
        } catch (TeachingFacadeException exception) {
            log.debug("createSchoolClass rejected: {}", exception.getCode());
            return converter.schoolClassFailure(exception.getCode(), exception.getMessage(), null);
        }
    }

    @Override
    public SchoolClassRpcResponse scheduleCourse(ScheduleCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.schoolClassSuccess(Objects.requireNonNull(delegate.scheduleCourse(input), "facade returned null"));
        } catch (TeachingFacadeException exception) {
            log.debug("scheduleCourse rejected: {}", exception.getCode());
            return converter.schoolClassFailure(exception.getCode(), exception.getMessage(), null);
        }
    }

    @Override
    public SchoolClassRpcResponse getSchoolClass(GetSchoolClassRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasSchoolClassId() ? request.getSchoolClassId() : null));
        try {
            return converter.schoolClassSuccess(Objects.requireNonNull(delegate.getSchoolClass(input.id()), "facade returned null"));
        } catch (TeachingFacadeException exception) {
            log.debug("getSchoolClass rejected: {}", exception.getCode());
            return converter.schoolClassFailure(exception.getCode(), exception.getMessage(), null);
        }
    }
}
