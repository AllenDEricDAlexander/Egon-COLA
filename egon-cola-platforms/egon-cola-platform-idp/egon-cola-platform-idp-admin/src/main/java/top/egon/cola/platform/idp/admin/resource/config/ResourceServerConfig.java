package top.egon.cola.platform.idp.admin.resource.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.resource.repo.JpaResourceServerStore;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService;
import top.egon.cola.platform.idp.admin.resource.support.outbox.DdcResourceServerLifecycleDeliveryHandler;
import top.egon.cola.platform.idp.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;

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
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            ObjectMapper objectMapper
    ) {
        return new JpaResourceServerStore(resources, grants, objectMapper);
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
            ObjectMapper objectMapper
    ) {
        return new ResourceServerProjectionService(redisson, objectMapper);
    }

    /**
     * 创建 Resource Server 生命周期事务事件适配器。
     * / Creates the Resource Server lifecycle transactional-event adapter.
     *
     * @param outbox 标准事务发件箱 / standard transactional outbox
     * @param clock IdP UTC 业务时钟 / IdP UTC business clock
     * @return Resource Server 事件适配器 / Resource Server event adapter
     */
    @Bean
    TransactionalOutboxResourceServerEventAdapter resourceServerEventAdapter(
            TransactionalOutbox outbox,
            @Qualifier("idpClock") Clock clock) {
        return new TransactionalOutboxResourceServerEventAdapter(
                outbox,
                clock
        );
    }

    /**
     * 创建按投递临时持有 Direct RPC 客户端的 DDC 生命周期投递器。
     * / Creates the DDC lifecycle handler that owns a Direct RPC client only for one delivery.
     *
     * @param factory DDC Direct RPC 客户端工厂 / DDC Direct RPC client factory
     * @param objectMapper JSON 编解码器 / JSON codec
     * @return Resource 停用投递器 / Resource-disabled delivery handler
     */
    @Bean
    DdcResourceServerLifecycleDeliveryHandler
            ddcResourceServerLifecycleDeliveryHandler(
                    DdcRpcClientFactory factory,
                    ObjectMapper objectMapper) {
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

}
