package top.egon.cola.component.tianshu.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DdcOutboxConfigurationTest {

    @Test
    void publicationAndPersistenceUseTheSameNamedUtcClock() {
        new ApplicationContextRunner()
                .withUserConfiguration(DdcOutboxConfiguration.class)
                .withBean("transactionManager", PlatformTransactionManager.class,
                        () -> mock(PlatformTransactionManager.class))
                .withBean("egon.cola.component.tianshu.admin-top.egon.cola.component.tianshu.admin.config.DdcAdminProperties",
                        DdcAdminProperties.class, DdcAdminProperties::new)
                .withBean("idpClock", Clock.class, Clock::systemDefaultZone)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Clock publication = context.getBean("ddcPublishClock", Clock.class);
                    assertThat(context.getBean("egonColaMybatisPlusClock", Clock.class)).isSameAs(publication);
                    assertThat(publication.getZone()).isEqualTo(ZoneOffset.UTC);
                    assertThat(context.getBean("idpClock")).isNotSameAs(publication);
                });
    }
}
