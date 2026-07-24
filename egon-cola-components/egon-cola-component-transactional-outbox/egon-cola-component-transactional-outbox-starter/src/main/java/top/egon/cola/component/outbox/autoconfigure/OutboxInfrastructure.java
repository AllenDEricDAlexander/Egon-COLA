package top.egon.cola.component.outbox.autoconfigure;

import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

public record OutboxInfrastructure(
        DataSource dataSource,
        String dataSourceBeanName,
        PlatformTransactionManager transactionManager,
        String transactionManagerBeanName
) {
}
