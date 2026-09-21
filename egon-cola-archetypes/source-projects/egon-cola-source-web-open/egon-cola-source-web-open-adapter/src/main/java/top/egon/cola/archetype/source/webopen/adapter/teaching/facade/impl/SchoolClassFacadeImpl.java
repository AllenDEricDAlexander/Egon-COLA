package top.egon.cola.archetype.source.webopen.adapter.teaching.facade.impl;

import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.AssignUserRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.CreateSchoolClassRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.DubboSchoolClassServiceTriple;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.GetSchoolClassRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.SchoolClass;
import top.egon.cola.archetype.source.webopen.facade.shared.v1.Empty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Dubbo Triple provider of the organization SchoolClass contract; maps Protobuf onto the use cases. */
@Service("schoolClassFacade")
@RequiredArgsConstructor
@Slf4j
public class SchoolClassFacadeImpl extends DubboSchoolClassServiceTriple.SchoolClassServiceImplBase {

    @Qualifier("schoolClassManage")
    private final SchoolClassManage schoolClassManage;
    @Qualifier("organizationFacadeSupport")
    private final OrganizationFacadeSupport support;

    @Override
    public SchoolClass createSchoolClass(CreateSchoolClassRequest request) {
        return support.invoke(() -> {
            if (request == null) {
                throw new IllegalArgumentException("createSchoolClass request must not be null");
            }
            return toProto(schoolClassManage.createSchoolClass(new CreateSchoolClassCommand(
                    support.requestId(), request.getName(), request.getGradeCode())));
        });
    }

    @Override
    public SchoolClass getSchoolClass(GetSchoolClassRequest request) {
        return support.invoke(() -> {
            if (request == null) {
                throw new IllegalArgumentException("getSchoolClass request must not be null");
            }
            return toProto(schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(
                    OrganizationFacadeSupport.positiveId(request.getGradeId(), "gradeId"),
                    OrganizationFacadeSupport.positiveId(request.getSchoolClassId(), "schoolClassId"))));
        });
    }

    @Override
    public Empty assignUser(AssignUserRequest request) {
        return support.invoke(() -> {
            if (request == null) {
                throw new IllegalArgumentException("assignUser request must not be null");
            }
            schoolClassManage.assignUser(new AssignUserToClassCommand(
                    support.requestId(),
                    OrganizationFacadeSupport.positiveId(request.getGradeId(), "gradeId"),
                    OrganizationFacadeSupport.positiveId(request.getSchoolClassId(), "schoolClassId"),
                    OrganizationFacadeSupport.positiveId(request.getUserId(), "userId")));
            return Empty.getDefaultInstance();
        });
    }

    private static SchoolClass toProto(SchoolClassDetailResult result) {
        return SchoolClass.newBuilder()
                .setId(result.id())
                .setName(result.name())
                .setGradeCode(result.gradeCode())
                .setGradeName(result.gradeName())
                .setStatus(result.status())
                .addAllUserIds(result.userIds())
                .build();
    }
}
