package top.egon.cola.platform.idp.admin.resource.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.resource.repo.JpaResourceServerStore;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService;
import top.egon.cola.platform.idp.admin.resource.service.impl.ResourceServerAdmissionServiceImpl;
import top.egon.cola.platform.idp.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.idp.core.port.ClientAssertionReplayStore;
import top.egon.cola.platform.idp.core.port.ClientCredentialStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.resource.ResourceServerAdmissionPolicy;

import java.net.URI;
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
     * 创建只接受 Admission Endpoint Audience 的 Client Assertion 认证器。
     *
     * <p>Creates the Client Assertion authenticator accepting only the Admission Endpoint
     * audience.</p>
     *
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param credentials Client JWK 查询端口；Client JWK lookup port
     * @param replays Assertion 防重放端口；assertion replay-prevention port
     * @param issuer IdP Issuer；IdP issuer
     * @param clock UTC 业务时钟；UTC business clock
     * @return Admission Endpoint 专用认证器；Admission Endpoint-specific authenticator
     */
    @Bean(name = "resourceServerAdmissionAuthenticator")
    PrivateKeyJwtAuthenticator resourceServerAdmissionAuthenticator(
            OAuthClientStore clients,
            ClientCredentialStore credentials,
            ClientAssertionReplayStore replays,
            @Value("${egon.idp.oauth.issuer}") String issuer,
            @Qualifier("idpClock") Clock clock
    ) {
        return new PrivateKeyJwtAuthenticator(
                clients,
                credentials,
                replays,
                admissionEndpoint(issuer),
                clock
        );
    }

    /**
     * 创建集中表达 Resource Server 启动准入规则的领域策略。
     *
     * <p>Creates the domain policy centralizing Resource Server startup-admission rules.</p>
     *
     * @return Resource Server 准入策略；Resource Server admission policy
     */
    @Bean
    ResourceServerAdmissionPolicy resourceServerAdmissionPolicy() {
        return new ResourceServerAdmissionPolicy();
    }

    /**
     * 创建 Resource Server Admission Ticket 签发服务。
     *
     * <p>Creates the Resource Server Admission Ticket issuance service.</p>
     *
     * @param authenticator Admission Endpoint 专用认证器；Admission Endpoint authenticator
     * @param clients OAuth Client 查询端口；OAuth Client lookup port
     * @param credentials Client JWK 查询端口；Client JWK lookup port
     * @param resources Resource Server 查询端口；Resource Server lookup port
     * @param policy 准入领域策略；admission domain policy
     * @param signer RS256 Token 服务；RS256 token service
     * @param clock UTC 业务时钟；UTC business clock
     * @param ids 全局 ID 生成器；global ID generator
     * @return Admission Ticket 签发服务；Admission Ticket issuance service
     */
    @Bean
    ResourceServerAdmissionServiceImpl resourceServerAdmissionService(
            @Qualifier("resourceServerAdmissionAuthenticator")
            PrivateKeyJwtAuthenticator authenticator,
            OAuthClientStore clients,
            ClientCredentialStore credentials,
            ResourceServerStore resources,
            ResourceServerAdmissionPolicy policy,
            Rs256TokenService signer,
            @Qualifier("idpClock") Clock clock,
            LongIdGenerator ids
    ) {
        return new ResourceServerAdmissionServiceImpl(
                authenticator,
                clients,
                credentials,
                resources,
                policy,
                signer,
                clock,
                ids::nextId
        );
    }

    /**
     * 从 Issuer 构建精确 Admission Endpoint URI。
     *
     * <p>Builds the exact Admission Endpoint URI from the issuer.</p>
     *
     * @param issuer IdP Issuer；IdP issuer
     * @return Admission Endpoint URI；Admission Endpoint URI
     */
    private static URI admissionEndpoint(String issuer) {
        String value = issuer.endsWith("/")
                ? issuer.substring(0, issuer.length() - 1)
                : issuer;
        return URI.create(value + "/oauth2/resource-server-admission");
    }
}
