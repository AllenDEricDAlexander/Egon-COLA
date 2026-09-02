package top.egon.cola.archetype.source.service.domain.client.organization;

public interface OrganizationDirectoryPort {

    OrganizationUser getUser(Long userId);

    OrganizationSchoolClass getSchoolClass(Long gradeId, Long schoolClassId);
}
