package ${package}.application.context;

import java.util.Set;

public record OrganizationRequestContext(String actorId, Set<String> actorRoles, String traceId) {

    public OrganizationRequestContext {
        actorRoles = Set.copyOf(actorRoles);
    }

    public boolean hasRole(String role) {
        return actorRoles.contains(role) || actorRoles.contains("SYSTEM");
    }
}
