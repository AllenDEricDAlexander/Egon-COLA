#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.teaching.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.SchoolClassDetailQuery;
import ${package}.application.teaching.result.SchoolClassDetailResult;
import ${package}.facade.organization.v1.AssignUserRequest;
import ${package}.facade.organization.v1.CreateSchoolClassRequest;
import ${package}.facade.organization.v1.DubboSchoolClassServiceTriple;
import ${package}.facade.organization.v1.GetSchoolClassRequest;
import ${package}.facade.organization.v1.SchoolClass;
import ${package}.facade.shared.v1.Empty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SchoolClassFacadeImpl extends DubboSchoolClassServiceTriple.SchoolClassServiceImplBase {

    private final SchoolClassManage schoolClassManage;
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
