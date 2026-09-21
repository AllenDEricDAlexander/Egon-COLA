package top.egon.cola.archetype.source.serviceopen.infrastructure.client.organization.impl;

import top.egon.cola.archetype.source.serviceopen.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.OrganizationSchoolClassBO;
import top.egon.cola.archetype.source.serviceopen.infrastructure.client.organization.OrganizationDirectoryClient;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.OrganizationUserBO;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.GetSchoolClassRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.GetUserRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.SchoolClass;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.SchoolClassService;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.User;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.UserService;
import java.util.List;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("organizationDirectoryClient")
@Profile({"dev", "prod"})
@Slf4j
public class DubboOrganizationDirectoryClientImpl implements OrganizationDirectoryClient {

    @DubboReference(
            group = "${app.integrations.organization.group:student-management-organization}",
            version = "${app.integrations.organization.version:1.0.0}",
            retries = 0,
            check = true)
    private UserService userService;

    @DubboReference(
            group = "${app.integrations.organization.group:student-management-organization}",
            version = "${app.integrations.organization.version:1.0.0}",
            retries = 0,
            check = true)
    private SchoolClassService schoolClassService;

    public DubboOrganizationDirectoryClientImpl() {
    }

    DubboOrganizationDirectoryClientImpl(UserService userService, SchoolClassService schoolClassService) {
        this.userService = userService;
        this.schoolClassService = schoolClassService;
    }

    @Override
    public OrganizationUserBO getUser(Long userId) {
        try {
            User response = userService.getUser(GetUserRequest.newBuilder().setUserId(userId).build());
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getUser");
            }
            return new OrganizationUserBO(response.getId(), response.getName(), response.getStatus());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw OrganizationClientFailureMapper.map(failure);
        }
    }

    @Override
    public OrganizationSchoolClassBO getSchoolClass(Long gradeId, Long schoolClassId) {
        try {
            SchoolClass response = schoolClassService.getSchoolClass(GetSchoolClassRequest.newBuilder()
                    .setGradeId(gradeId)
                    .setSchoolClassId(schoolClassId)
                    .build());
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getSchoolClass");
            }
            List<Long> userIds = response.getUserIdsList().stream().map(Long::valueOf).toList();
            return new OrganizationSchoolClassBO(
                    response.getId(), response.getName(), response.getGradeCode(),
                    response.getStatus(), userIds);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw OrganizationClientFailureMapper.map(failure);
        }
    }
}
