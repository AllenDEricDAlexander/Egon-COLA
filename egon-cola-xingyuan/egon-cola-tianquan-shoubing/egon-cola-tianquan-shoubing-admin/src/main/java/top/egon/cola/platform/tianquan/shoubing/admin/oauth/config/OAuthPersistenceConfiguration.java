package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;
import top.egon.cola.component.outbox.migration.OutboxLogicalDataSourceFactory;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class OAuthPersistenceConfiguration {

    @Bean("egonColaShardingLogicalDataSourceFactory")
    public LogicalDataSourceFactory egonColaShardingLogicalDataSourceFactory(
            OAuthManagedDdlComponent oauthManagedDdlComponent,
            @Qualifier("outboxManagedDdlInitializer") OutboxManagedDdlInitializer outboxDdlInitializer) {
        return (physical, yaml) -> {
            oauthManagedDdlComponent.initialize(physical, yaml);
            return new OutboxLogicalDataSourceFactory(outboxDdlInitializer).create(physical, yaml);
        };
    }

    @Bean("transactionManager")
    @Primary
    public DataSourceTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
