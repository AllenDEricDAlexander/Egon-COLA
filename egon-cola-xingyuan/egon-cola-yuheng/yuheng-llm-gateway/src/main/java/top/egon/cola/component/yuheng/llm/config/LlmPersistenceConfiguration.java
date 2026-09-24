package top.egon.cola.component.yuheng.llm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 中文说明：{@code LlmPersistenceConfiguration} 是 LLM 数据面（只读消费方）的持久化身份装配点：绑定并校验
 * {@code yuheng.persistence.*} 部署身份，然后断言进程内唯一数据源、唯一 {@code SqlSessionFactory}、唯一事务管理器且
 * 无 JPA/Flyway 运行期。本类<b>不</b>声明 {@code DataSource}、{@code SqlSessionFactory}、
 * {@code MybatisPlusInterceptor} 或第二个事务管理器，也绝不执行 DDL：连接池归组件的
 * {@code EgonColaShardingAutoConfiguration} 所有，只读角色另由数据库账号授权约束，代码里再造一个池只会绕过守卫。
 * 身份缺项、空白、非正数由 {@link LlmPersistenceProperties} 的约束在绑定阶段失败关闭，没有默认租户。
 * English summary: {@code LlmPersistenceConfiguration} is the persistence identity wiring point of the LLM data plane, a
 * read-only consumer: it binds and validates the {@code yuheng.persistence.*} deployment identity and then asserts one
 * datasource, one {@code SqlSessionFactory}, one transaction manager and no JPA/Flyway runtime in the process. This
 * class declares <b>no</b> {@code DataSource}, {@code SqlSessionFactory}, {@code MybatisPlusInterceptor} or second
 * transaction manager and never runs DDL: the component's {@code EgonColaShardingAutoConfiguration} owns the pool and the
 * read-only role is additionally enforced by database grants, so rebuilding a pool in code would only bypass the guards.
 * Missing, blank or non-positive identity values fail closed at binding through {@link LlmPersistenceProperties}
 * constraints; there is no default tenant.
 *
 * 用法 / Usage: 由宿主装配消费；本进程被配成 DDL 持有者或与组件侧 DDL 开关不一致时拒绝启动。
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(value = "llmPersistenceConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(LlmPersistenceProperties.class)
public class LlmPersistenceConfiguration implements SmartInitializingSingleton {

    /** 中文说明：组件唯一数据源的 bean 名，只读路径同样只能走它。 English summary: bean name of the component's single datasource, which the read-only path must also use. */
    private static final String DATA_SOURCE_BEAN_NAME = "dataSource";

    /** 中文说明：被移除的旧 JPA 运行期 bean 名，出现即说明有依赖把第二套 ORM 带了回来。 English summary: bean name of the removed JPA runtime; its presence means a dependency reintroduced a second ORM. */
    private static final String LEGACY_ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";

    /** 中文说明：只读消费方禁止持有的 Flyway 运行期 bean 名；建表只属于 Admin。 English summary: the Flyway runtime bean name a read-only consumer must never hold; schema creation belongs to Admin alone. */
    private static final String LEGACY_FLYWAY_BEAN_NAME = "flyway";

    /** 中文说明：组件侧持久化开关，用于证明本进程没有 claim 受管 DDL 职责。 English summary: component-side persistence switches, used to prove this process claims no managed-DDL duty. */
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties mybatisPlusProperties;

    /** 中文说明：容器内省入口，仅用于断言单数据源拓扑。 English summary: container introspection entry, used only to assert the single-datasource topology. */
    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * 中文说明：注册 LLM 的部署身份上下文，并在此校验只读消费方角色：{@code managed-ddl-enabled} 必须为假，组件侧
     * {@code egon.cola.component.mybatis-plus.ddl.enabled} 也必须为假。校验落在这个无条件 bean 方法上，条件化 bean
     * 可能整体退场而让角色检查被静默跳过。
     * English summary: Registers LLM's deployment identity context and validates the read-only consumer role here:
     * {@code managed-ddl-enabled} must be false and the component's
     * {@code egon.cola.component.mybatis-plus.ddl.enabled} must be false as well. The check sits on this unconditional
     * bean method because a conditional bean could back off and skip role validation silently.
     *
     * 用法 / Usage: 由读路径适配器按 bean 名 {@code llmPersistenceContextComponent} 注入。
     * @param persistenceProperties 参数 已绑定并校验的部署持久化身份；parameter the bound and validated deployment identity.
     * @return 返回 LLM 持久化上下文组件；returns the LLM persistence context component.
     */
    @Bean("llmPersistenceContextComponent")
    public LlmPersistenceContextComponent llmPersistenceContextComponent(
            @Qualifier(LlmPersistenceProperties.BEAN_NAME)
            LlmPersistenceProperties persistenceProperties) {
        if (persistenceProperties.isManagedDdlEnabled() != mybatisPlusProperties.getDdl().isEnabled()) {
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_FLAG_CONFLICT");
        }
        if (persistenceProperties.isManagedDdlEnabled()) {
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_ROLE_MISMATCH");
        }
        return new LlmPersistenceContextComponent(persistenceProperties, mybatisPlusProperties);
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
            log.error("llm consumer must ride exactly one dataSource/SqlSessionFactory/transaction manager and run no "
                            + "JPA or Flyway runtime, found dataSources={} sqlSessionFactory={} transactionManagers={}",
                    Arrays.toString(dataSources), Arrays.toString(factories), Arrays.toString(managers));
            throw new EgonColaMybatisPlusConfigurationException("PERSISTENCE_RUNTIME_NOT_SINGLE");
        }
    }
}
