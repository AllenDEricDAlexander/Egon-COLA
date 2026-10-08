package top.egon.cola.platform.tianquan.shoubing.admin.resource.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.MpResourceServerStore;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.DdcResourceServerLifecycleDeliveryHandler;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.ResourceServerProjectionDeliveryHandler;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore;

import java.time.Clock;

/**
 * Resource Server 管理域的运行态投影装配。
 *
 * <p>Runtime projection wiring for Resource Server administration.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ResourceServerConfig {

    /**
     * 创建 Resource Server 与 Client Grant 的领域查询端口。
     *
     * <p>Creates the domain lookup port for Resource Servers and Client Grants.</p>
     *
     * @param resources Resource Server 仓储；Resource Server repository
     * @param grants Client Resource Grant 仓储；Client Resource Grant repository
     * @param objectMapper JSON 编解码器；JSON codec
     * @return Resource Server 查询端口；Resource Server lookup port
     */
    @Bean
    ResourceServerStore resourceServerStore(
            @Qualifier("oAuthResourceRepository") OAuthResourceRepository resources,
            @Qualifier("oAuthGrantRepository") OAuthGrantRepository grants,
            @Qualifier("oAuthClientRepository") OAuthClientRepository clients,
            @Qualifier("tenantIssuerService") TenantIssuerService tenantIssuerService,
            @Qualifier("oauthManagementRealmResolver") OAuthManagementRealmResolver realmResolver
    ) {
        return new MpResourceServerStore(resources, grants, clients, tenantIssuerService, realmResolver);
    }

    /**
     * 创建 Redis 运行态投影服务。
     *
     * <p>Creates the Redis runtime projection service.</p>
     *
     * @param redisson 身份运行态 Redis 客户端；identity-runtime Redis client
     * @param objectMapper JSON 编解码器；JSON codec
     * @return Resource Server 投影服务；Resource Server projection service
     */
    @Bean
    ResourceServerProjectionService resourceServerProjectionService(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper
    ) {
        return new ResourceServerProjectionService(redisson, objectMapper);
    }

    /**
     * 创建 Resource Server 生命周期事务事件适配器。
     * / Creates the Resource Server lifecycle transactional-event adapter.
     *
     * @param outbox 标准事务发件箱 / standard transactional outbox
     * @param clock Tianquan-Shoubing UTC 业务时钟 / Tianquan-Shoubing UTC business clock
     * @return Resource Server 事件适配器 / Resource Server event adapter
     */
    @Bean
    TransactionalOutboxResourceServerEventAdapter resourceServerEventAdapter(
            @Qualifier("transactionalOutbox") TransactionalOutbox outbox,
            @Qualifier("idpClock") Clock clock) {
        return new TransactionalOutboxResourceServerEventAdapter(
                outbox,
                clock
        );
    }

    /**
     * 创建按投递临时持有 Direct RPC 客户端的 Tianshu 生命周期投递器。
     * / Creates the Tianshu lifecycle handler that owns a Direct RPC client only for one delivery.
     *
     * @param factory Tianshu Direct RPC 客户端工厂 / Tianshu Direct RPC client factory
     * @param objectMapper JSON 编解码器 / JSON codec
     * @return Resource 停用投递器 / Resource-disabled delivery handler
     */
    @Bean
    DdcResourceServerLifecycleDeliveryHandler
            ddcResourceServerLifecycleDeliveryHandler(
                    @Qualifier("ddcRpcClientFactory") DdcRpcClientFactory factory,
                    @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper) {
        return new DdcResourceServerLifecycleDeliveryHandler(
                request -> {
                    try (var handle = factory.managementClient()) {
                        return handle.client().revokeResourceAdmission(
                                request
                        );
                    }
                },
                objectMapper
        );
    }

    /** Creates the post-commit, authoritative Resource/Grant Redis projection consumer. */
    @Bean
    ResourceServerProjectionDeliveryHandler resourceServerProjectionDeliveryHandler(
            @Qualifier("oAuthResourceRepository") OAuthResourceRepository resources,
            @Qualifier("oAuthClientRepository") OAuthClientRepository clients,
            @Qualifier("oAuthGrantRepository") OAuthGrantRepository grants,
            @Qualifier("oauthManagementRealmResolver") OAuthManagementRealmResolver realms,
            @Qualifier("resourceServerProjectionService") ResourceServerProjectionService projections,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper) {
        return new ResourceServerProjectionDeliveryHandler(resources, clients, grants, realms, projections,
                objectMapper);
    }

}
