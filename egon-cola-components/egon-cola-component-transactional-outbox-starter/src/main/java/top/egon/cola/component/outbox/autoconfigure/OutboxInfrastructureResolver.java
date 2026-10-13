package top.egon.cola.component.outbox.autoconfigure;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import org.springframework.util.ClassUtils;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;

import javax.sql.DataSource;
import java.util.Arrays;

@Slf4j
public class OutboxInfrastructureResolver {

    public OutboxInfrastructure resolve(
            ConfigurableListableBeanFactory beanFactory,
            TransactionalOutboxProperties properties
    ) {
        SelectedBean<DataSource> selectedDataSource = select(
                beanFactory,
                DataSource.class,
                properties.getStorage().getDataSourceBeanName(),
                "storage.data-source-bean-name"
        );
        SelectedBean<PlatformTransactionManager> selectedTransactionManager = select(
                beanFactory,
                PlatformTransactionManager.class,
                properties.getStorage().getTransactionManagerBeanName(),
                "storage.transaction-manager-bean-name"
        );
        PlatformTransactionManager manager = selectedTransactionManager.bean();
        ClassLoader loader = beanFactory.getBeanClassLoader();
        if (isKnownLocalManager(manager, loader)
                && !supportsSelectedLocalTransaction(manager, selectedDataSource.bean(), loader)) {
            throw new OutboxConfigurationException(
                    "Transactional outbox DataSource and transaction manager do not match"
            );
        }
        return new OutboxInfrastructure(
                selectedDataSource.bean(),
                selectedDataSource.name(),
                selectedTransactionManager.bean(),
                selectedTransactionManager.name()
        );
    }

    static boolean supportsSelectedLocalTransaction(PlatformTransactionManager manager, DataSource selected, ClassLoader loader) {
        if (selected == null) { return false; }
        if (manager instanceof DataSourceTransactionManager jdbc) { return jdbc.getDataSource() == selected; }
        return jpaTypesPresent(loader) && JpaTransactionManagerSupport.matches(manager, selected);
    }

    private static boolean isKnownLocalManager(PlatformTransactionManager manager, ClassLoader loader) {
        return manager instanceof DataSourceTransactionManager
                || jpaTypesPresent(loader) && JpaTransactionManagerSupport.isJpa(manager);
    }

    private static boolean jpaTypesPresent(ClassLoader loader) {
        return ClassUtils.isPresent("org.springframework.orm.jpa.JpaTransactionManager", loader)
                && ClassUtils.isPresent("jakarta.persistence.EntityManagerFactory", loader);
    }

    /** Isolated typed adapter: JDBC-only hosts never load optional JPA signatures. */
    @Slf4j
    private static class JpaTransactionManagerSupport {
        static boolean matches(PlatformTransactionManager manager, DataSource selected) {
            return manager instanceof org.springframework.orm.jpa.JpaTransactionManager jpa && jpa.getDataSource() == selected;
        }

        static boolean isJpa(PlatformTransactionManager manager) {
            return manager instanceof org.springframework.orm.jpa.JpaTransactionManager;
        }
    }

    private <T> SelectedBean<T> select(
            ConfigurableListableBeanFactory beanFactory,
            Class<T> type,
            String explicitName,
            String property
    ) {
        if (StringUtils.hasText(explicitName)) {
            if (!beanFactory.containsBean(explicitName)
                    || !type.isAssignableFrom(beanFactory.getType(explicitName))) {
                throw new OutboxConfigurationException(
                        "Invalid transactional outbox bean '" + explicitName + "' for " + property
                );
            }
            return new SelectedBean<>(explicitName, beanFactory.getBean(explicitName, type));
        }

        String[] candidates = beanFactory.getBeanNamesForType(type, false, false);
        if (candidates.length == 1) {
            return new SelectedBean<>(candidates[0], beanFactory.getBean(candidates[0], type));
        }
        String[] primaries = Arrays.stream(candidates)
                .filter(beanFactory::containsBeanDefinition)
                .filter(name -> beanFactory.getBeanDefinition(name).isPrimary())
                .toArray(String[]::new);
        if (primaries.length == 1) {
            return new SelectedBean<>(primaries[0], beanFactory.getBean(primaries[0], type));
        }
        throw new OutboxConfigurationException(
                "Configure egon.cola.component.transactional-outbox."
                        + property + "; candidates=" + Arrays.toString(candidates)
        );
    }

    private record SelectedBean<T>(String name, T bean) {
    }
}
