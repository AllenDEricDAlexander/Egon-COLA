package top.egon.cola.component.yuheng.admin.catalog.repository.mp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.yuheng.admin.catalog.dao.GatewayOperationDefinitionDAO;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationDefinitionRecordPO;

/**
 * gateway_operation_definition 的受守卫持久化边界，复用 EgonColaRepository 的租户过滤、活跃读取与乐观锁 CAS 语义。
 * Guarded persistence boundary for gateway_operation_definition; reuses EgonColaRepository tenant filtering, active reads and CAS writes.
 * 用法 / Usage: 业务代码仅经继承的 save/updateById/removeById 及具名查询访问，禁止裸 Wrapper 绕过守卫。
 */
@Slf4j
@Repository("gatewayOperationDefinitionPersistenceRepository")
@RequiredArgsConstructor
public class GatewayOperationDefinitionPersistenceRepository extends EgonColaRepository<GatewayOperationDefinitionDAO, GatewayOperationDefinitionRecordPO> {

    @Qualifier("gatewayOperationDefinitionDAO")
    private final GatewayOperationDefinitionDAO mapper;

    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public GatewayOperationDefinitionDAO getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }
}
