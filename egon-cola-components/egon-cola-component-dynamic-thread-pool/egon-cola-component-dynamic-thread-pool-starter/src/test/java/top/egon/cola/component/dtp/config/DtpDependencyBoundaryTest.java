package top.egon.cola.component.dtp.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The starter must not put third-party auto-configuration on a business application's classpath.
 * redisson-spring-boot-starter registers RedissonAutoConfigurationV2, which eagerly connects to
 * redis://127.0.0.1:6379 and fails startup for applications that have no Redis. This component
 * builds its own RedissonClient, so only the plain Redisson client belongs here.
 */
class DtpDependencyBoundaryTest {

    @Test
    void redissonSpringBootAutoConfigurationIsNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class, () ->
                Class.forName("org.redisson.spring.starter.RedissonAutoConfigurationV2"));
    }

    @Test
    void plainRedissonClientIsAvailable() throws Exception {
        Class.forName("org.redisson.api.RedissonClient");
    }

    @Test
    void retiredThirdPartyLibrariesAreGone() {
        assertThrows(ClassNotFoundException.class, () ->
                Class.forName("org.apache.commons.lang.StringUtils"));
        assertThrows(ClassNotFoundException.class, () ->
                Class.forName("com.alibaba.fastjson2.JSON"));
    }
}
