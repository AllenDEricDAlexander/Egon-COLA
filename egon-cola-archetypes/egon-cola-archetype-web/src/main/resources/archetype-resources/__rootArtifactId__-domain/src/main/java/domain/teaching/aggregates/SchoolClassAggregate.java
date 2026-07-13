package ${package}.domain.teaching.aggregates;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.user.entities.User;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

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
