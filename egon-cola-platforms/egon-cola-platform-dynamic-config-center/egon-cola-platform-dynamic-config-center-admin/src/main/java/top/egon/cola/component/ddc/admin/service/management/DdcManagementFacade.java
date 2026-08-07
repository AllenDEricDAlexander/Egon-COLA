package top.egon.cola.component.ddc.admin.service.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigService;
import top.egon.cola.component.ddc.admin.service.lease.DdcInstanceAdminService;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.admin.service.publish.DdcPublishService;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.management.client.DdcManagementErrorCode;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTarget;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceKey;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DdcManagementFacade {

    private final DdcConfigService configService;

    private final DdcPublishService publishService;

    private final DdcPublishTaskRepository publishTaskRepository;

    private final DdcPublishAckRepository publishAckRepository;

    private final DdcInstanceAdminService instanceAdminService;

    private final DdcServiceRegistryService registryService;

    private final DdcScopeGate scopeGate;

    private final DdcNamespaceEnvAppBindingRepository bindingRepository;

    @Autowired
    public DdcManagementFacade(
            DdcConfigService configService,
            DdcPublishService publishService,
            DdcPublishTaskRepository publishTaskRepository,
            DdcPublishAckRepository publishAckRepository,
            DdcInstanceAdminService instanceAdminService,
            DdcServiceRegistryService registryService,
            DdcScopeGate scopeGate,
            DdcNamespaceEnvAppBindingRepository bindingRepository
    ) {
        this.configService = configService;
        this.publishService = publishService;
        this.publishTaskRepository = publishTaskRepository;
        this.publishAckRepository = publishAckRepository;
        this.instanceAdminService = instanceAdminService;
        this.registryService = registryService;
        this.scopeGate = scopeGate;
        this.bindingRepository = bindingRepository;
    }

    public DdcManagementFacade(
            DdcConfigService configService,
            DdcPublishService publishService,
            DdcPublishTaskRepository publishTaskRepository,
            DdcPublishAckRepository publishAckRepository,
            DdcInstanceAdminService instanceAdminService,
            DdcServiceRegistryService registryService,
            DdcScopeGate scopeGate
    ) {
        this(
                configService,
                publishService,
                publishTaskRepository,
                publishAckRepository,
                instanceAdminService,
                registryService,
                scopeGate,
                null
        );
    }

    public DdcManagementConfig findConfig(DdcManagementConfigQuery query) {
        require(query, "config query");
        scopeGate.assertPhysicalEnabled(
                requireText(query.bizCode(), "bizCode"),
                requireText(query.appCode(), "appCode"),
                requireText(query.env(), "env")
        );
        validateScope(
                query.bizCode(),
                query.env(),
                query.appCode(),
                query.configKey()
        );
        return config(configService.find(
                query.bizCode(),
                query.env(),
                query.appCode(),
                query.configKey()
        ).orElseThrow(() -> new DdcAdminException(
                DdcManagementErrorCode.CONFIG_NOT_FOUND
        )));
    }

    public DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request) {
        require(request, "config upsert request");
        validateScope(
                request.bizCode(),
                request.env(),
                request.appCode(),
                request.configKey()
        );
        requireText(request.operator(), "operator");
        DdcConfigVO saved = configService.upsert(
                new DdcConfigCreateRequest(
                        request.bizCode(),
                        request.env(),
                        request.appCode(),
                        null,
                        request.configKey(),
                        request.configValue(),
                        null,
                        request.valueType(),
                        request.description()
                ),
                request.expectedVersion(),
                request.operator()
        );
        return config(saved);
    }

    public void delete(DdcManagementConfigDeleteRequest request) {
        require(request, "config delete request");
        validateScope(
                request.bizCode(),
                request.env(),
                request.appCode(),
                request.configKey()
        );
        requireText(request.operator(), "operator");
        configService.delete(
                request.bizCode(),
                request.env(),
                request.appCode(),
                request.configKey(),
                request.expectedVersion(),
                request.operator(),
                request.reason()
        );
    }

    public DdcManagementPublishResult publish(DdcManagementPublishRequest request) {
        require(request, "publish request");
        validateScope(
                request.bizCode(),
                request.env(),
                request.appCode(),
                request.configKey()
        );
        requireText(request.operator(), "operator");
        DdcPublishRequest command = new DdcPublishRequest();
        command.setChangeId(request.changeId());
        command.setBizCode(request.bizCode());
        command.setEnv(request.env());
        command.setAppCode(request.appCode());
        command.setConfigKey(request.configKey());
        command.setConfigValue(request.configValue());
        command.setExpectedVersion(request.expectedVersion());
        command.setTimeoutMs(request.timeoutMs());
        publishService.publish(command, request.operator());
        return result(getPublishTask(request.changeId()));
    }

    public DdcManagementPublishTask getPublishTask(String changeId) {
        requireText(changeId, "changeId");
        DdcPublishTaskEntity task = publishTaskRepository.findByChangeId(changeId)
                .orElseThrow(() -> new DdcAdminException(
                        DdcManagementErrorCode.PUBLISH_TASK_NOT_FOUND
                ));
        List<DdcManagementPublishTarget> targets =
                publishAckRepository.findByChangeId(changeId).stream()
                        .sorted(Comparator
                                .comparing(DdcPublishAckEntity::getInstanceId)
                                .thenComparing(
                                        DdcPublishAckEntity::getLeaseId,
                                        Comparator.nullsFirst(String::compareTo)
                                ))
                        .map(this::target)
                        .toList();
        return new DdcManagementPublishTask(
                task.getChangeId(),
                status(task.getStatus()),
                task.getTargetVersion(),
                task.getContentChecksum(),
                count(task.getTargetCount()),
                count(task.getAckCount()),
                count(task.getFailedCount()),
                count(task.getIgnoredCount()),
                count(task.getTimeoutCount()),
                count(task.getAttemptCount()),
                targets,
                task.getErrorMessage(),
                instant(task.getCreatedAt()),
                instant(task.getDispatchedAt()),
                instant(task.getCompletedAt())
        );
    }

    public DdcManagementPublishResult retry(String changeId) {
        publishService.retry(changeId);
        return result(getPublishTask(changeId));
    }

    public List<DdcManagementConfigClientInstance> getConfigClients(
            DdcManagementInstanceQuery query
    ) {
        require(query, "instance query");
        requireText(query.bizCode(), "bizCode");
        requireText(query.appCode(), "appCode");
        requireText(query.env(), "env");
        return instanceAdminService.list(
                        query.bizCode(),
                        query.env(),
                        query.appCode()
                ).stream()
                .sorted(Comparator.comparing(DdcInstanceEntity::getInstanceId))
                .map(this::configClient)
                .toList();
    }

    public DdcManagementServiceCatalog getServiceKeys(
            DdcManagementServiceQuery query
    ) {
        DdcServiceQuery serviceQuery = serviceQuery(query);
        DdcServiceCatalogSnapshot snapshot =
                registryService.getServiceKeys(serviceQuery);
        Set<String> visibleScopes = visibleScopes(query);
        return new DdcManagementServiceCatalog(
                snapshot.revision(),
                snapshot.observedAt(),
                snapshot.serviceKeys().stream()
                        .filter(key -> visibleScopes == null
                                || visibleScopes.contains(scope(key)))
                        .map(this::serviceKey)
                        .toList()
        );
    }

    public DdcManagementServiceSnapshot getInstances(
            DdcManagementServiceQuery query
    ) {
        require(query, "service query");
        DdcServiceKind kind = serviceKind(query.serviceKind());
        DdcServiceKey key = new DdcServiceKey(
                requireText(query.bizCode(), "bizCode"),
                requireText(query.env(), "env"),
                requireText(query.appCode(), "appCode"),
                kind,
                requireText(query.serviceName(), "serviceName"),
                query.group(),
                query.version(),
                requireText(query.protocol(), "protocol")
        );
        DdcServiceSnapshot snapshot = registryService.getInstances(key);
        return new DdcManagementServiceSnapshot(
                serviceKey(snapshot.serviceKey()),
                snapshot.revision(),
                snapshot.observedAt(),
                snapshot.instances().stream().map(this::serviceInstance).toList()
        );
    }

    private DdcManagementPublishResult result(DdcManagementPublishTask task) {
        return new DdcManagementPublishResult(
                task.changeId(),
                task.status(),
                task.targetVersion(),
                task.contentChecksum(),
                task.targetCount(),
                task.targets(),
                task.errorMessage(),
                task.createdAt(),
                task.dispatchedAt(),
                task.completedAt()
        );
    }

    private DdcManagementConfig config(DdcConfigVO value) {
        return new DdcManagementConfig(
                value.getBizCode(),
                value.getEnv(),
                value.getAppCode(),
                value.getConfigKey(),
                value.getConfigValue(),
                value.getValueType(),
                value.getCurrentVersion(),
                Boolean.TRUE.equals(value.getEnabled()),
                Boolean.TRUE.equals(value.getDeleted()),
                instant(value.getUpdatedAt())
        );
    }

    private DdcManagementPublishTarget target(DdcPublishAckEntity value) {
        return new DdcManagementPublishTarget(
                value.getInstanceId(),
                value.getLeaseId(),
                value.getCurrentVersion(),
                value.getAckStatus() == null ? "PENDING" : value.getAckStatus(),
                value.getErrorMessage(),
                instant(value.getAckAt())
        );
    }

    private DdcManagementConfigClientInstance configClient(DdcInstanceEntity value) {
        return new DdcManagementConfigClientInstance(
                value.getBizCode(),
                value.getEnv(),
                value.getAppCode(),
                value.getInstanceId(),
                value.getLeaseId(),
                value.getHost(),
                value.getPort(),
                "CONFIG_CLIENT",
                value.getStatus(),
                instant(value.getCreatedAt()),
                instant(value.getLastHeartbeatAt()),
                instant(value.getLeaseExpireAt()),
                value.getRuntimeMetadata()
        );
    }

    private DdcManagementServiceKey serviceKey(DdcServiceKey value) {
        return new DdcManagementServiceKey(
                value.bizCode(),
                value.env(),
                value.appCode(),
                value.serviceId(),
                value.serviceKind().name(),
                value.serviceName(),
                value.group(),
                value.version(),
                value.protocol()
        );
    }

    private DdcManagementServiceInstance serviceInstance(
            top.egon.cola.component.ddc.model.registry.DdcServiceInstance value
    ) {
        return new DdcManagementServiceInstance(
                value.instanceId(),
                value.leaseId(),
                value.host(),
                value.port(),
                value.secure(),
                value.metadata(),
                value.status(),
                value.registeredAt(),
                value.lastHeartbeatAt(),
                value.leaseExpireAt()
        );
    }

    private DdcServiceQuery serviceQuery(DdcManagementServiceQuery query) {
        if (query == null) {
            throw new DdcAdminException("service query is required");
        }
        return new DdcServiceQuery(
                query.bizCode(),
                query.env(),
                query.appCode(),
                optionalServiceKind(query.serviceKind()),
                query.protocol(),
                query.serviceName(),
                query.group(),
                query.version()
        );
    }

    private DdcServiceKind serviceKind(String value) {
        try {
            return DdcServiceKind.valueOf(
                    requireText(value, "serviceKind").toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new DdcAdminException("unsupported serviceKind");
        }
    }

    private DdcServiceKind optionalServiceKind(String value) {
        return value == null || value.isBlank() ? null : serviceKind(value);
    }

    private DdcManagementPublishStatus status(String value) {
        try {
            return DdcManagementPublishStatus.valueOf(value);
        } catch (RuntimeException exception) {
            return DdcManagementPublishStatus.UNKNOWN;
        }
    }

    private void validateScope(
            String bizCode,
            String env,
            String appCode,
            String configKey
    ) {
        requireText(bizCode, "bizCode");
        requireText(appCode, "appCode");
        requireText(env, "env");
        String exactKey = requireText(configKey, "configKey");
        if (exactKey.contains("*") || exactKey.contains("?")) {
            throw new DdcAdminException("an exact configKey is required");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DdcAdminException(fieldName + " is required");
        }
        return value;
    }

    private int count(Integer value) {
        return value == null ? 0 : value;
    }

    private <T> T require(T value, String fieldName) {
        if (Objects.isNull(value)) {
            throw new DdcAdminException(fieldName + " is required");
        }
        return value;
    }

    private Instant instant(LocalDateTime value) {
        return value == null
                ? null
                : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private Set<String> visibleScopes(DdcManagementServiceQuery query) {
        if (query.namespaceCode() == null || query.namespaceCode().isBlank()) {
            return null;
        }
        if (bindingRepository == null) {
            return Set.of();
        }
        String bizCode = query.bizCode() == null || query.bizCode().isBlank()
                ? null
                : query.bizCode().trim();
        return bindingRepository.findVisiblePhysicalScopes(
                        bizCode,
                        query.namespaceCode().trim()
                ).stream()
                .map(row -> String.join(
                        "\n",
                        String.valueOf(row[0]),
                        String.valueOf(row[1]),
                        String.valueOf(row[2])
                ))
                .collect(Collectors.toUnmodifiableSet());
    }

    private String scope(DdcServiceKey key) {
        return String.join("\n", key.bizCode(), key.env(), key.appCode());
    }
}
