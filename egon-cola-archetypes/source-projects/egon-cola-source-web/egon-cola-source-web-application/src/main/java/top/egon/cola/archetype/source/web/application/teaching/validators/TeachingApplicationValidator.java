package top.egon.cola.archetype.source.web.application.teaching.validators;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationFailureType;

public final class TeachingApplicationValidator {
    public void requireTeachingAdmin() {
        if (OrganizationRequestContextHolder.current()
                .filter(context -> context.hasRole("TEACHING_ADMIN"))
                .isEmpty()) {
            throw new OrganizationApplicationException(
                OrganizationFailureType.FORBIDDEN, "ORG_FORBIDDEN", "TEACHING_ADMIN is required");
        }
    }
}
