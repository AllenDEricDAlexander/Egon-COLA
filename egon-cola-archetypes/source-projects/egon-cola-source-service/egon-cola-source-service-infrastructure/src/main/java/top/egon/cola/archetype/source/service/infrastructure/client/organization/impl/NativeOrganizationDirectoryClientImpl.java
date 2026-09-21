package top.egon.cola.archetype.source.service.infrastructure.client.organization.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationSchoolClassBO;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationUserBO;
import top.egon.cola.archetype.source.service.infrastructure.client.organization.OrganizationDirectoryClient;
import top.egon.cola.archetype.source.service.infrastructure.client.organization.OrganizationDirectoryConverter;
import top.egon.cola.archetype.source.web.facade.teaching.SchoolClassFacade;
import top.egon.cola.archetype.source.web.facade.user.UserFacade;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Native DIRECT adapter for the outbound organization directory client. */
@Component("organizationDirectoryClient")
@Profile({"dev", "prod"})
@RequiredArgsConstructor
@Slf4j
public class NativeOrganizationDirectoryClientImpl implements OrganizationDirectoryClient {
    @Qualifier("organizationUserFacade")
    private final UserFacade userFacade;
    @Qualifier("organizationSchoolClassFacade")
    private final SchoolClassFacade schoolClassFacade;
    @Qualifier("organizationDirectoryConverter")
    private final OrganizationDirectoryConverter directories;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public OrganizationUserBO getUser(Long userId) {
        var query = validation.validate(new OrganizationDirectoryConverter.UserQuery(userId));
        try {
            var response = userFacade.getUser(directories.userRequest(query));
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getUser");
            }
            if (!response.getSuccess()) {
                throw OrganizationClientFailureMapper.rejected(
                        response.hasCode() ? response.getCode() : null);
            }
            var data = response.getData();
            if (!response.hasData() || !data.hasId() || data.getId() <= 0L) {
                throw OrganizationClientFailureMapper.incompatible("getUser");
            }
            return directories.toTarget(data);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            var mapped = OrganizationClientFailureMapper.map(failure);
            log.debug("getUser dependency failure: {}", mapped.failure());
            throw mapped;
        }
    }

    @Override
    public OrganizationSchoolClassBO getSchoolClass(Long gradeId, Long schoolClassId) {
        var query = validation.validate(new OrganizationDirectoryConverter.SchoolClassQuery(gradeId, schoolClassId));
        try {
            var response = schoolClassFacade.getSchoolClass(directories.schoolClassRequest(query));
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getSchoolClass");
            }
            if (!response.getSuccess()) {
                throw OrganizationClientFailureMapper.rejected(
                        response.hasCode() ? response.getCode() : null);
            }
            var data = response.getData();
            if (!response.hasData() || !data.hasId() || data.getId() <= 0L
                    || data.getUserIdsList().stream().anyMatch(id -> id <= 0L)) {
                throw OrganizationClientFailureMapper.incompatible("getSchoolClass");
            }
            return directories.toTarget(data);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            var mapped = OrganizationClientFailureMapper.map(failure);
            log.debug("getSchoolClass dependency failure: {}", mapped.failure());
            throw mapped;
        }
    }
}
