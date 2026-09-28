package top.egon.cola.component.tianshu.admin.service.config;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigQueryRequest;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigRollbackRequest;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigUpdateRequest;
import top.egon.cola.component.tianshu.admin.model.bo.DdcOperationLogBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.enums.ChangeType;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigItemConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigVersionConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcConfigVersionVO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.format.DdcConfigFormatStrategyRegistry;
import top.egon.cola.component.tianshu.format.DdcYamlConfigFormatStrategy;
import top.egon.cola.component.tianshu.model.config.DdcConfigFormat;
import top.egon.cola.component.tianshu.model.config.DdcConfigValue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service("ddcConfigService")
@Validated
public class DdcConfigService {

    public static final String DEFAULT_RESOURCE_NAME = DdcYamlConfigFormatStrategy.DEFAULT_RESOURCE_NAME;
    public static final String YAML_FORMAT = DdcConfigFormat.YAML.name();

    private static final Set<String> SUPPORTED_RESOURCE_NAMES = Set.of(
            DEFAULT_RESOURCE_NAME, DdcYamlConfigFormatStrategy.ALTERNATE_RESOURCE_NAME);
    private static final List<String> PREPARED_PUBLISH_STATUSES = List.of(
            PublishStatus.PUBLISHING.name(), PublishStatus.UNKNOWN.name());

    private final DdcConfigItemRepository configItemRepository;
    private final DdcConfigVersionRepository versionRepository;
    private final DdcConfigItemConverter itemConverter;
    private final DdcConfigVersionConverter versionConverter;
    private final DdcOperationLogRepository operationLogRepository;
    private final DdcNamespaceEnvAppBindingRepository bindingRepository;
    private final DdcPublishTaskRepository publishTaskRepository;
    private final DdcYamlConfigValidator yamlValidator;

    @Autowired
    public DdcConfigService(
            @Qualifier("ddcConfigItemRepository") DdcConfigItemRepository configItemRepository,
            @Qualifier("ddcConfigVersionRepository") DdcConfigVersionRepository versionRepository,
            @Qualifier("ddcConfigItemConverterImpl") DdcConfigItemConverter itemConverter,
            @Qualifier("ddcConfigVersionConverterImpl") DdcConfigVersionConverter versionConverter,
            @Qualifier("ddcOperationLogRepository") DdcOperationLogRepository operationLogRepository,
            @Qualifier("ddcNamespaceEnvAppBindingRepository") DdcNamespaceEnvAppBindingRepository bindingRepository,
            @Qualifier("ddcPublishTaskRepository") DdcPublishTaskRepository publishTaskRepository,
            ObjectProvider<DdcAdminProperties> propertiesProvider) {
        this.configItemRepository = configItemRepository;
        this.versionRepository = versionRepository;
        this.itemConverter = itemConverter;
        this.versionConverter = versionConverter;
        this.operationLogRepository = operationLogRepository;
        this.bindingRepository = bindingRepository;
        this.publishTaskRepository = publishTaskRepository;
        DdcAdminProperties properties = propertiesProvider.getIfAvailable(DdcAdminProperties::new);
        this.yamlValidator = new DdcYamlConfigValidator(properties.getMaxConfigBytes());
    }

    @Transactional
    public DdcConfigVO create(@Valid DdcConfigCreateRequest request, String operator) {
        validateDraft(request);
        findScopeConfig(request.getBizCode(), request.getEnv(), request.getAppCode())
                .ifPresent(item -> { throw new DdcAdminException("config item already exists"); });
        return itemConverter.toConfigVO(createDraft(request, operator));
    }

