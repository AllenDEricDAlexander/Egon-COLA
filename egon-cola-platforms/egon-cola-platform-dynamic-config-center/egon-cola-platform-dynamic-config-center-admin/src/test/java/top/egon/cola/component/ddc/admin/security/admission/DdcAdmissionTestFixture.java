package top.egon.cola.component.ddc.admin.security.admission;

import java.time.Instant;

public final class DdcAdmissionTestFixture {

    private DdcAdmissionTestFixture() {
    }

    public static DdcAdmissionClaims claims(Instant expiresAt) {
        return claims(expiresAt, 7L);
    }

    public static DdcAdmissionClaims claims(Instant expiresAt,
                                             long resourceVersion) {
        return new DdcAdmissionClaims(
                "resource-1",
                "https://api.example/resource-1",
                resourceVersion,
                "default",
                "demo",
                "dev",
                "instance-1",
                "credential-1",
                expiresAt.minusSeconds(60),
                expiresAt
        );
    }

    public static DdcAdmissionVerifier verifier(Instant expiresAt) {
        return (ticket, bizCode, appCode, env, instanceId) ->
                new DdcAdmissionClaims(
                        "resource-1",
                        "https://api.example/resource-1",
                        7L,
                        bizCode,
                        appCode,
                        env,
                        instanceId,
                        "credential-1",
                        expiresAt.minusSeconds(60),
                        expiresAt
                );
    }
}
