package top.egon.cola.platform.idp.core.identity;

public record AuthenticatedIdentity(
        String identitySub,
        String username,
        String displayName,
        boolean mustChangePassword
) {
}
