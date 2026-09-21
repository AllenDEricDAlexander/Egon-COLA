package top.egon.cola.archetype.source.service.domain.course.vos;

import java.util.List;

public record OrganizationSchoolClassBO(
        Long id,
        String name,
        String gradeCode,
        String status,
        List<Long> userIds) {

    public OrganizationSchoolClassBO {
        userIds = List.copyOf(userIds);
    }
}
