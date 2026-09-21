package top.egon.cola.archetype.source.web.adapter.teaching.facade.impl;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.application.teaching.pojo.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.web.facade.proto.AssignUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.RpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.SchoolClassRpcResponse;
import top.egon.cola.archetype.source.web.facade.teaching.SchoolClassFacade;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

/** Native unary provider of the web-owned SchoolClass facade; maps Protobuf onto the use cases. */
@Service("schoolClassFacade")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class SchoolClassFacadeImpl implements SchoolClassFacade {

    @Qualifier("schoolClassManage")
    private final SchoolClassManage schoolClassManage;
    @Qualifier("organizationFacadeConverter")
    private final OrganizationFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    /** Validates the pair of scalar identifiers after Protobuf presence has been decoded. */
    record SchoolClassQuery(@NotNull @Positive Long gradeId, @NotNull @Positive Long schoolClassId) {
    }

    @Override
    public SchoolClassRpcResponse createSchoolClass(CreateSchoolClassRpcRequest request) {
        var command = validation.validate(
                converter.createSchoolClassCommand(request, OrganizationFacadeSupport.requestId()));
        return OrganizationFacadeSupport.invoke(
                () -> converter.schoolClassSuccess(require(schoolClassManage.createSchoolClass(command))),
                (code, message, traceId) -> reject("createSchoolClass", code, message, traceId));
    }

    @Override
    public SchoolClassRpcResponse getSchoolClass(GetSchoolClassRpcRequest request) {
        var input = validation.validate(new SchoolClassQuery(
                request.hasGradeId() ? request.getGradeId() : null,
                request.hasSchoolClassId() ? request.getSchoolClassId() : null));
        return OrganizationFacadeSupport.invoke(
                () -> converter.schoolClassSuccess(require(schoolClassManage.getSchoolClass(
                        new SchoolClassDetailQuery(input.gradeId(), input.schoolClassId())))),
                (code, message, traceId) -> reject("getSchoolClass", code, message, traceId));
    }

    @Override
    public RpcResponse assignUser(AssignUserRpcRequest request) {
        var command = validation.validate(
                converter.assignUserToClassCommand(request, OrganizationFacadeSupport.requestId()));
        return OrganizationFacadeSupport.invoke(
                () -> {
                    schoolClassManage.assignUser(command);
                    return converter.response(true, "SUCCESS", "success", null);
                },
                (code, message, traceId) -> rejectAck("assignUser", code, message, traceId));
    }

    private static SchoolClassDetailResult require(SchoolClassDetailResult result) {
        return Objects.requireNonNull(result, "facade returned null");
    }

    private SchoolClassRpcResponse reject(String operation, String code, String message, String traceId) {
        log.debug("{} rejected: {}", operation, code);
        return converter.schoolClassFailure(code, message, traceId);
    }

    private RpcResponse rejectAck(String operation, String code, String message, String traceId) {
        log.debug("{} rejected: {}", operation, code);
        return converter.response(false, code, message, traceId);
    }
}
