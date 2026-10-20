package top.egon.cola.component.dtp.admin.trigger.converter;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.dtp.admin.trigger.model.ResizeExecutorRequest;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

import static org.assertj.core.api.Assertions.*;

class ResizeExecutorConverterTest {
    @Test
    void mapsAllParametersAndDerivesIdentityFromPath() {
        var source = new ResizeExecutorRequest();
        source.setExecutorKind(ExecutorKind.SPRING_THREAD_POOL_TASK_EXECUTOR);
        source.setCorePoolSize(2); source.setMaximumPoolSize(4); source.setQueueCapacity(128);
        source.setKeepAliveSeconds(30L); source.setAllowCoreThreadTimeOut(true); source.setOperator("ops");
        var converter = Mappers.getMapper(ResizeExecutorConverter.class);
        var result = converter.toCommand(source, "app", "instance", "pool");
        assertThat(result.getAppName()).isEqualTo("app");
        assertThat(result.getInstanceId()).isEqualTo("instance");
        assertThat(result.getExecutorName()).isEqualTo("pool");
        assertThat(result.getExecutorKind()).isSameAs(source.getExecutorKind());
        assertThat(result.getCorePoolSize()).isEqualTo(2);
        assertThat(result.getMaximumPoolSize()).isEqualTo(4);
        assertThat(result.getQueueCapacity()).isEqualTo(128);
        assertThat(result.getKeepAliveSeconds()).isEqualTo(30);
        assertThat(result.getAllowCoreThreadTimeOut()).isTrue();
        assertThat(result.getOperator()).isEqualTo("ops");
        assertThat(result.getVersion()).isNull();
        source.setQueueCapacity(null);
        assertThat(converter.toCommand(source, "app", "instance", "pool").getQueueCapacity()).isNull();
    }
}
