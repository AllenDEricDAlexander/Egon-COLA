package top.egon.cola.platform.tianquan.shoubing.core.identity;

public record AuthenticatedIdentity(
        String identitySub,
        String username,
        String displayName,
        boolean mustChangePassword
) {
}
