package top.egon.cola.component.outbox.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OutboxInfrastructureResolverTest {

    private final OutboxInfrastructureResolver resolver = new OutboxInfrastructureResolver();

    @Test
    void shouldSelectUniqueDataSourceAndTransactionManager() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        DataSource dataSource = mock(DataSource.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        beanFactory.registerSingleton("businessDataSource", dataSource);
        beanFactory.registerSingleton("businessTransactionManager", transactionManager);

        OutboxInfrastructure infrastructure =
                resolver.resolve(beanFactory, new TransactionalOutboxProperties());

        assertThat(infrastructure.dataSource()).isSameAs(dataSource);
        assertThat(infrastructure.dataSourceBeanName()).isEqualTo("businessDataSource");
        assertThat(infrastructure.transactionManager()).isSameAs(transactionManager);
        assertThat(infrastructure.transactionManagerBeanName())
                .isEqualTo("businessTransactionManager");
    }

    @Test
    void shouldRequireNamesForAmbiguousCandidates() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("firstDataSource", mock(DataSource.class));
        beanFactory.registerSingleton("secondDataSource", mock(DataSource.class));
        beanFactory.registerSingleton("firstTx", mock(PlatformTransactionManager.class));
        beanFactory.registerSingleton("secondTx", mock(PlatformTransactionManager.class));

        assertThatThrownBy(() ->
                resolver.resolve(beanFactory, new TransactionalOutboxProperties()))
                .isInstanceOf(OutboxConfigurationException.class)
                .hasMessageContaining("data-source-bean-name");
    }

    @Test
    void shouldHonorExplicitBeanNames() {
        DefaultListableBeanFactory beanFactory = candidates();
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getStorage().setDataSourceBeanName("secondDataSource");
        properties.getStorage().setTransactionManagerBeanName("secondTx");

        OutboxInfrastructure infrastructure = resolver.resolve(beanFactory, properties);

        assertThat(infrastructure.dataSourceBeanName()).isEqualTo("secondDataSource");
        assertThat(infrastructure.transactionManagerBeanName()).isEqualTo("secondTx");
    }

    @Test
    void shouldSelectTheSolePrimaryCandidate() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        register(beanFactory, "firstDataSource", DataSource.class, mock(DataSource.class), false);
        register(beanFactory, "primaryDataSource", DataSource.class, mock(DataSource.class), true);
        register(
                beanFactory,
                "firstTx",
                PlatformTransactionManager.class,
                mock(PlatformTransactionManager.class),
                false
        );
        register(
                beanFactory,
                "primaryTx",
                PlatformTransactionManager.class,
                mock(PlatformTransactionManager.class),
                true
        );

        OutboxInfrastructure infrastructure =
                resolver.resolve(beanFactory, new TransactionalOutboxProperties());

        assertThat(infrastructure.dataSourceBeanName()).isEqualTo("primaryDataSource");
        assertThat(infrastructure.transactionManagerBeanName()).isEqualTo("primaryTx");
    }

    @Test
    void shouldRejectExplicitBeanWithWrongType() {
        DefaultListableBeanFactory beanFactory = candidates();
        beanFactory.registerSingleton("notADataSource", new Object());
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getStorage().setDataSourceBeanName("notADataSource");
        properties.getStorage().setTransactionManagerBeanName("firstTx");

        assertThatThrownBy(() -> resolver.resolve(beanFactory, properties))
                .isInstanceOf(OutboxConfigurationException.class)
                .hasMessageContaining("notADataSource");
    }

    private DefaultListableBeanFactory candidates() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("firstDataSource", mock(DataSource.class));
        beanFactory.registerSingleton("secondDataSource", mock(DataSource.class));
        beanFactory.registerSingleton("firstTx", mock(PlatformTransactionManager.class));
        beanFactory.registerSingleton("secondTx", mock(PlatformTransactionManager.class));
        return beanFactory;
    }

    private <T> void register(
            DefaultListableBeanFactory beanFactory,
            String name,
            Class<T> type,
            T instance,
            boolean primary
    ) {
        RootBeanDefinition definition = new RootBeanDefinition(type, () -> instance);
        definition.setPrimary(primary);
        beanFactory.registerBeanDefinition(name, definition);
    }
}
