package top.egon.cola.archetype.source.webopen.adapter.teaching.facade.impl;

import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.query.GradeDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.result.GradeDetailResult;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.CreateGradeRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.DubboGradeServiceTriple;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.GetGradeRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.Grade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Dubbo Triple provider of the organization Grade contract; maps Protobuf onto the use cases. */
@Service("gradeFacade")
@RequiredArgsConstructor
@Slf4j
public class GradeFacadeImpl extends DubboGradeServiceTriple.GradeServiceImplBase {

    @Qualifier("gradeManage")
    private final GradeManage gradeManage;
    @Qualifier("organizationFacadeSupport")
    private final OrganizationFacadeSupport support;

    @Override
    public Grade createGrade(CreateGradeRequest request) {
        return support.invoke(() -> {
            if (request == null) {
                throw new IllegalArgumentException("createGrade request must not be null");
            }
            return toProto(gradeManage.createGrade(new CreateGradeCommand(
                    support.requestId(), request.getCode(), request.getName())));
        });
    }

    @Override
    public Grade getGrade(GetGradeRequest request) {
        return support.invoke(() -> {
            if (request == null) {
                throw new IllegalArgumentException("getGrade request must not be null");
            }
            return toProto(gradeManage.getGrade(new GradeDetailQuery(
                    OrganizationFacadeSupport.positiveId(request.getGradeId(), "gradeId"))));
        });
    }

    private static Grade toProto(GradeDetailResult result) {
        return Grade.newBuilder()
                .setId(result.id())
                .setCode(result.code())
                .setName(result.name())
                .setStatus(result.status())
                .build();
    }
}