    @Transactional
    public DdcConfigVO upsert(@Valid DdcConfigCreateRequest request, Long expectedVersion, String operator) {
        validateDraft(request);
        DdcConfigItemBO existing = findScopeConfig(request.getBizCode(), request.getEnv(), request.getAppCode())
                .orElse(null);
        if (existing == null) {
            if (expectedVersion != null && expectedVersion != 0L) throw new DdcAdminException("config version changed");
            return itemConverter.toConfigVO(createDraft(request, operator));
        }
        if (Boolean.TRUE.equals(existing.getDeleted())) throw new DdcAdminException("deleted config resource cannot be reused");
        if (!Objects.equals(request.getResourceName(), existing.getResourceName())
                || !Objects.equals(request.getFormat(), existing.getFormat())) {
            throw new DdcAdminException("config resource name and format cannot be changed");
        }
        if (expectedVersion == null || !Objects.equals(expectedVersion, existing.getCurrentVersion()))
            throw new DdcAdminException("config version changed");

        String oldContent = existing.getContent();
        existing.setContent(request.getContent()).setDefaultValue(null).setDescription(request.getDescription())
                .setCurrentVersion(existing.getCurrentVersion() + 1).setUpdatedAt(LocalDateTime.now());
        DdcConfigItemBO saved = configItemRepository.update(existing);
        saveVersion(saved, oldContent, saved.getContent(), ChangeType.UPDATE, "upsert config draft", operator);
        saveOperation(saved, ChangeType.UPDATE, operator, "upsert config draft");
        return itemConverter.toConfigVO(saved);
    }

    private DdcConfigItemBO createDraft(DdcConfigCreateRequest request, String operator) {
        LocalDateTime now = LocalDateTime.now();
        DdcConfigItemBO command = DdcConfigItemBO.builder()
                .bizCode(request.getBizCode()).appCode(request.getAppCode()).env(request.getEnv())
                .resourceName(request.getResourceName()).content(request.getContent()).defaultValue(null)
                .format(request.getFormat()).description(request.getDescription()).currentVersion(1L)
                .publishedVersion(null).enabled(true).deleted(false).createdAt(now).updatedAt(now).build();
        DdcConfigItemBO saved = configItemRepository.insert(command);
        saveVersion(saved, null, saved.getContent(), ChangeType.CREATE, "create config", operator);
        saveOperation(saved, ChangeType.CREATE, operator, "create config");
        return saved;
    }

    @Transactional
    public DdcConfigVO update(@Valid DdcConfigUpdateRequest request, String operator) {
        if (request == null) throw new DdcAdminException("config update request is required");
        DdcConfigItemBO item = getConfig(request.getId());
        yamlValidator.validate(item.getResourceName(), item.getFormat(), request.getContent());
        if (request.getCurrentVersion() != null && !Objects.equals(request.getCurrentVersion(), item.getCurrentVersion()))
            throw new DdcAdminException("config version changed");
        String oldContent = item.getContent();
        item.setContent(request.getContent()).setCurrentVersion(item.getCurrentVersion() + 1)
                .setUpdatedAt(LocalDateTime.now());
        DdcConfigItemBO saved = configItemRepository.update(item);
        saveVersion(saved, oldContent, saved.getContent(), ChangeType.UPDATE, request.getChangeReason(), operator);
        saveOperation(saved, ChangeType.UPDATE, operator, request.getChangeReason());
        return itemConverter.toConfigVO(saved);
    }

    @Transactional
    public DdcConfigVO delete(String configPublicId, String operator, String reason) {
        DdcConfigItemBO item = getConfig(configPublicId);
        if (Boolean.TRUE.equals(item.getDeleted())) return config(item);
        return delete(item, operator, reason);
    }

    @Transactional
    public DdcConfigVO delete(String bizCode, String env, String appCode, Long expectedVersion,
            String operator, String reason) {
        DdcConfigItemBO item = findScopeConfig(bizCode, env, appCode).orElse(null);
        if (item == null) return null;
        if (Boolean.TRUE.equals(item.getDeleted())) return config(item);
        if (expectedVersion == null || !Objects.equals(expectedVersion, item.getCurrentVersion()))
            throw new DdcAdminException("config version changed");
        return delete(item, operator, reason);
    }

    private DdcConfigVO delete(DdcConfigItemBO item, String operator, String reason) {
        String oldContent = item.getContent();
        item.setDeleted(true).setEnabled(false).setCurrentVersion(item.getCurrentVersion() + 1)
                .setUpdatedAt(LocalDateTime.now());
        DdcConfigItemBO saved = configItemRepository.update(item);
        saveVersion(saved, oldContent, null, ChangeType.DELETE, reason, operator);
        saveOperation(saved, ChangeType.DELETE, operator, reason);
        return config(saved);
    }

