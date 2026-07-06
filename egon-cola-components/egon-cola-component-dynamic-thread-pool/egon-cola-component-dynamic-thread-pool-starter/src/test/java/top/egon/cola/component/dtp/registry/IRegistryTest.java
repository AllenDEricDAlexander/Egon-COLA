package top.egon.cola.component.dtp.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolConfigEntity;
import top.egon.cola.component.dtp.registry.model.DtpAuditEvent;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @description 注册中心接口单元测试
 */
public class IRegistryTest {

    @Test
    public void test_deprecatedReportThreadPool_throwsUnsupportedOperationException() {
        IRegistry registry = new UnsupportedRegistry();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> registry.reportThreadPool(Collections.emptyList()));

        assertEquals("reportThreadPool is deprecated; implement reportSnapshots instead", exception.getMessage());
    }

    @Test
    public void test_deprecatedReportThreadPoolConfigParameter_throwsUnsupportedOperationException() {
        IRegistry registry = new UnsupportedRegistry();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> registry.reportThreadPoolConfigParameter(new ThreadPoolConfigEntity("test-app", "executor01")));

        assertEquals("reportThreadPoolConfigParameter is deprecated; implement reportSnapshot instead", exception.getMessage());
    }

    private static class UnsupportedRegistry implements IRegistry {

        @Override
        public void reportSnapshot(ExecutorSnapshot snapshot) {
        }

        @Override
        public void reportSnapshots(List<ExecutorSnapshot> snapshots) {
        }

        @Override
        public List<ExecutorSnapshot> querySnapshots(String appName, String instanceId) {
            return Collections.emptyList();
        }

        @Override
        public ExecutorSnapshot querySnapshot(String appName, String instanceId, String executorName) {
            return null;
        }

        @Override
        public void publishConfigChange(DtpConfigChangeMessage message) {
        }

        @Override
        public void recordAuditEvent(DtpAuditEvent event) {
        }

        @Override
        public List<DtpAuditEvent> queryAuditEvents(String appName, String date) {
            return Collections.emptyList();
        }

    }

}
