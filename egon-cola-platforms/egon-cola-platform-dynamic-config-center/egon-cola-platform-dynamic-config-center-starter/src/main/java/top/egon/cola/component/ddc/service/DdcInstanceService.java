package top.egon.cola.component.ddc.service;

import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DdcInstanceService {

    private final DdcProperties properties;

    private final DdcAdminClient adminClient;

    private final DdcInstanceIdentity identity;

    private final DdcLeaseSessionHolder sessionHolder;

    private final List<DdcInstanceMetadataContributor> metadataContributors;

    public DdcInstanceService(DdcProperties properties,
                              DdcAdminClient adminClient,
                              DdcInstanceIdentity identity,
                              DdcLeaseSessionHolder sessionHolder) {
        this(properties, adminClient, identity, sessionHolder, List.of());
    }

    public DdcInstanceService(
            DdcProperties properties,
            DdcAdminClient adminClient,
            DdcInstanceIdentity identity,
            DdcLeaseSessionHolder sessionHolder,
            List<DdcInstanceMetadataContributor> metadataContributors) {
        this.properties = properties;
        this.adminClient = adminClient;
        this.identity = identity;
        this.sessionHolder = sessionHolder;
        this.metadataContributors = List.copyOf(metadataContributors);
    }

    public DdcLeaseSession register() {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        fill(request);
        request.setLeaseSeconds(properties.getInstance().getLeaseSeconds());
        request.setHeartbeatIntervalSeconds(properties.getInstance().getHeartbeatIntervalSeconds());
        DdcLeaseSession session = adminClient.register(request);
        validate(session);
        sessionHolder.replace(session);
        return session;
    }

    public DdcLeaseOperationResult heartbeat(DdcLeaseSession session) {
        return adminClient.heartbeat(operationRequest(session));
    }

    public DdcLeaseOperationResult offline(DdcLeaseSession session) {
        return adminClient.offline(operationRequest(session));
    }

    public DdcInstanceIdentity identity() {
        return identity;
    }

    private DdcHeartbeatRequest operationRequest(DdcLeaseSession session) {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        request.setLeaseId(session.leaseId());
        fill(request);
        return request;
    }

    private void fill(DdcHeartbeatRequest request) {
        request.setInstanceId(identity.instanceId());
        request.setBizCode(identity.bizCode());
        request.setAppCode(identity.appCode());
        request.setEnv(identity.env());
        request.setHost(identity.host());
        request.setPort(identity.port());
        request.setPid(identity.pid());
        request.setSdkVersion(identity.sdkVersion());
        request.setMetadata(metadata());
    }

    private void fill(DdcInstanceRegisterRequest request) {
        request.setInstanceId(identity.instanceId());
        request.setBizCode(identity.bizCode());
        request.setAppCode(identity.appCode());
        request.setEnv(identity.env());
        request.setHost(identity.host());
        request.setPort(identity.port());
        request.setPid(identity.pid());
        request.setSdkVersion(identity.sdkVersion());
        request.setMetadata(metadata());
    }

    private Map<String, String> metadata() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        metadataContributors.forEach(contributor -> {
            Map<String, String> values = contributor.metadata();
            if (values != null) {
                values.forEach((key, value) -> result.put(
                        validatedKey(key),
                        validatedValue(key, value)
                ));
            }
        });
        if (result.size() > 32) {
            throw new DdcException(
                    "DDC instance metadata must contain at most 32 entries"
            );
        }
        return Map.copyOf(result);
    }

    private String validatedKey(String key) {
        if (key == null || key.isBlank() || key.length() > 64) {
            throw new DdcException("Invalid DDC instance metadata key");
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.contains("password")
                || lower.contains("secret")
                || lower.contains("token")
                || lower.contains("privatekey")
                || lower.contains("private-key")
                || lower.contains("certificate")) {
            throw new DdcException(
                    "DDC instance metadata key may expose sensitive data"
            );
        }
        return key;
    }

    private String validatedValue(String key, String value) {
        String normalized = value == null ? "" : value;
        if (normalized.length() > 512) {
            throw new DdcException(
                    "DDC instance metadata value is too long: " + key
            );
        }
        return normalized;
    }

    private void validate(DdcLeaseSession session) {
        if (session == null
                || !identity.instanceId().equals(session.instanceId())
                || session.leaseId() == null
                || session.leaseId().isBlank()
                || session.role() != DdcLeaseRole.CONFIG_CLIENT) {
            throw new DdcException("Admin returned an invalid DDC lease");
        }
    }
}
