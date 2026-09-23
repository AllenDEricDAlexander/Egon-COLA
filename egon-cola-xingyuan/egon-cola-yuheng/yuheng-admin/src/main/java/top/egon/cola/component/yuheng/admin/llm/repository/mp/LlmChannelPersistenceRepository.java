package top.egon.cola.component.yuheng.admin.llm.repository.mp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.yuheng.admin.llm.dao.LlmChannelDAO;
import top.egon.cola.component.yuheng.admin.llm.domain.po.LlmChannelPO;

/**
 * gateway_llm_channel 的受守卫持久化边界，复用 EgonColaRepository 的租户过滤、活跃读取与乐观锁 CAS 语义。
 * Guarded persistence boundary for gateway_llm_channel; reuses EgonColaRepository tenant filtering, active reads and CAS writes.
 * 用法 / Usage: 业务代码仅经继承的 save/updateById/removeById 及具名查询访问，禁止裸 Wrapper 绕过守卫。
 */
@Slf4j
@Repository("llmChannelPersistenceRepository")
@RequiredArgsConstructor
public class LlmChannelPersistenceRepository extends EgonColaRepository<LlmChannelDAO, LlmChannelPO> {

    @Qualifier("llmChannelDAO")
    private final LlmChannelDAO mapper;

    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public LlmChannelDAO getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }
}
