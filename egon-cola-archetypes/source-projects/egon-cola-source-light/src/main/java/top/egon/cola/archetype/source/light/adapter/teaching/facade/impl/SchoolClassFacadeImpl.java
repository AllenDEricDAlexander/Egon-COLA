package top.egon.cola.archetype.source.light.adapter.teaching.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.pojo.dto.RpcIdQuery;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.light.common.exception.TeachingFacadeException;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.facade.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetSchoolClassRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.SchoolClassRpcResponse;
import top.egon.cola.archetype.source.light.facade.teaching.SchoolClassFacade;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.ScheduleCourseDTO;
import top.egon.cola.archetype.source.light.facade.teaching.dto.SchoolClassDetailDTO;
import top.egon.cola.archetype.source.light.facade.teaching.utils.TeachingFacadeAssert;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

/** Native unary provider of the SchoolClass facade; maps Protobuf onto the teaching use cases. */
@EgonRpcProvider
@Component("schoolClassFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class SchoolClassFacadeImpl implements SchoolClassFacade {

    private static final String EMPTY_RESULT = "FACADE_EMPTY_RESULT";

    @Qualifier("schoolClassManageImpl")
    private final SchoolClassManage schoolClassManage;
    @Qualifier("lightFacadeConverter")
    private final LightFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public SchoolClassRpcResponse createSchoolClass(CreateSchoolClassRpcRequest request) {
        CreateSchoolClassDTO input = validation.validate(
                converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.schoolClassSuccess(create(input));
        } catch (TeachingFacadeException exception) {
            log.debug("createSchoolClass rejected: {}", exception.getStatus());
            return converter.schoolClassFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    @Override
    public SchoolClassRpcResponse scheduleCourse(ScheduleCourseRpcRequest request) {
        ScheduleCourseDTO input = validation.validate(
                converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.schoolClassSuccess(schedule(input));
        } catch (TeachingFacadeException exception) {
            log.debug("scheduleCourse rejected: {}", exception.getStatus());
            return converter.schoolClassFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    @Override
    public SchoolClassRpcResponse getSchoolClass(GetSchoolClassRpcRequest request) {
        RpcIdQuery input = validation.validate(
                new RpcIdQuery(request.hasSchoolClassId() ? request.getSchoolClassId() : null));
        try {
            return converter.schoolClassSuccess(get(input.id()));
        } catch (TeachingFacadeException exception) {
            log.debug("getSchoolClass rejected: {}", exception.getStatus());
            return converter.schoolClassFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    private SchoolClassDetailDTO create(CreateSchoolClassDTO request) {
        try {
            return toDto(requireResult(schoolClassManage.create(new CreateSchoolClassCommand(
                    request.name(), request.semester(), request.operatorId(), request.requestId()))));
        } catch (TeachingUseCaseException exception) {
            throw new TeachingFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private SchoolClassDetailDTO schedule(ScheduleCourseDTO request) {
        try {
            return toDto(requireResult(schoolClassManage.schedule(new ScheduleCourseCommand(
                    request.schoolClassId(), request.courseId(), request.startsAt(), request.endsAt(),
                    request.operatorId(), request.requestId()))));
        } catch (TeachingUseCaseException exception) {
            throw new TeachingFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private SchoolClassDetailDTO get(Long schoolClassId) {
        try {
            return toDto(requireResult(schoolClassManage.get(new GetSchoolClassQuery(schoolClassId))));
        } catch (TeachingUseCaseException exception) {
            throw new TeachingFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private static SchoolClassResult requireResult(SchoolClassResult result) {
        return TeachingFacadeAssert.notNull(result, EMPTY_RESULT, "facade returned null");
    }

    private static SchoolClassDetailDTO toDto(SchoolClassResult result) {
        return new SchoolClassDetailDTO(
                result.id(), result.name(), result.semester(), result.status(), result.scheduleCount());
    }
}
