package top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;

/** Immutable adapter-owned evidence from the same completed token verification. */
public record GatewayVerifiedIdentityBO(@Valid TokenAuditProofBO proof,
        @NotNull @Size(max = 64) Set<@NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String> verifiedScopes) {
    public GatewayVerifiedIdentityBO {
        Objects.requireNonNull(verifiedScopes, "verifiedScopes");
        if (verifiedScopes.size() > 64 || verifiedScopes.stream().anyMatch(scope -> scope == null
                || !scope.matches("[A-Za-z0-9._:-]{1,128}"))) {
            throw new IllegalArgumentException("GATEWAY_VERIFIED_SCOPES_INVALID");
        }
        verifiedScopes = Set.copyOf(verifiedScopes);
    }

    @Override public String toString() { return "GatewayVerifiedIdentityBO[redacted]"; }
}