    @Transactional
    public DdcConfigVO rollback(@Valid DdcConfigRollbackRequest request, String operator) {
        DdcConfigItemBO item = getConfig(request.getConfigId());
        DdcConfigVersionBO target = versionRepository.findByConfigIdAndVersion(request.getConfigId(), request.getVersion())
                .orElseThrow(() -> new DdcAdminException("config version not found"));
        yamlValidator.validate(target.getResourceName(), target.getFormat(), target.getNewContent());
        String oldContent = item.getContent();
        item.setContent(target.getNewContent()).setDeleted(false).setEnabled(true)
                .setCurrentVersion(item.getCurrentVersion() + 1).setUpdatedAt(LocalDateTime.now());
        DdcConfigItemBO saved = configItemRepository.update(item);
        saveVersion(saved, oldContent, saved.getContent(), ChangeType.ROLLBACK, request.getReason(), operator);
        saveOperation(saved, ChangeType.ROLLBACK, operator, request.getReason());
        return config(saved);
    }

    public List<DdcConfigVO> list(@Valid DdcConfigQueryRequest request) {
        DdcConfigQueryRequest query = request == null ? new DdcConfigQueryRequest() : request;
        return configItemRepository.search(optional(query.getBizCode()), optional(query.getNamespaceCode()),
                        optional(query.getEnv()), optional(query.getAppCode()), null, query.isIncludeDeleted())
                .stream().map(this::config).toList();
    }

    public DdcPageResult<DdcConfigVO> page(@Valid DdcConfigQueryRequest request, PageQuery pageQuery) {
        DdcConfigQueryRequest query = request == null ? new DdcConfigQueryRequest() : request;
        DdcPageResult<DdcConfigItemBO> page = configItemRepository.search(optional(query.getBizCode()),
                optional(query.getNamespaceCode()), optional(query.getEnv()), optional(query.getAppCode()), null,
                query.isIncludeDeleted(), pageQuery);
        return DdcPageResult.<DdcConfigVO>builder().records(page.getRecords().stream().map(this::config).toList())
                .total(page.getTotal()).pageNo(page.getPageNo()).pageSize(page.getPageSize()).build();
    }

    public DdcConfigVO get(String configPublicId) { return config(getConfig(configPublicId)); }

    public Optional<DdcConfigVO> find(String bizCode, String env, String appCode) {
        return findScopeConfig(bizCode, env, appCode).map(this::config);
    }

    public List<DdcConfigValue> pull(String bizCode, String env, String appCode) {
        return pull(bizCode, env, appCode, null, null);
    }

    public List<DdcConfigValue> pull(String bizCode, String env, String appCode, String resourceName,
            Long targetVersion) {
        return findScopeConfig(bizCode, env, appCode)
                .flatMap(item -> requestedVersion(item, resourceName, targetVersion))
                .filter(this::isRuntimeValue).map(this::toConfigValue).map(List::of).orElseGet(List::of);
    }

    private Optional<DdcConfigVersionBO> requestedVersion(DdcConfigItemBO item, String resourceName,
            Long targetVersion) {
        if (resourceName == null && targetVersion == null) return publishedVersion(item);
        if (resourceName == null || targetVersion == null || targetVersion <= 0
                || !Objects.equals(resourceName, item.getResourceName())) return Optional.empty();
        boolean published = Objects.equals(targetVersion, item.getPublishedVersion());
        boolean prepared = !published && Objects.equals(targetVersion, item.getCurrentVersion())
                && publishTaskRepository != null
                && publishTaskRepository.findFirstByBizCodeAndEnvAndAppCodeAndResourceNameAndStatusIn(
                        item.getBizCode(), item.getEnv(), item.getAppCode(), item.getResourceName(),
                        PREPARED_PUBLISH_STATUSES)
                .map(DdcPublishTaskBO::getTargetVersion).filter(targetVersion::equals).isPresent();
        if (!published && !prepared) return Optional.empty();
        return versionRepository.findByConfigItemIdAndRevision(item.getId(), targetVersion);
    }

    public List<DdcConfigVersionVO> versions(String configPublicId) {
        return versionRepository.findByConfigIdOrderByVersionDesc(configPublicId).stream()
                .map(versionConverter::toVersionVO).toList();
    }

