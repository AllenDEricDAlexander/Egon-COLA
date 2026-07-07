package top.egon.cola.component.ddc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.util.IdUtils;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigQueryRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigRollbackRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigUpdateRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcOperationLogEntity;
import top.egon.cola.component.ddc.admin.model.enums.ChangeType;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVersionVO;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcOperationLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class DdcConfigService {

    private final DdcConfigItemRepository configItemRepository;

    private final DdcConfigVersionRepository versionRepository;

    private final DdcOperationLogRepository operationLogRepository;

    public DdcConfigService(DdcConfigItemRepository configItemRepository,
                            DdcConfigVersionRepository versionRepository,
                            DdcOperationLogRepository operationLogRepository) {
        this.configItemRepository = configItemRepository;
        this.versionRepository = versionRepository;
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional
    public DdcConfigVO create(DdcConfigCreateRequest request, String operator) {
        requireText(request.getAppCode(), "appCode");
        requireText(request.getEnv(), "env");
        requireText(request.getNamespace(), "namespace");
        requireText(request.getConfigKey(), "configKey");
        requireText(request.getValueType(), "valueType");
        configItemRepository.findByAppCodeAndEnvAndNamespaceAndConfigKey(
                        request.getAppCode(), request.getEnv(), request.getNamespace(), request.getConfigKey())
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .ifPresent(item -> {
                    throw new DdcAdminException("config item already exists");
                });

        LocalDateTime now = LocalDateTime.now();
        DdcConfigItemEntity entity = new DdcConfigItemEntity();
        entity.setId(IdUtils.simpleUuid());
        entity.setAppCode(request.getAppCode());
        entity.setEnv(request.getEnv());
        entity.setNamespace(request.getNamespace());
        entity.setConfigKey(request.getConfigKey());
        entity.setConfigValue(request.getConfigValue());
        entity.setDefaultValue(request.getDefaultValue());
        entity.setValueType(request.getValueType());
        entity.setDescription(request.getDescription());
        entity.setCurrentVersion(1L);
        entity.setEnabled(true);
        entity.setDeleted(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        DdcConfigItemEntity saved = configItemRepository.save(entity);
        saveVersion(saved, null, saved.getConfigValue(), ChangeType.CREATE, "create config", operator);
        saveOperation(saved, ChangeType.CREATE, operator, "create config");
        return DdcConfigVO.from(saved);
    }

    @Transactional
    public DdcConfigVO update(DdcConfigUpdateRequest request, String operator) {
        DdcConfigItemEntity entity = getConfig(request.getId());
        if (request.getCurrentVersion() != null && !Objects.equals(request.getCurrentVersion(), entity.getCurrentVersion())) {
            throw new DdcAdminException("config version changed");
        }
        String oldValue = entity.getConfigValue();
        entity.setConfigValue(request.getConfigValue());
        entity.setCurrentVersion(entity.getCurrentVersion() + 1);
        entity.setUpdatedAt(LocalDateTime.now());
        DdcConfigItemEntity saved = configItemRepository.save(entity);
        saveVersion(saved, oldValue, saved.getConfigValue(), ChangeType.UPDATE, request.getChangeReason(), operator);
        saveOperation(saved, ChangeType.UPDATE, operator, request.getChangeReason());
        return DdcConfigVO.from(saved);
    }

    @Transactional
    public DdcConfigVO delete(String configId, String operator, String reason) {
        DdcConfigItemEntity entity = getConfig(configId);
        String oldValue = entity.getConfigValue();
        entity.setDeleted(true);
        entity.setEnabled(false);
        entity.setCurrentVersion(entity.getCurrentVersion() + 1);
        entity.setUpdatedAt(LocalDateTime.now());
        DdcConfigItemEntity saved = configItemRepository.save(entity);
        saveVersion(saved, oldValue, null, ChangeType.DELETE, reason, operator);
        saveOperation(saved, ChangeType.DELETE, operator, reason);
        return DdcConfigVO.from(saved);
    }

    @Transactional
    public DdcConfigVO rollback(DdcConfigRollbackRequest request, String operator) {
        DdcConfigItemEntity entity = getConfig(request.getConfigId());
        DdcConfigVersionEntity target = versionRepository.findByConfigIdAndVersion(request.getConfigId(), request.getVersion())
                .orElseThrow(() -> new DdcAdminException("config version not found"));
        String oldValue = entity.getConfigValue();
        entity.setConfigValue(target.getNewValue());
        entity.setDeleted(false);
        entity.setEnabled(true);
        entity.setCurrentVersion(entity.getCurrentVersion() + 1);
        entity.setUpdatedAt(LocalDateTime.now());
        DdcConfigItemEntity saved = configItemRepository.save(entity);
        saveVersion(saved, oldValue, saved.getConfigValue(), ChangeType.ROLLBACK, request.getReason(), operator);
        saveOperation(saved, ChangeType.ROLLBACK, operator, request.getReason());
        return DdcConfigVO.from(saved);
    }

    public List<DdcConfigVO> list(DdcConfigQueryRequest request) {
        List<DdcConfigItemEntity> items = configItemRepository.findByAppCodeAndEnvAndNamespace(
                request.getAppCode(), request.getEnv(), request.getNamespace());
        return items.stream()
                .filter(item -> request.isIncludeDeleted() || !Boolean.TRUE.equals(item.getDeleted()))
                .filter(item -> request.getConfigKey() == null || item.getConfigKey().contains(request.getConfigKey()))
                .map(DdcConfigVO::from)
                .toList();
    }

    public List<DdcConfigVersionVO> versions(String configId) {
        return versionRepository.findByConfigIdOrderByVersionDesc(configId).stream()
                .map(DdcConfigVersionVO::from)
                .toList();
    }

    private DdcConfigItemEntity getConfig(String configId) {
        return configItemRepository.findById(configId)
                .orElseThrow(() -> new DdcAdminException("config item not found"));
    }

    private void saveVersion(DdcConfigItemEntity entity, String oldValue, String newValue, ChangeType changeType,
                             String reason, String operator) {
        DdcConfigVersionEntity version = new DdcConfigVersionEntity();
        version.setId(IdUtils.simpleUuid());
        version.setConfigId(entity.getId());
        version.setAppCode(entity.getAppCode());
        version.setEnv(entity.getEnv());
        version.setNamespace(entity.getNamespace());
        version.setConfigKey(entity.getConfigKey());
        version.setVersion(entity.getCurrentVersion());
        version.setOldValue(oldValue);
        version.setNewValue(newValue);
        version.setValueType(entity.getValueType());
        version.setChangeType(changeType.name());
        version.setChangeReason(reason);
        version.setOperator(operator);
        version.setCreatedAt(LocalDateTime.now());
        versionRepository.save(version);
    }

    private void saveOperation(DdcConfigItemEntity entity, ChangeType changeType, String operator, String content) {
        DdcOperationLogEntity log = new DdcOperationLogEntity();
        log.setId(IdUtils.simpleUuid());
        log.setAppCode(entity.getAppCode());
        log.setEnv(entity.getEnv());
        log.setNamespace(entity.getNamespace());
        log.setConfigKey(entity.getConfigKey());
        log.setOperationType(changeType.name());
        log.setOperator(operator);
        log.setOperationContent(content);
        log.setCreatedAt(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DdcAdminException(fieldName + " is required");
        }
    }
}
