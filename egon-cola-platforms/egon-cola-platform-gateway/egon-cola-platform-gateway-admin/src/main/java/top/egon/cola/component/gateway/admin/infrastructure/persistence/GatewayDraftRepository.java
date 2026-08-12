package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 中文说明：{@code GatewayDraftRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关草稿Repository相关的职责与边界。
 * English summary: {@code GatewayDraftRepository} is an interface contract in the current Gateway module; it owns the gateway draft repository-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayDraftRepository
        extends JpaRepository<GatewayDraftEntity, String> {
}
