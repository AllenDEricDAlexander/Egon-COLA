package top.egon.cola.component.outbox.autoconfigure;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;

import javax.sql.DataSource;
import java.util.Arrays;

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
        if (selectedTransactionManager.bean() instanceof DataSourceTransactionManager manager
                && manager.getDataSource() != selectedDataSource.bean()) {
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