    public DdcPageResult<DdcConfigVersionVO> pageVersions(String configPublicId, PageQuery pageQuery) {
        DdcPageResult<DdcConfigVersionBO> page = versionRepository
                .findByConfigIdOrderByVersionDescIdDesc(configPublicId, pageQuery);
        return DdcPageResult.<DdcConfigVersionVO>builder()
                .records(page.getRecords().stream().map(versionConverter::toVersionVO).toList())
                .total(page.getTotal()).pageNo(page.getPageNo()).pageSize(page.getPageSize()).build();
    }

    private DdcConfigItemBO getConfig(String configPublicId) {
        return configItemRepository.findByPublicId(configPublicId)
                .orElseThrow(() -> new DdcAdminException("config item not found"));
    }

    private void saveVersion(DdcConfigItemBO item, String oldContent, String newContent, ChangeType changeType,
            String reason, String operator) {
        DdcConfigVersionBO version = DdcConfigVersionBO.builder()
                .configItemId(item.getId()).configId(item.getPublicId()).bizCode(item.getBizCode())
                .appCode(item.getAppCode()).env(item.getEnv()).namespace(null).resourceName(item.getResourceName())
                .configRevision(item.getCurrentVersion()).oldContent(oldContent).newContent(newContent)
                .format(item.getFormat()).changeType(changeType.name()).changeReason(reason).operator(operator)
                .createdAt(LocalDateTime.now()).build();
        versionRepository.insert(version);
    }

    private void saveOperation(DdcConfigItemBO item, ChangeType changeType, String operator, String content) {
        DdcOperationLogBO operation = new DdcOperationLogBO();
        operation.setBizCode(item.getBizCode());
        operation.setAppCode(item.getAppCode());
        operation.setEnv(item.getEnv());
        operation.setNamespace(null);
        operation.setResourceName(item.getResourceName());
        operation.setOperationType(changeType.name());
        operation.setOperator(operator);
        operation.setOperationContent(content);
        operation.setCreatedAt(LocalDateTime.now());
        operationLogRepository.insert(operation);
    }

    private Optional<DdcConfigVersionBO> publishedVersion(DdcConfigItemBO item) {
        if (item.getPublishedVersion() == null) return Optional.empty();
        return Optional.of(versionRepository.findByConfigItemIdAndRevision(item.getId(), item.getPublishedVersion())
                .orElseThrow(() -> new DdcAdminException("published config version not found")));
    }

    private DdcConfigValue toConfigValue(DdcConfigVersionBO version) {
        DdcConfigValue value = new DdcConfigValue();
        value.setResourceName(version.getResourceName());
        value.setContent(version.getNewContent());
        value.setFormat(version.getFormat());
        value.setVersion(version.getConfigRevision());
        return value;
    }

    private boolean isRuntimeValue(DdcConfigVersionBO version) {
        return !ChangeType.DELETE.name().equals(version.getChangeType());
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new DdcAdminException(fieldName + " is required");
    }

    private void validateDraft(DdcConfigCreateRequest request) {
        if (request == null) throw new DdcAdminException("config request is required");
        requireText(request.getBizCode(), "bizCode");
        requireText(request.getAppCode(), "appCode");
        requireText(request.getEnv(), "env");
        requireText(request.getResourceName(), "resourceName");
        requireText(request.getFormat(), "format");
        if (!SUPPORTED_RESOURCE_NAMES.contains(request.getResourceName()))
            throw new DdcAdminException("resourceName must be application.yml or application.yaml");
        try {
            DdcConfigFormatStrategyRegistry.defaults().get(request.getFormat(), request.getResourceName());
        } catch (IllegalArgumentException failure) {
            throw new DdcAdminException("unsupported config resource format");
        }
        yamlValidator.validate(request.getResourceName(), request.getFormat(), request.getContent());
    }

    private Optional<DdcConfigItemBO> findScopeConfig(String bizCode, String env, String appCode) {
        List<DdcConfigItemBO> items = configItemRepository.findByBizCodeAndEnvAndAppCode(bizCode, env, appCode);
        if (items.size() > 1) throw new DdcAdminException("Tianshu scope contains multiple configuration resources");
        return items.stream().findFirst();
    }

    private DdcConfigVO config(DdcConfigItemBO item) {
        DdcConfigVO value = itemConverter.toConfigVO(item);
        if (bindingRepository != null) {
            value.setVisibleNamespaces(bindingRepository.findVisibleNamespaceCodes(
                    item.getBizCode(), item.getEnv(), item.getAppCode()));
        }
        return value;
    }

    private String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
