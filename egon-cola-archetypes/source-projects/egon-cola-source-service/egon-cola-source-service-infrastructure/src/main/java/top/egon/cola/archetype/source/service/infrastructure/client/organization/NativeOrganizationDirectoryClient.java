package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.organization.facade.rpc.OrganizationRpcConverter;
import top.egon.cola.organization.facade.rpc.RpcIdQuery;
import top.egon.cola.organization.facade.rpc.RpcSchoolClassQuery;
import top.egon.cola.organization.facade.rpc.UserRpcService;
import top.egon.cola.organization.facade.rpc.SchoolClassRpcService;
import top.egon.cola.organization.facade.exceptions.OrganizationFacadeException;
import top.egon.cola.archetype.source.service.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationDirectoryPort;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationUser;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationSchoolClass;

/** Native DIRECT adapter for the existing organization directory port. */
@Component("nativeOrganizationDirectoryClient")
@Profile({"dev", "prod"})
@RequiredArgsConstructor
@Slf4j
public class NativeOrganizationDirectoryClient implements OrganizationDirectoryPort {
    @Qualifier("organizationUserRpcService")
    private final UserRpcService userService;
    @Qualifier("organizationSchoolClassRpcService")
    private final SchoolClassRpcService schoolClassService;
    @Qualifier("organizationRpcConverter")
    private final OrganizationRpcConverter converter;
    @Qualifier("organizationDirectoryConverter")
    private final OrganizationDirectoryConverter directoryConverter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public OrganizationUser getUser(Long userId) {
        var query = validation.validate(new RpcIdQuery(userId));
        try {
            var response = userService.getUser(directoryConverter.userRequest(query));
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getUser");
            }
            if (!response.getSuccess()) {
                throw new OrganizationFacadeException(response.hasCode() ? response.getCode() : null,
                        response.hasMessage() ? response.getMessage() : null,
                        response.hasTraceId() ? response.getTraceId() : null);
            }
            if (!response.hasData()) {
                throw OrganizationClientFailureMapper.incompatible("getUser");
            }
            var data = converter.toSource(response.getData());
            if (!validation.isValid(data)) {
                throw OrganizationClientFailureMapper.incompatible("getUser");
            }
            return directoryConverter.toTarget(data);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            var mapped = OrganizationClientFailureMapper.map(failure);
            log.debug("getUser dependency failure: {}", mapped.failure());
            throw mapped;
        }
    }

    @Override
    public OrganizationSchoolClass getSchoolClass(Long gradeId, Long schoolClassId) {
        var query = validation.validate(new RpcSchoolClassQuery(gradeId, schoolClassId));
        try {
            var response = schoolClassService.getSchoolClass(directoryConverter.schoolClassRequest(query));
            if (response == null) {
                throw OrganizationClientFailureMapper.incompatible("getSchoolClass");
            }
            if (!response.getSuccess()) {
                throw new OrganizationFacadeException(response.hasCode() ? response.getCode() : null,
                        response.hasMessage() ? response.getMessage() : null,
                        response.hasTraceId() ? response.getTraceId() : null);
            }
            if (!response.hasData()) {
                throw OrganizationClientFailureMapper.incompatible("getSchoolClass");
            }
            var data = converter.toSource(response.getData());
            if (!validation.isValid(data)) {
                throw OrganizationClientFailureMapper.incompatible("getSchoolClass");
            }
            return directoryConverter.toTarget(data);
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            var mapped = OrganizationClientFailureMapper.map(failure);
            log.debug("getSchoolClass dependency failure: {}", mapped.failure());
            throw mapped;
        }
    }
}
