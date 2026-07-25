package top.egon.cola.component.gateway.admin.domain;

import java.util.Set;

public record AdminActor(
        String actorId,
        ActorType actorType,
        Set<String> scopes,
        Set<String> roles
) {

    public AdminActor {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId is required");
        }
        actorId = actorId.trim();
        actorType = java.util.Objects.requireNonNull(
                actorType,
                "actorType"
        );
        scopes = Set.copyOf(scopes == null ? Set.of() : scopes);
        roles = Set.copyOf(roles == null ? Set.of() : roles);
    }

    public enum ActorType {
        USER,
        SERVICE
    }
}
