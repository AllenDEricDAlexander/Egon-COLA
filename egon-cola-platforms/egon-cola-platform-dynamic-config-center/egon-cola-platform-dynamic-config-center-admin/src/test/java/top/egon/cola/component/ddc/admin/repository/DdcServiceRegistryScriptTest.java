package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcServiceRegistryScriptTest {

    @Test
    void scriptsUseAtomicIdentityChecksWithoutGlobalDiscoveryCommands() throws Exception {
        List<String> scripts = List.of(
                script("redis/ddc_service_register.lua"),
                script("redis/ddc_service_heartbeat.lua"),
                script("redis/ddc_service_deregister.lua"),
                script("redis/ddc_service_expire.lua")
        );

        assertThat(scripts)
                .allSatisfy(script -> assertThat(script)
                        .doesNotContain("redis.call('KEYS'")
                        .doesNotContain("redis.call('SCAN'"));
        assertThat(scripts.get(1))
                .contains("instance['instanceId'] ~= ARGV[1]")
                .contains("instance['leaseId'] ~= ARGV[2]")
                .contains("instance['serviceKeyCanonical'] ~= ARGV[3]");
        assertThat(scripts.get(2))
                .contains("instance['instanceId'] ~= ARGV[1]")
                .contains("instance['leaseId'] ~= ARGV[2]")
                .contains("instance['serviceKeyCanonical'] ~= ARGV[3]");
        assertThat(List.of(scripts.get(0), scripts.get(2), scripts.get(3)))
                .allSatisfy(script -> assertThat(script)
                        .contains("redis.call('PUBLISH', KEYS[6]")
                        .doesNotContain("redis.call('PUBLISH', ARGV["));
    }

    private String script(String path) throws Exception {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
