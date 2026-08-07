package top.egon.cola.component.ddc.admin.service.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
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
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DdcConfigService {

    private final DdcConfigItemRepository configItemRepository;

    private final DdcConfigVersionRepository versionRepository;

    private final DdcOperationLogRepository operationLogRepository;

    private final DdcNamespaceEnvAppBindingRepository bindingRepository;

    private final DdcConfigValueGuard valueGuard;

    @Autowired
    public DdcConfigService(
            DdcConfigItemRepository configItemRepository,
            DdcConfigVersionRepository versionRepository,
            DdcOperationLogRepository operationLogRepository,
            ObjectProvider<DdcAdminProperties> propertiesProvider,
            DdcNamespaceEnvAppBindingRepository bindingRepository) {
        this.configItemRepository = configItemRepository;
        this.versionRepository = versionRepository;
        this.operationLogRepository = operationLogRepository;
        this.bindingRepository = bindingRepository;
        DdcAdminProperties properties =
                propertiesProvider.getIfAvailable(DdcAdminProperties::new);
        this.valueGuard = new DdcConfigValueGuard(properties.getMaxValueBytes());
    }

    public DdcConfigService(
            DdcConfigItemRepository configItemRepository,
            DdcConfigVersionRepository versionRepository,
            DdcOperationLogRepository operationLogRepository,
            ObjectProvider<DdcAdminProperties> propertiesProvider) {
        this(
                configItemRepository,
                versionRepository,
                operationLogRepository,
                propertiesProvider,
                null
        );
    }

    @Transactional
    public DdcConfigVO create(DdcConfigCreateRequest request, String operator) {
        validateDraft(request);
        configItemRepository.findByBizCodeAndEnvAndAppCodeAndConfigKey(
                        request.getBizCode(), request.getEnv(),
                        request.getAppCode(), request.getConfigKey())
                .ifPresent(item -> {
                    throw new DdcAdminException("config item already exists");
                });
        return createDraft(request, operator);
    }

    @Transactional
    public DdcConfigVO upsert(
            DdcConfigCreateRequest request,
            Long expectedVersion,
            String operator
    ) {
        validateDraft(request);
        DdcConfigItemEntity existing =
                configItemRepository.findByBizCodeAndEnvAndAppCodeAndConfigKey(
                        request.getBizCode(),
                        request.getEnv(),
                        request.getAppCode(),
                        request.getConfigKey()
                ).orElse(null);
        if (existing == null) {
            if (expectedVersion != null && expectedVersion != 0L) {
                throw new DdcAdminException("config version changed");
            }
            return createDraft(request, operator);
        }
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            throw new DdcAdminException("deleted config key cannot be reused");
        }
        if (expectedVersion == null
                || !Objects.equals(expectedVersion, existing.getCurrentVersion())) {
            throw new DdcAdminException("config version changed");
        }
        String oldValue = existing.getConfigValue();
        existing.setConfigValue(request.getConfigValue());
        existing.setValueType(request.getValueType());
        existing.setDescription(request.getDescription());
        existing.setCurrentVersion(existing.getCurrentVersion() + 1);
        existing.setUpdatedAt(LocalDateTime.now());
        DdcConfigItemEntity saved = configItemRepository.save(existing);
        saveVersion(
                saved,
                oldValue,
                saved.getConfigValue(),
                ChangeType.UPDATE,
                "upsert config draft",
                operator
        );
        saveOperation(
                saved,
                ChangeType.UPDATE,
                operator,
                "upsert config draft"
        );
        return DdcConfigVO.from(saved);
    }

    private DdcConfigVO createDraft(
            DdcConfigCreateRequest request,
            String operator
    ) {
        LocalDateTime now = LocalDateTime.now();
        DdcConfigItemEntity entity = new DdcConfigItemEntity();
        entity.setId(UuidV7.simpleString());
        entity.setBizCode(request.getBizCode());
        entity.setAppCode(request.getAppCode());
        entity.setEnv(request.getEnv());
        entity.setConfigKey(request.getConfigKey());
        entity.setConfigValue(request.getConfigValue());
        entity.setDefaultValue(request.getDefaultValue());
        entity.setValueType(request.getValueType());
        entity.setDescription(request.getDescription());
        entity.setCurrentVersion(1L);
        entity.setPublishedVersion(null);
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
        if (request == null) {
            throw new DdcAdminException("config update request is required");
        }
        valueGuard.check(request.getConfigValue());
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
        if (Boolean.TRUE.equals(entity.getDeleted())) {
            return DdcConfigVO.from(entity);
        }
        return delete(entity, operator, reason);
    }

    @Transactional
    public DdcConfigVO delete(
            String bizCode,
            String env,
            String appCode,
            String configKey,
            Long expectedVersion,
            String operator,
            String reason
    ) {
        DdcConfigItemEntity entity =
                configItemRepository.findByBizCodeAndEnvAndAppCodeAndConfigKey(
                        bizCode,
                        env,
                        appCode,
                        configKey
                ).orElse(null);
        if (entity == null) {
            return null;
        }
        if (Boolean.TRUE.equals(entity.getDeleted())) {
            return DdcConfigVO.from(entity);
        }
        if (expectedVersion == null
                || !Objects.equals(expectedVersion, entity.getCurrentVersion())) {
            throw new DdcAdminException("config version changed");
        }
        return delete(entity, operator, reason);
    }

    private DdcConfigVO delete(
            DdcConfigItemEntity entity,
            String operator,
            String reason
    ) {
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
        valueGuard.check(target.getNewValue());
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
        DdcConfigQueryRequest query = request == null
                ? new DdcConfigQueryRequest()
                : request;
        return configItemRepository.search(
                        optional(query.getBizCode()),
                        optional(query.getNamespaceCode()),
                        optional(query.getEnv()),
                        optional(query.getAppCode()),
                        optional(query.getConfigKey()),
                        query.isIncludeDeleted()
                ).stream()
                .map(this::config)
                .toList();
    }

    public DdcConfigVO get(String configId) {
        return DdcConfigVO.from(getConfig(configId));
    }

    public Optional<DdcConfigVO> find(
            String bizCode,
            String env,
            String appCode,
            String configKey
    ) {
        return configItemRepository.findByBizCodeAndEnvAndAppCodeAndConfigKey(
                bizCode,
                env,
                appCode,
                configKey
        ).map(this::config);
    }

    public List<DdcConfigValue> pull(String bizCode, String env, String appCode) {
        return configItemRepository.findByBizCodeAndEnvAndAppCode(
                        bizCode, env, appCode
                ).stream()
                .map(this::publishedVersion)
                .flatMap(Optional::stream)
                .filter(this::isRuntimeValue)
                .map(this::toConfigValue)
                .toList();
    }

    public DdcConfigValue value(
            String bizCode, String env, String appCode, String configKey) {
        return configItemRepository.findByBizCodeAndEnvAndAppCodeAndConfigKey(
                        bizCode, env, appCode, configKey)
                .flatMap(this::publishedVersion)
                .filter(this::isRuntimeValue)
                .map(this::toConfigValue)
                .orElse(null);
    }

    @Transactional
    public void reportDefaults(DdcDefaultReportRequest request) {
        if (request == null || request.getConfigs() == null) {
            return;
        }
        request.getConfigs().forEach(config ->
                valueGuard.check(config.getDefaultValue()));
        for (DdcDefaultReportRequest.DdcConfigValueRequest config : request.getConfigs()) {
            configItemRepository.findByBizCodeAndEnvAndAppCodeAndConfigKey(
                            request.getBizCode(), request.getEnv(),
                            request.getAppCode(), config.getConfigKey())
                    .orElseGet(() -> createDefaultConfig(request, config));
        }
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
        version.setId(UuidV7.simpleString());
        version.setConfigId(entity.getId());
        version.setBizCode(entity.getBizCode());
        version.setAppCode(entity.getAppCode());
        version.setEnv(entity.getEnv());
        version.setNamespace(null);
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
        log.setId(UuidV7.simpleString());
        log.setBizCode(entity.getBizCode());
        log.setAppCode(entity.getAppCode());
        log.setEnv(entity.getEnv());
        log.setNamespace(null);
        log.setConfigKey(entity.getConfigKey());
        log.setOperationType(changeType.name());
        log.setOperator(operator);
        log.setOperationContent(content);
        log.setCreatedAt(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    private DdcConfigItemEntity createDefaultConfig(DdcDefaultReportRequest request,
                                                    DdcDefaultReportRequest.DdcConfigValueRequest config) {
        LocalDateTime now = LocalDateTime.now();
        DdcConfigItemEntity entity = new DdcConfigItemEntity();
        entity.setId(UuidV7.simpleString());
        entity.setBizCode(request.getBizCode());
        entity.setAppCode(request.getAppCode());
        entity.setEnv(request.getEnv());
        entity.setConfigKey(config.getConfigKey());
        entity.setConfigValue(config.getDefaultValue());
        entity.setDefaultValue(config.getDefaultValue());
        entity.setValueType(config.getValueType());
        entity.setCurrentVersion(1L);
        entity.setPublishedVersion(null);
        entity.setEnabled(true);
        entity.setDeleted(false);
        entity.setDescription("reported by " + request.getInstanceId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        DdcConfigItemEntity saved = configItemRepository.save(entity);
        saveVersion(saved, null, saved.getConfigValue(), ChangeType.CREATE, "report default", request.getInstanceId());
        saveOperation(saved, ChangeType.CREATE, request.getInstanceId(), "report default");
        return saved;
    }

    private Optional<DdcConfigVersionEntity> publishedVersion(
            DdcConfigItemEntity entity
    ) {
        if (entity.getPublishedVersion() == null) {
            return Optional.empty();
        }
        return Optional.of(versionRepository.findByConfigIdAndVersion(
                entity.getId(),
                entity.getPublishedVersion()
        ).orElseThrow(() -> new DdcAdminException(
                "published config version not found"
        )));
    }

    private DdcConfigValue toConfigValue(DdcConfigVersionEntity version) {
        DdcConfigValue value = new DdcConfigValue();
        value.setConfigKey(version.getConfigKey());
        value.setConfigValue(version.getNewValue());
        value.setValueType(version.getValueType());
        value.setVersion(version.getVersion());
        return value;
    }

    private boolean isRuntimeValue(DdcConfigVersionEntity version) {
        return !ChangeType.DELETE.name().equals(version.getChangeType());
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DdcAdminException(fieldName + " is required");
        }
    }

    private void validateDraft(DdcConfigCreateRequest request) {
        if (request == null) {
            throw new DdcAdminException("config request is required");
        }
        requireText(request.getBizCode(), "bizCode");
        requireText(request.getAppCode(), "appCode");
        requireText(request.getEnv(), "env");
        requireText(request.getConfigKey(), "configKey");
        requireText(request.getValueType(), "valueType");
        valueGuard.check(request.getConfigValue());
    }

    private DdcConfigVO config(DdcConfigItemEntity entity) {
        DdcConfigVO value = DdcConfigVO.from(entity);
        if (bindingRepository != null) {
            value.setVisibleNamespaces(bindingRepository.findVisibleNamespaceCodes(
                    entity.getBizCode(),
                    entity.getEnv(),
                    entity.getAppCode()
            ));
        }
        return value;
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
