package top.egon.cola.component.yuheng.admin.release.repository.mp;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.yuheng.admin.release.dao.GatewayReleaseContentDAO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseContentPO;

/**
 * 中文说明：GatewayReleaseContentPersistenceRepository 是 gateway_release_content 表的受守卫 MP 持久化边界，复用 EgonColaRepository 的租户校验、乐观锁与审计填充终态方法。
 * English summary: Guarded MyBatis-Plus persistence boundary for the gateway_release_content table; inherits the final tenant-checked, version-guarded CRUD from EgonColaRepository.
 *
 * 用法 / Usage: 由所属模块的应用服务注入使用；影响 0 行的写命令按布尔/行数如实返回，不伪造成功。
 * Injected by the owning module's application services; a write affecting 0 rows is returned truthfully and never faked as success.
 */
@Slf4j
@Repository("gatewayReleaseContentPersistenceRepository")
@RequiredArgsConstructor
public class GatewayReleaseContentPersistenceRepository extends EgonColaRepository<GatewayReleaseContentDAO, GatewayReleaseContentPO> {

    @Getter
    @Qualifier("gatewayReleaseContentDAO")
    private final GatewayReleaseContentDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
}
