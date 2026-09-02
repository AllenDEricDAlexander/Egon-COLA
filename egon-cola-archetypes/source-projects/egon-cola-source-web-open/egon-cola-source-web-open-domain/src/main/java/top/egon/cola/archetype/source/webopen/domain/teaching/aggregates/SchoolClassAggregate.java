package top.egon.cola.archetype.source.webopen.domain.teaching.aggregates;

import top.egon.cola.archetype.source.webopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.webopen.domain.exceptions.OrganizationDomainException;

public final class SchoolClassAggregate {
    private final SchoolClass schoolClass;

    public SchoolClassAggregate(SchoolClass schoolClass) { this.schoolClass = schoolClass; }

    public void validateAssignment(User user) {
        if (user.status() == UserStatus.DISABLED) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.USER_DISABLED, "disabled user cannot join a school class");
        }
        if (schoolClass.status() == SchoolClassStatus.ARCHIVED) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.DOMAIN_REJECTED, "archived school class cannot receive users");
        }
    }

    public SchoolClass schoolClass() { return schoolClass; }
}
