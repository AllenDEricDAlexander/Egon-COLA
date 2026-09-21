package top.egon.cola.archetype.source.web.adapter.teaching.facade.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter;
import top.egon.cola.archetype.source.web.adapter.pojo.dto.RpcIdQuery;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.pojo.query.GradeDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.GradeDetailResult;
import top.egon.cola.archetype.source.web.facade.proto.CreateGradeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetGradeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GradeRpcResponse;
import top.egon.cola.archetype.source.web.facade.teaching.GradeFacade;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

/** Native unary provider of the web-owned Grade facade; maps Protobuf onto the use cases. */
@Service("gradeFacade")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class GradeFacadeImpl implements GradeFacade {

    @Qualifier("gradeManage")
    private final GradeManage gradeManage;
    @Qualifier("organizationFacadeConverter")
    private final OrganizationFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public GradeRpcResponse createGrade(CreateGradeRpcRequest request) {
        var command = validation.validate(
                converter.createGradeCommand(request, OrganizationFacadeSupport.requestId()));
        return OrganizationFacadeSupport.invoke(
                () -> converter.gradeSuccess(require(gradeManage.createGrade(command))),
                (code, message, traceId) -> reject("createGrade", code, message, traceId));
    }

    @Override
    public GradeRpcResponse getGrade(GetGradeRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasGradeId() ? request.getGradeId() : null));
        return OrganizationFacadeSupport.invoke(
                () -> converter.gradeSuccess(require(gradeManage.getGrade(new GradeDetailQuery(input.id())))),
                (code, message, traceId) -> reject("getGrade", code, message, traceId));
    }

    private static GradeDetailResult require(GradeDetailResult result) {
        return Objects.requireNonNull(result, "facade returned null");
    }

    private GradeRpcResponse reject(String operation, String code, String message, String traceId) {
        log.debug("{} rejected: {}", operation, code);
        return converter.gradeFailure(code, message, traceId);
    }
}
