package ${package}.application.user.validators;

import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.domain.user.validators.UserDomainValidator;

public final class UserApplicationValidator {

    public void requireOrganizationAdmin() {
        if (OrganizationRequestContextHolder.current()
                .filter(context -> context.hasRole("ORGANIZATION_ADMIN"))
                .isEmpty()) {
            throw new OrganizationApplicationException(
                OrganizationFailureType.FORBIDDEN, "ORG_FORBIDDEN", "ORGANIZATION_ADMIN is required");
        }
    }

    public String normalizedEmail(String email) { return UserDomainValidator.normalizeEmail(email); }
}
