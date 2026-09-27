package top.egon.cola.component.yuheng.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 中文说明：{@code GatewayPersistenceConfiguration} 是 Admin 进程持久化运行期的唯一装配点。组件的
 * {@code EgonColaShardingAutoConfiguration} 已经创建全进程唯一的 {@code dataSource}（保留其唯一 {@code @Primary}），
 * 本类只在此之上新增具名 {@code gatewayTransactionManager} 与受管身份上下文；<b>不</b>声明任何
 * {@code DataSource}、{@code SqlSessionFactory}、{@code MybatisPlusInterceptor} 或第二个事务管理器，
 * 因为「按功能域偷偷造第二个连接池」会让审计、幂等与 journal 落到不同连接上而失去同事务原子性。
 * 旧 JPA/Flyway 运行链路已随 MP 切换移除，启动时再断言一次，防止依赖回归把第二套 ORM 悄悄带回来。
 * English summary: {@code GatewayPersistenceConfiguration} is the only persistence runtime wiring point of the Admin
 * process. The component's {@code EgonColaShardingAutoConfiguration} already owns the process-wide single
 * {@code dataSource} (keeping its single {@code @Primary}); this class only adds the named
 * {@code gatewayTransactionManager} and the managed identity context on top of it. It declares <b>no</b>
 * {@code DataSource}, {@code SqlSessionFactory}, {@code MybatisPlusInterceptor} or second transaction manager, because a
 * second pool per functional domain would split audit, idempotency and journal writes away from one transaction. The
 * legacy JPA/Flyway runtime was removed with the MyBatis-Plus cutover and is asserted again at startup so a dependency
 * regression cannot silently reintroduce a second ORM.
 *
 * 用法 / Usage: 由 {@code GatewayAdminApplication} 的包扫描装配；{@code yuheng.persistence.*} 缺任一项或角色开关
 * 与 Admin 职责不符时直接拒绝启动。
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(value = "gatewayPersistenceConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayPersistenceProperties.class)
public class GatewayPersistenceConfiguration implements SmartInitializingSingleton {

    /** 中文说明：组件唯一数据源的 bean 名，Admin 全部事务都必须绑定它。 English summary: bean name of the component's single datasource, which every Admin transaction must bind. */
    private static final String DATA_SOURCE_BEAN_NAME = "dataSource";

    /** 中文说明：本进程唯一事务管理器的 bean 名。 English summary: bean name of this process's only transaction manager. */
    private static final String TRANSACTION_MANAGER_BEAN_NAME = "gatewayTransactionManager";

    /** 中文说明：被移除的旧 JPA 运行期 bean 名，出现即说明有依赖把 JPA 带了回来。 English summary: bean name of the removed JPA runtime; its presence means a dependency reintroduced JPA. */
    private static final String LEGACY_ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";

    /** 中文说明：被移除的旧 Flyway 运行期 bean 名；受管 DDL 才是本进程唯一的建表路径。 English summary: bean name of the removed Flyway runtime; managed DDL is this process's only schema path. */
    private static final String LEGACY_FLYWAY_BEAN_NAME = "flyway";

    /** 中文说明：组件侧持久化开关，用于与部署侧受管 DDL 开关比对。 English summary: the component-side persistence switches, compared against the deployment managed-DDL flag. */
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties mybatisPlusProperties;

    /** 中文说明：容器内省入口，仅用于断言单数据源/单会话工厂/单事务管理器拓扑。 English summary: container introspection entry, used only to assert the single datasource/factory/transaction-manager topology. */
    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * 中文说明：把唯一事务管理器绑定到组件唯一的 {@code dataSource}。这里必须显式使用
     * {@link DataSourceTransactionManager}，因为运行期只有 MyBatis-Plus/JDBC 一条语句通路；声明它同时也让 Boot 的
     * {@code @ConditionalOnMissingBean(TransactionManager.class)} 退让，保证「唯一的 TM 就是具名的这一个」，
     * 现有 {@code @Transactional} 与 {@code TransactionTemplate} 无需改名即可落在同一连接上。
     * English summary: Binds the only transaction manager to the component's single {@code dataSource}. It must be an
     * explicit {@link DataSourceTransactionManager} because MyBatis-Plus/JDBC is the only statement path at runtime;
     * declaring it also makes Boot's {@code @ConditionalOnMissingBean(TransactionManager.class)} step aside, so the one
     * and only transaction manager is this named bean and existing {@code @Transactional} plus
     * {@code TransactionTemplate} usages resolve onto the same connection without renaming.
     *
     * 用法 / Usage: 由容器调用一次。/ Invoked once by the container.
     * @param dataSource 参数 组件唯一数据源；parameter the component's single datasource.
     * @return 返回 绑定该数据源的事务管理器；returns the transaction manager bound to that datasource.
     */
    @Bean(TRANSACTION_MANAGER_BEAN_NAME)
    public DataSourceTransactionManager gatewayTransactionManager(
            @Qualifier(DATA_SOURCE_BEAN_NAME) DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 中文说明：注册部署身份与 MDC 上下文组件，并在此校验部署身份与 Admin 角色是否一致：Admin 是唯一持有受管 DDL
     * 职责的进程，{@code managed-ddl-enabled} 必须为真且必须与组件侧
     * {@code egon.cola.component.mybatis-plus.ddl.enabled} 同开同关。放在这个无条件 bean 方法里，是因为条件化 bean
     * 可能整体退场，从而让角色校验被静默跳过。
     * English summary: Registers the deployment identity plus MDC context component and validates here that the
     * deployment identity matches the Admin role: Admin is the only process owning managed DDL, so
     * {@code managed-ddl-enabled} must be true and must agree switch-for-switch with the component's
     * {@code egon.cola.component.mybatis-plus.ddl.enabled}. The check lives in this unconditional bean method because a
     * conditional bean could back off entirely and silently skip the role validation.
     *
     * 用法 / Usage: 由 {@link GatewayPersistenceContextFilter} 经 bean 名
     * {@code gatewayPersistenceContextComponent} 注入。/ Injected by {@link GatewayPersistenceContextFilter} under bean
     * name {@code gatewayPersistenceContextComponent}.
     * @param persistenceProperties 参数 已绑定并校验的部署持久化身份；parameter the bound and validated deployment identity.
     * @return 返回 单企业持久化上下文组件；returns the single-enterprise persistence context component.
     */
    @Bean("gatewayPersistenceContextComponent")
    public GatewayPersistenceContextComponent gatewayPersistenceContextComponent(
            @Qualifier(GatewayPersistenceProperties.BEAN_NAME)
            GatewayPersistenceProperties persistenceProperties) {
        Boolean managedDdlEnabled = persistenceProperties.getManagedDdlEnabled();
        if (managedDdlEnabled == null) {
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_FLAG_REQUIRED");
        }
        if (managedDdlEnabled.booleanValue() != mybatisPlusProperties.getDdl().isEnabled()) {
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_FLAG_CONFLICT");
        }
        if (!managedDdlEnabled) {
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_ROLE_MISMATCH");
        }
        return new GatewayPersistenceContextComponent(persistenceProperties, mybatisPlusProperties);
    }

    /**
     * 中文说明：把持久化身份过滤器注册到认证链之后（{@link GatewayPersistenceContextFilter#FILTER_ORDER}）。
     * 过滤器本身不是 bean，避免 Boot 以默认顺序重复注册而在认证之前执行。
     * English summary: Registers the persistence identity filter after the authentication chain
     * ({@link GatewayPersistenceContextFilter#FILTER_ORDER}). The filter instance is deliberately not a bean of its own,
     * so Boot cannot also register it at the default order and run it before authentication.
     *
     * 用法 / Usage: 由 Servlet 容器消费。/ Consumed by the servlet container.
     * @param persistenceContext 参数 部署持久化上下文；parameter the deployment persistence context.
     * @return 返回 显式顺序的过滤器注册；returns the explicitly ordered filter registration.
     */
    @Bean("gatewayPersistenceContextFilterRegistration")
    public FilterRegistrationBean<GatewayPersistenceContextFilter> gatewayPersistenceContextFilterRegistration(
            @Qualifier("gatewayPersistenceContextComponent")
            GatewayPersistenceContextComponent persistenceContext) {
        FilterRegistrationBean<GatewayPersistenceContextFilter> registration = new FilterRegistrationBean<>(
                new GatewayPersistenceContextFilter(persistenceContext));
        registration.setName("gatewayPersistenceContextFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(GatewayPersistenceContextFilter.FILTER_ORDER);
        return registration;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String[] dataSources = beanFactory.getBeanNamesForType(DataSource.class, false, false);
        String[] factories = beanFactory.getBeanNamesForType(SqlSessionFactory.class, false, false);
        String[] managers = beanFactory.getBeanNamesForType(PlatformTransactionManager.class, false, false);
        if (dataSources.length != 1 || factories.length != 1 || managers.length != 1
                || !beanFactory.containsBean(DATA_SOURCE_BEAN_NAME)
                || !beanFactory.containsBean(TRANSACTION_MANAGER_BEAN_NAME)
                || beanFactory.containsBean(LEGACY_ENTITY_MANAGER_FACTORY_BEAN_NAME)
                || beanFactory.containsBean(LEGACY_FLYWAY_BEAN_NAME)) {
            log.error("admin persistence runtime must expose exactly one dataSource/SqlSessionFactory/transaction "
                            + "manager and no JPA or Flyway runtime, found dataSources={} sqlSessionFactory={} "
                            + "transactionManagers={}",
                    Arrays.toString(dataSources), Arrays.toString(factories), Arrays.toString(managers));
            throw new EgonColaMybatisPlusConfigurationException("PERSISTENCE_RUNTIME_NOT_SINGLE");
        }
    }
}
