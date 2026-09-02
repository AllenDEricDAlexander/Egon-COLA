package top.egon.cola.archetype.source.service.domain.client.organization;

import java.util.List;

public record OrganizationSchoolClass(
        Long id,
        String name,
        String gradeCode,
        String status,
        List<Long> userIds) {

    public OrganizationSchoolClass {
        userIds = List.copyOf(userIds);
    }
}
