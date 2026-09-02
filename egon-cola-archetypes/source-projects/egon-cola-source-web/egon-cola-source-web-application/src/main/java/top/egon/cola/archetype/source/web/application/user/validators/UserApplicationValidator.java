package top.egon.cola.archetype.source.web.application.user.validators;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationFailureType;
import top.egon.cola.archetype.source.web.domain.user.validators.UserDomainValidator;

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
