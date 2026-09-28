package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.tianshu.admin.dao.DdcPublishAckMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishAckBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcPublishAckConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcPublishAckPO;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcPublishAckRepository")
@RequiredArgsConstructor
@Validated
public class DdcPublishAckRepositoryImpl extends EgonColaRepository<DdcPublishAckMapper, DdcPublishAckPO>
        implements DdcPublishAckRepository {

    @Qualifier("ddcPublishAckMapper") private final DdcPublishAckMapper mapper;
    @Qualifier("ddcPublishAckConverterImpl") private final DdcPublishAckConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcPublishAckMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }
    private Long tenantId() { return EgonColaTenantIdProvider.currentTenantId(); }
    private String actor() {
        String actor = MDC.get("userId");
        if (actor == null || actor.isBlank()) throw new IllegalStateException("TIANSHU_AUDIT_ACTOR_REQUIRED");
        return actor;
    }

    @Override public Optional<DdcPublishAckBO> findByChangeIdAndInstanceIdAndLeaseId(
            String changeId, String instanceId, String leaseId) {
        return Optional.ofNullable(mapper.selectByChangeIdAndInstanceIdAndLeaseId(
                tenantId(), changeId, instanceId, leaseId)).map(converter::toTarget);
    }
    @Override public Optional<DdcPublishAckBO> findForUpdateByChangeIdAndInstanceIdAndLeaseId(
            String changeId, String instanceId, String leaseId) {
        return Optional.ofNullable(mapper.selectForUpdateByChangeIdAndInstanceIdAndLeaseId(
                tenantId(), changeId, instanceId, leaseId)).map(converter::toTarget);
    }
    @Override public List<DdcPublishAckBO> findByChangeId(String changeId) {
        return converter.toTargetList(mapper.selectByChangeId(tenantId(), changeId));
    }
    @Override public int markIncompleteTimeout(String changeId, String timeoutStatus, LocalDateTime ackAt) {
        return mapper.markIncompleteTimeout(tenantId(), changeId, timeoutStatus, ackAt, actor());
    }
    @Override public int resetTargets(String changeId) {
        return mapper.resetTargets(tenantId(), changeId, actor());
    }
    @Override public DdcPublishAckBO insert(DdcPublishAckBO command) {
        DdcPublishAckPO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id).setTenantId(tenantId());
        if (row.getPublicId() == null || row.getPublicId().isBlank()) row.setPublicId(Long.toString(id));
        if (!save(row)) throw new IllegalStateException("TIANSHU_PUBLISH_ACK_INSERT_NOT_APPLIED");
        return converter.toTarget(row);
    }
    @Override public List<DdcPublishAckBO> insertAll(List<DdcPublishAckBO> commands) {
        if (commands == null || commands.isEmpty()) return List.of();
        Long tenant = tenantId();
        List<DdcPublishAckPO> rows = commands.stream().map(converter::toSource).toList();
        for (DdcPublishAckPO row : rows) {
            long id = identifierGenerator.nextId(row);
            row.setId(id).setTenantId(tenant);
            if (row.getPublicId() == null || row.getPublicId().isBlank()) row.setPublicId(Long.toString(id));
        }
        if (!saveBatch(rows, properties.getBatch().getDefaultSize())) {
            throw new IllegalStateException("TIANSHU_PUBLISH_ACK_BATCH_INSERT_NOT_APPLIED");
        }
        return converter.toTargetList(rows);
    }
    @Override public DdcPublishAckBO update(DdcPublishAckBO command) {
        DdcPublishAckPO row = converter.toSource(command);
        if (!updateById(row)) throw new IllegalStateException("TIANSHU_PUBLISH_ACK_VERSION_CHANGED");
        return converter.toTarget(row);
    }
}
