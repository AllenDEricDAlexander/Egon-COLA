package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.tianshu.admin.dao.DdcOperationLogMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcOperationLogBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcOperationLogConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcOperationLogPO;
import top.egon.cola.component.tianshu.admin.repository.DdcOperationLogRepository;

import java.util.List;

@Slf4j
@Repository("ddcOperationLogRepository")
@RequiredArgsConstructor
@Validated
public class DdcOperationLogRepositoryImpl extends EgonColaRepository<DdcOperationLogMapper, DdcOperationLogPO>
        implements DdcOperationLogRepository {

    @Qualifier("ddcOperationLogMapper") private final DdcOperationLogMapper mapper;
    @Qualifier("ddcOperationLogConverterImpl") private final DdcOperationLogConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcOperationLogMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }

    @Override public List<DdcOperationLogBO> findByBizCodeAndEnvAndAppCode(
            String bizCode, String env, String appCode) {
        return converter.toTargetList(mapper.selectByScope(
                EgonColaTenantIdProvider.currentTenantId(), bizCode, env, appCode));
    }

    @Override public DdcOperationLogBO insert(DdcOperationLogBO command) {
        DdcOperationLogPO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id).setTenantId(EgonColaTenantIdProvider.currentTenantId());
        if (row.getPublicId() == null || row.getPublicId().isBlank()) row.setPublicId(Long.toString(id));
        if (!save(row)) throw new IllegalStateException("TIANSHU_OPERATION_LOG_INSERT_NOT_APPLIED");
        return converter.toTarget(row);
    }
}
