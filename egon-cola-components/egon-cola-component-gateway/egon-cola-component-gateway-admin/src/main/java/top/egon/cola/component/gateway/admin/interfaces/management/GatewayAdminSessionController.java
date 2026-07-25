package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.time.Instant;
import java.util.List;

/**
 * Exposes identity and authorization facts from the verified token.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/session")
public class GatewayAdminSessionController {

    @GetMapping
    public SessionView session(
            AdminActor actor,
            Authentication authentication) {
        String displayName = actor.actorId();
        Instant expiresAt = null;
        if (authentication instanceof JwtAuthenticationToken jwt) {
            displayName = displayName(jwt, actor.actorId());
            expiresAt = jwt.getToken().getExpiresAt();
        }
        return new SessionView(
                actor.actorId(),
                displayName,
                actor.actorType().name(),
                actor.scopes().stream().sorted().toList(),
                actor.roles().stream().sorted().toList(),
                expiresAt
        );
    }

    private String displayName(
            JwtAuthenticationToken jwt,
            String fallback) {
        String preferred = jwt.getToken().getClaimAsString(
                "preferred_username"
        );
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        String name = jwt.getToken().getClaimAsString("name");
        return name == null || name.isBlank() ? fallback : name;
    }

    public record SessionView(
            String actorId,
            String displayName,
            String actorType,
            List<String> capabilities,
            List<String> roles,
            Instant expiresAt
    ) {
    }
}
