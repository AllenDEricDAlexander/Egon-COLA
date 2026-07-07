package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.service.policy.PublishConsistencyPolicyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        DdcPublishService.class,
        PublishFailureRecorder.class,
        PublishConsistencyPolicyFactory.class,
        DdcPublishServiceFailureTest.RedisTestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_publish_failure_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.flyway.enabled=false",
        "egon.cola.component.ddc.enabled=false"
})
class DdcPublishServiceFailureTest {

    @Autowired
    private DdcPublishService publishService;

    @Autowired
    private DdcPublishTaskRepository publishTaskRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void transactionFailureCreatesFailedRecordAndThrows() {
        DdcPublishRequest request = DdcPublishRequest.invalidForTest("demo", "dev", "default", "switch");

        assertThatThrownBy(() -> publishService.publish(request, "tester"))
                .isInstanceOf(DdcAdminException.class);

        assertThat(publishTaskRepository.findAll())
                .anyMatch(task -> "FAILED".equals(task.getStatus()) && task.getErrorMessage() != null);
    }

    @TestConfiguration
    static class RedisTestConfig {

        @Bean
        DdcRedisRepository ddcRedisRepository() {
            return Mockito.mock(DdcRedisRepository.class);
        }
    }
}
