package ${package}.application.teaching.validators;

import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;

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
