package top.egon.cola.component.ddc.admin.security.registration;

import java.time.Instant;
import java.util.Set;

public final class DdcRegistrationTestFixture {

    private DdcRegistrationTestFixture() {
    }

    public static VerifiedDdcRegistrationIdentity identity(Instant expiresAt) {
        return identity(expiresAt, 7L);
    }

    public static VerifiedDdcRegistrationIdentity identity(
            Instant expiresAt,
            long resourceVersion) {
        return new VerifiedDdcRegistrationIdentity(
                "app-id",
                "client-id",
                "resource-1",
                "https://api.example/resource-1",
                resourceVersion,
                "default",
                "demo",
                "dev",
                "instance-1",
                "credential-1",
                "token-1",
                expiresAt.minusSeconds(60),
                expiresAt,
                Set.of(IdpJwtDdcRegistrationCredentialVerifier.REGISTRATION_SCOPE)
        );
    }

    public static DdcRegistrationCredentialVerifier verifier(Instant expiresAt) {
        return (token, bizCode, appCode, env, instanceId) ->
                new VerifiedDdcRegistrationIdentity(
                        "app-id",
                        "client-id",
                        "resource-1",
                        "https://api.example/resource-1",
                        7L,
                        bizCode,
                        appCode,
                        env,
                        instanceId,
                        "credential-1",
                        "token-1",
                        expiresAt.minusSeconds(60),
                        expiresAt,
                        Set.of(IdpJwtDdcRegistrationCredentialVerifier.REGISTRATION_SCOPE)
                );
    }
}
