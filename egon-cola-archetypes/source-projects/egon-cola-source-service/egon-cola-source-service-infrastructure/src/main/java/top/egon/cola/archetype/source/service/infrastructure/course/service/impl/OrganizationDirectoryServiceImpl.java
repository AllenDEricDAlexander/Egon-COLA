package top.egon.cola.archetype.source.service.infrastructure.course.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.service.domain.course.service.OrganizationDirectoryService;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationSchoolClassBO;
import top.egon.cola.archetype.source.service.domain.course.vos.OrganizationUserBO;
import top.egon.cola.archetype.source.service.infrastructure.client.organization.OrganizationDirectoryClient;

/** Domain directory capability served by the named infrastructure client. */
@Validated
@Service("organizationDirectoryService")
@RequiredArgsConstructor
@Slf4j
public class OrganizationDirectoryServiceImpl implements OrganizationDirectoryService {
    @Qualifier("organizationDirectoryClient")
    private final OrganizationDirectoryClient organizationDirectoryClient;

    @Override
    public OrganizationUserBO getUser(Long userId) {
        return organizationDirectoryClient.getUser(userId);
    }

    @Override
    public OrganizationSchoolClassBO getSchoolClass(Long gradeId, Long schoolClassId) {
        return organizationDirectoryClient.getSchoolClass(gradeId, schoolClassId);
    }
}
