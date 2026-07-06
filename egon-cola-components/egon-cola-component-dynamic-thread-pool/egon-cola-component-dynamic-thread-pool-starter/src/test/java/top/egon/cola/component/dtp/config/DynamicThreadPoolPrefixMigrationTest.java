package top.egon.cola.component.dtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicThreadPoolPrefixMigrationTest {

    @Test
    void bindsEgonColaDtpPrefix() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "egon.cola.component.dtp.enabled", "true",
                "egon.cola.component.dtp.app-name", "student-management",
                "egon.cola.component.dtp.instance-id", "student-management-8080",
                "egon.cola.component.dtp.registry.redis.host", "127.0.0.1",
                "egon.cola.component.dtp.report.interval", "30s"
        ));

        DynamicThreadPoolAutoProperties properties = new Binder(source)
                .bind("egon.cola.component.dtp", DynamicThreadPoolAutoProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertTrue(properties.isEnabled());
        assertEquals("student-management", properties.getAppName());
        assertEquals("student-management-8080", properties.getInstanceId());
        assertEquals("127.0.0.1", properties.getRegistry().getRedis().getHost());
        assertEquals(30, properties.getReport().getInterval().toSeconds());
    }
}
