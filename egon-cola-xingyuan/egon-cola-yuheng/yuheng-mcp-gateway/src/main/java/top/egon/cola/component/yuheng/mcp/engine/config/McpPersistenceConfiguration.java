package top.egon.cola.component.yuheng.mcp.engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 中文说明：{@code McpPersistenceConfiguration} 是 MCP 引擎作为 schema <b>消费者</b> 的装配点。它不创建数据源、
 * SqlSessionFactory 或事务管理器：这些都由组件的 {@code EgonColaShardingAutoConfiguration} 与 MyBatis-Plus 自动装配提供，
 * 引擎与 Admin 共用同一份 49 逻辑表 / 1 库 x 1 表拓扑，但<b>不</b>拥有 DDL。
 * 因此这里强制三件事：消费者必须 {@code managed-ddl-enabled=false}、组件侧
 * {@code egon.cola.component.mybatis-plus.ddl.enabled} 也必须为 false（两处不一致即拒绝启动，避免出现「谁都会建表」或
 * 「谁都以为对方在建表」），以及部署身份必须完整（期望 schema 版本与 SHA 由 {@link McpPersistenceProperties} 以校验注解
 * 强制，缺任一项在绑定阶段就失败）。建表权只属于 Admin 的受管 DDL 工厂。
 * English summary: {@code McpPersistenceConfiguration} is the wiring point for the MCP engine acting as a schema
 * <b>consumer</b>. It creates no datasource, SqlSessionFactory or transaction manager: those come from the component's
 * {@code EgonColaShardingAutoConfiguration} and MyBatis-Plus auto-configuration, and the engine shares the very same
 * 49-logical-table / 1x1 topology as Admin but does <b>not</b> own DDL. Three things are therefore enforced here: a
 * consumer must set {@code managed-ddl-enabled=false}, the component-side
 * {@code egon.cola.component.mybatis-plus.ddl.enabled} must be false as well (a disagreement rejects startup, so no host
 * can end up either "everyone creates tables" or "everyone assumed the other one did"), and the deployment identity must be
 * complete — the expected schema version and SHA are mandated by validation annotations on {@link McpPersistenceProperties},
 * so a missing value fails at binding time. Table creation belongs exclusively to the Admin managed-DDL factory.
 *
 * 用法 / Usage: 由引擎的包扫描装配；{@link #mcpPersistenceContextComponent} 里的租户值只来自
 * {@code yuheng.persistence.*}，因此本进程的持久化身份只有一个事实来源。
 * / Picked up by the engine's component scan; {@link #mcpPersistenceContextComponent} takes its tenancy solely from
 * {@code yuheng.persistence.*}, so the process has one source of truth for persistence identity.
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(value = "mcpPersistenceConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(McpPersistenceProperties.class)
public class McpPersistenceConfiguration implements SmartInitializingSingleton {

    /** 中文说明：组件唯一数据源的 bean 名，消费者的读路径也只能走它。 English summary: bean name of the component's single datasource, the only read path available to a consumer. */
    private static final String DATA_SOURCE_BEAN_NAME = "dataSource";

    /** 中文说明：被移除的旧 JPA 运行期 bean 名，出现即说明有依赖把第二套 ORM 带了回来。 English summary: bean name of the removed JPA runtime; its presence means a dependency reintroduced a second ORM. */
    private static final String LEGACY_ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";

    /** 中文说明：消费者禁止持有的 Flyway 运行期 bean 名；建表只属于 Admin。 English summary: the Flyway runtime bean name a consumer must never hold; schema creation belongs to Admin alone. */
    private static final String LEGACY_FLYWAY_BEAN_NAME = "flyway";

    /** 中文说明：容器内省入口，仅用于断言单数据源/单会话工厂/单事务管理器拓扑。 English summary: container introspection entry, used only to assert the single datasource/factory/transaction-manager topology. */
    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * 中文说明：注册消费者身份上下文组件，并在装配时复核「消费者不得持有 DDL」这一不变式。
     * English summary: registers the consumer identity context component and re-checks the "a consumer holds no DDL"
     * invariant at wiring time.
     *
     * 用法 / Usage: 由 {@code MpMcpRuntimeTaskStore} 与 {@code MpMcpApprovalAdapter} 之外的非 HTTP 执行路径注入使用。
     * / Injected by the non-HTTP execution paths around the guarded MCP stores.
     * @param persistenceProperties 参数 部署身份配置；parameter the deployment identity configuration.
     * @param mybatisPlusProperties 参数 组件持久化开关；parameter the component persistence switches.
     * @return 返回 消费者身份上下文组件；returns the consumer identity context component.
     */
    @Bean("mcpPersistenceContextComponent")
    public McpPersistenceContextComponent mcpPersistenceContextComponent(
            @Qualifier(McpPersistenceProperties.BEAN_NAME) McpPersistenceProperties persistenceProperties,
            @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
            EgonColaMybatisPlusProperties mybatisPlusProperties) {
        Boolean managedDdlEnabled = persistenceProperties.getManagedDdlEnabled();
        if (managedDdlEnabled == null) {
            log.error("the MCP engine requires the managed DDL role flag to be configured");
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_FLAG_REQUIRED");
        }
        if (managedDdlEnabled.booleanValue() != mybatisPlusProperties.getDdl().isEnabled()) {
            log.error("deployment managed-ddl flag {} disagrees with the component ddl flag {}",
                    managedDdlEnabled, mybatisPlusProperties.getDdl().isEnabled());
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_FLAG_CONFLICT");
        }
        if (managedDdlEnabled) {
            log.error("the MCP engine only consumes the managed schema and must never own managed DDL");
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_ROLE_MISMATCH");
        }
        log.info("MCP engine is a managed-schema consumer for version {}", persistenceProperties.getExpectedSchemaVersion());
        return new McpPersistenceContextComponent(persistenceProperties, mybatisPlusProperties);
    }

    @Override
    public void afterSingletonsInstantiated() {
        String[] dataSources = beanFactory.getBeanNamesForType(DataSource.class, false, false);
        String[] factories = beanFactory.getBeanNamesForType(SqlSessionFactory.class, false, false);
        String[] managers = beanFactory.getBeanNamesForType(PlatformTransactionManager.class, false, false);
        if (dataSources.length != 1 || factories.length != 1 || managers.length != 1
                || !beanFactory.containsBean(DATA_SOURCE_BEAN_NAME)
                || beanFactory.containsBean(LEGACY_ENTITY_MANAGER_FACTORY_BEAN_NAME)
                || beanFactory.containsBean(LEGACY_FLYWAY_BEAN_NAME)) {
            log.error("mcp consumer must ride exactly one dataSource/SqlSessionFactory/transaction manager and run no "
                            + "JPA or Flyway runtime, found dataSources={} sqlSessionFactory={} transactionManagers={}",
                    Arrays.toString(dataSources), Arrays.toString(factories), Arrays.toString(managers));
            throw new EgonColaMybatisPlusConfigurationException("PERSISTENCE_RUNTIME_NOT_SINGLE");
        }
    }
}
