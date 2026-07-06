package top.egon.cola.component.dtp.registry.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @description Redis key 单元测试
 */
public class DtpRedisKeysTest {

    @Test
    public void test_keys() {
        assertEquals("DTP:APPS", DtpRedisKeys.apps());
        assertEquals("DTP:APP:test-app:INSTANCES", DtpRedisKeys.instances("test-app"));
        assertEquals("DTP:SNAPSHOT:test-app:i-001:executor01", DtpRedisKeys.snapshot("test-app", "i-001", "executor01"));
        assertEquals("DTP:CHANGE_TOPIC:test-app", DtpRedisKeys.changeTopic("test-app"));
        assertEquals("DTP:EVENT:test-app:20260629", DtpRedisKeys.event("test-app", "20260629"));
    }

}
