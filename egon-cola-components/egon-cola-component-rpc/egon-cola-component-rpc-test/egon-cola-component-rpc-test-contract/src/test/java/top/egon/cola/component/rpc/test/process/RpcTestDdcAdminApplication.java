package top.egon.cola.component.rpc.test.process;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import top.egon.cola.component.ddc.admin.DynamicConfigCenterAdminApplication;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionClaims;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionVerifier;

import java.time.Instant;

/**
 * Starts DDC Admin with the process test's isolated admission boundary.
 */
public final class RpcTestDdcAdminApplication {

    private RpcTestDdcAdminApplication() {
    }

    /**
     * Starts the real DDC Admin application plus the test-only verifier.
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
        DdcAdmissionVerifier processTestAdmissionVerifier() {
            return (ticket, bizCode, appCode, env, instanceId) -> {
                if (!"test-admission-ticket".equals(ticket)) {
                    throw new IllegalArgumentException(
                            "Unexpected process-test admission ticket"
                    );
                }
                Instant issuedAt = Instant.now();
                return new DdcAdmissionClaims(
                        "rpc-process-test-resource",
                        "urn:egon:resource:rpc-process-test",
                        1L,
                        bizCode,
                        appCode,
                        env,
                        instanceId,
                        "rpc-process-test-credential",
                        issuedAt,
                        issuedAt.plusSeconds(300)
                );
            };
        }

    }
}
