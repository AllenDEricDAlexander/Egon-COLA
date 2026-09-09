package top.egon.cola.component.rpc.test.process;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import top.egon.cola.component.tianshu.admin.DynamicConfigCenterAdminApplication;
import top.egon.cola.component.tianshu.admin.security.registration.DdcRegistrationCredentialVerifier;
import top.egon.cola.component.tianshu.admin.security.registration.VerifiedDdcRegistrationIdentity;

import java.time.Instant;
import java.util.Set;

/**
 * Starts Tianshu Admin with the process test's isolated admission boundary.
 */
public final class RpcTestDdcAdminApplication {

    private RpcTestDdcAdminApplication() {
    }

    /**
     * Starts the real Tianshu Admin application plus the test-only verifier.
     *
     * @param args Spring Boot command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(
                new Class<?>[]{
                        DynamicConfigCenterAdminApplication.class,
                        AdmissionConfiguration.class
                },
                args
        );
    }

    @Configuration(proxyBeanMethods = false)
    static class AdmissionConfiguration {

        @Bean
        @Primary
        DdcRegistrationCredentialVerifier processTestRegistrationVerifier() {
            return (ticket, bizCode, appCode, env, instanceId) -> {
                if (!"test-admission-ticket".equals(ticket)) {
                    throw new IllegalArgumentException(
                            "Unexpected process-test admission ticket"
                    );
                }
                Instant issuedAt = Instant.now();
                return new VerifiedDdcRegistrationIdentity(
                        "rpc-process-test-app",
                        "rpc-process-test-client",
                        "rpc-process-test-resource",
                        "urn:egon:resource:rpc-process-test",
                        1L,
                        bizCode,
                        appCode,
                        env,
                        instanceId,
                        "rpc-process-test-credential",
                        "rpc-process-test-token",
                        issuedAt,
                        issuedAt.plusSeconds(300),
                        Set.of("tianshu:registration:write")
                );
            };
        }

    }
}
