package top.egon.cola.archetype.source.web.adapter.teaching.rpc;

import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.organization.facade.rpc.OrganizationRpcConverter;
import top.egon.cola.organization.facade.rpc.RpcIdQuery;
import top.egon.cola.organization.facade.rpc.RpcSchoolClassQuery;
import top.egon.cola.organization.facade.exceptions.OrganizationFacadeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Objects;
import top.egon.cola.organization.facade.rpc.GradeRpcService;
import top.egon.cola.organization.facade.teaching.GradeFacade;
import top.egon.cola.organization.facade.rpc.SchoolClassRpcService;
import top.egon.cola.organization.facade.teaching.SchoolClassFacade;
import top.egon.cola.organization.facade.rpc.proto.CreateGradeRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GradeRpcResponse;
import top.egon.cola.organization.facade.rpc.proto.GetGradeRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.SchoolClassRpcResponse;
import top.egon.cola.organization.facade.rpc.proto.GetSchoolClassRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.AssignUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.RpcResponse;

/** Preserves organization facade behavior behind the native unary contracts. */
@Component("schoolClassRpcProvider")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class SchoolClassRpcProvider implements GradeRpcService, SchoolClassRpcService {
    @Qualifier("gradeFacade")
    private final GradeFacade gradeFacade;
    @Qualifier("schoolClassFacade")
    private final SchoolClassFacade schoolClassFacade;
    @Qualifier("organizationRpcConverter")
    private final OrganizationRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public GradeRpcResponse createGrade(CreateGradeRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.gradeSuccess(Objects.requireNonNull(
                    gradeFacade.createGrade(input), "facade returned null"));
        } catch (OrganizationFacadeException exception) {
            log.debug("createGrade rejected: {}", exception.code());
            return converter.gradeFailure(exception.code(), exception.getMessage(), exception.traceId());
        }
    }

    @Override
    public GradeRpcResponse getGrade(GetGradeRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasGradeId() ? request.getGradeId() : null));
        try {
            return converter.gradeSuccess(Objects.requireNonNull(
                    gradeFacade.getGrade(input.id()), "facade returned null"));
        } catch (OrganizationFacadeException exception) {
            log.debug("getGrade rejected: {}", exception.code());
            return converter.gradeFailure(exception.code(), exception.getMessage(), exception.traceId());
        }
    }

    @Override
    public SchoolClassRpcResponse createSchoolClass(CreateSchoolClassRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.schoolClassSuccess(Objects.requireNonNull(
                    schoolClassFacade.createSchoolClass(input), "facade returned null"));
        } catch (OrganizationFacadeException exception) {
            log.debug("createSchoolClass rejected: {}", exception.code());
            return converter.schoolClassFailure(exception.code(), exception.getMessage(), exception.traceId());
        }
    }

    @Override
    public SchoolClassRpcResponse getSchoolClass(GetSchoolClassRpcRequest request) {
        var input = validation.validate(new RpcSchoolClassQuery(
                request.hasGradeId() ? request.getGradeId() : null,
                request.hasSchoolClassId() ? request.getSchoolClassId() : null));
        try {
            return converter.schoolClassSuccess(Objects.requireNonNull(
                    schoolClassFacade.getSchoolClass(input.gradeId(), input.schoolClassId()), "facade returned null"));
        } catch (OrganizationFacadeException exception) {
            log.debug("getSchoolClass rejected: {}", exception.code());
            return converter.schoolClassFailure(exception.code(), exception.getMessage(), exception.traceId());
        }
    }

    @Override
    public RpcResponse assignUser(AssignUserRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            schoolClassFacade.assignUser(input);
            return converter.response(true, "SUCCESS", "success", null);
        } catch (OrganizationFacadeException exception) {
            log.debug("assignUser rejected: {}", exception.code());
            return converter.response(false, exception.code(), exception.getMessage(), exception.traceId());
        }
    }
}
