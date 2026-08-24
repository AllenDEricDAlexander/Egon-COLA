#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.teaching.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.teaching.command.CreateGradeCommand;
import ${package}.application.teaching.manage.GradeManage;
import ${package}.application.teaching.query.GradeDetailQuery;
import ${package}.application.teaching.result.GradeDetailResult;
import ${package}.facade.organization.v1.CreateGradeRequest;
import ${package}.facade.organization.v1.DubboGradeServiceTriple;
import ${package}.facade.organization.v1.GetGradeRequest;
import ${package}.facade.organization.v1.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GradeFacadeImpl extends DubboGradeServiceTriple.GradeServiceImplBase {

    private final GradeManage gradeManage;
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
