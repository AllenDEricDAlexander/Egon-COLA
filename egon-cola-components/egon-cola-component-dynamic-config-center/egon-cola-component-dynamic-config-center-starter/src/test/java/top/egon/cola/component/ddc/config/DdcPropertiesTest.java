package top.egon.cola.component.ddc.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcPropertiesTest {

    @Test
    void adminEndpointIsRequiredAndNormalized() {
        DdcProperties.Admin admin = new DdcProperties.Admin();

        assertThatThrownBy(admin::requireEndpoint)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("egon.cola.component.ddc.admin.endpoint is required");

        admin.setEndpoint("http://ddc.test/");

        assertThat(admin.requireEndpoint()).isEqualTo("http://ddc.test");
    }

    @Test
    void adminEndpointRejectsNonRootUris() {
        DdcProperties.Admin admin = new DdcProperties.Admin();
        for (String endpoint : List.of(
                "file:///tmp/ddc",
                "http://ddc.test/context",
                "http://ddc.test?node=1",
                "http://ddc.test#fragment"
        )) {
            admin.setEndpoint(endpoint);
            assertThatThrownBy(admin::requireEndpoint)
                    .hasMessage(
                            "egon.cola.component.ddc.admin.endpoint "
                                    + "must be an HTTP or HTTPS root URI"
                    );
        }
    }

    @Test
    void signedRequestsRequireBothCredentials() {
        DdcProperties.Admin admin = new DdcProperties.Admin();
        admin.setSignatureEnabled(true);

        assertThatThrownBy(admin::validateCredentials)
                .hasMessage(
                        "egon.cola.component.ddc.admin.access-key "
                                + "is required when signature is enabled"
                );

        admin.setAccessKey("ak");
        assertThatThrownBy(admin::validateCredentials)
                .hasMessage(
                        "egon.cola.component.ddc.admin.secret-key "
                                + "is required when signature is enabled"
                );

        admin.setSecretKey("sk");
        admin.validateCredentials();
    }

    @Test
    void configClientHeartbeatMustBePositiveAndShorterThanLease() {
        DdcProperties.Instance instance = new DdcProperties.Instance();
        instance.setHeartbeatIntervalSeconds(0);
        instance.setLeaseSeconds(30);

        assertThatThrownBy(instance::validate)
                .hasMessage(
                        "egon.cola.component.ddc.instance.heartbeat-interval-seconds "
                                + "must be positive and less than lease-seconds"
                );

        instance.setHeartbeatIntervalSeconds(30);

        assertThatThrownBy(instance::validate)
                .hasMessage(
                        "egon.cola.component.ddc.instance.heartbeat-interval-seconds "
                                + "must be positive and less than lease-seconds"
                );

        instance.setHeartbeatIntervalSeconds(10);
        instance.validate();
    }
}
