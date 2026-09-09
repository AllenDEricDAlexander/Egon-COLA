package top.egon.cola.component.yuheng.admin.openapi.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import top.egon.cola.component.tianshu.model.management.DdcInstanceStatus;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceKey;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.yuheng.contract.reporting.openapi.GatewayOpenApiGroupManifestDTO;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * Trusted Provider target derived from a Tianshu instance and its group manifest.
 *
 * <p>中文：只允许由受信 Tianshu 服务实例、Manifest 和已解析 applicationId
 * 组装；调用方不能直接提交任意 URL。</p>
 */
public record GatewayOpenApiSyncCandidateDTO(
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 64, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String applicationId,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 128, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String bizCode,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 128, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String applicationCode,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 256, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String buildId,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 128, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String artifactVersion,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Pattern(
                regexp = GatewayOpenApiGroupManifestDTO.GROUP_PATTERN,
                groups = {Default.class, GatewayOpenApiIngestionGroup.class}
        )
        @Size(max = 64, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String openapiGroup,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 256, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String providerServiceName,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 128, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String providerGroup,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 128, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String providerVersion,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 256, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String instanceId,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 256, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String host,
        @Min(value = 1, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Max(value = 65535, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        int port,
        boolean secure,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(max = 512, groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String pathTemplate,
        @NotNull(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        URI resourceUri,
        @NotNull(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        Instant observedAt,
        @NotNull(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        Instant expiresAt,
        boolean developmentPlaintext
) {

    /**
     * Fixed endpoint path and trusted control-plane identity are validated at
     * the candidate boundary before a token or network call is attempted.
     */
    public GatewayOpenApiSyncCandidateDTO {
        applicationId = required(applicationId, "applicationId", 64);
        bizCode = required(bizCode, "bizCode", 128);
        applicationCode = required(applicationCode, "applicationCode", 128);
        buildId = required(buildId, "buildId", 256);
        artifactVersion = required(artifactVersion, "artifactVersion", 128);
        openapiGroup = required(openapiGroup, "openapiGroup", 64);
        if (!openapiGroup.matches(
                GatewayOpenApiGroupManifestDTO.GROUP_PATTERN
        )) {
            throw new IllegalArgumentException(
                    "openapiGroup has an invalid code"
            );
        }
        providerServiceName = required(
                providerServiceName,
                "providerServiceName",
                256
        );
        providerGroup = required(providerGroup, "providerGroup", 128);
        providerVersion = required(
                providerVersion,
                "providerVersion",
                128
        );
        instanceId = required(instanceId, "instanceId", 256);
        host = host(host);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "port must be between 1 and 65535"
            );
        }
        if (!secure && !developmentPlaintext) {
            throw new IllegalArgumentException(
                    "OpenAPI provider target must use HTTPS"
            );
        }
        if (secure && developmentPlaintext) {
            throw new IllegalArgumentException(
                    "developmentPlaintext requires an insecure target"
            );
        }
        pathTemplate = required(pathTemplate, "pathTemplate", 512);
        if (!GatewayOpenApiGroupManifestDTO.PATH_TEMPLATE.equals(
                pathTemplate
        )) {
            throw new IllegalArgumentException(
                    "pathTemplate must equal "
                            + GatewayOpenApiGroupManifestDTO.PATH_TEMPLATE
            );
        }
        resourceUri = resourceUri(resourceUri);
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(observedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after observedAt"
            );
        }
    }

    /** Compatibility constructor for the strict HTTPS candidate contract. */
    public GatewayOpenApiSyncCandidateDTO(
            String applicationId,
            String bizCode,
            String applicationCode,
            String buildId,
            String artifactVersion,
            String openapiGroup,
            String providerServiceName,
            String providerGroup,
            String providerVersion,
            String instanceId,
            String host,
            int port,
            boolean secure,
            String pathTemplate,
            URI resourceUri,
            Instant observedAt,
            Instant expiresAt) {
        this(
                applicationId,
                bizCode,
                applicationCode,
                buildId,
                artifactVersion,
                openapiGroup,
                providerServiceName,
                providerGroup,
                providerVersion,
                instanceId,
                host,
                port,
                secure,
                pathTemplate,
                resourceUri,
                observedAt,
                expiresAt,
                false
        );
    }

    /**
     * Builds a candidate only from a coherent Tianshu service snapshot, instance
     * and published group manifest.
     *
     * @param applicationId resolved physical Gateway application id
     * @param snapshot Tianshu service snapshot
     * @param instance healthy candidate instance
     * @param manifest validated published group manifest
     * @param group group selected from the manifest
     * @return trusted normalized candidate
     */
    public static GatewayOpenApiSyncCandidateDTO from(
            String applicationId,
            DdcManagementServiceSnapshot snapshot,
            DdcManagementServiceInstance instance,
            GatewayOpenApiGroupManifestDTO manifest,
            String group) {
        return from(
                applicationId,
                snapshot,
                instance,
                manifest,
                group,
                false
        );
    }

    /**
     * Builds a candidate with an explicit local-development HTTP exception.
     * The exception is only materialized when the caller has already enabled
     * the local plaintext policy; the default overload remains HTTPS-only.
     *
     * @param applicationId resolved physical Gateway application id
     * @param snapshot Tianshu service snapshot
     * @param instance healthy candidate instance
     * @param manifest validated published group manifest
     * @param group group selected from the manifest
     * @param allowDevelopmentHttp explicit local plaintext policy
     * @return trusted normalized candidate
     */
    public static GatewayOpenApiSyncCandidateDTO from(
            String applicationId,
            DdcManagementServiceSnapshot snapshot,
            DdcManagementServiceInstance instance,
            GatewayOpenApiGroupManifestDTO manifest,
            String group,
            boolean allowDevelopmentHttp) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(manifest, "manifest");
        String normalizedGroup = required(group, "group", 64);
        if (!manifest.groups().contains(normalizedGroup)) {
            throw new IllegalArgumentException(
                    "group is not published in the manifest"
            );
        }
        DdcManagementServiceKey service = Objects.requireNonNull(
                snapshot.serviceKey(),
                "snapshot.serviceKey"
        );
        boolean secureHttp = "https".equalsIgnoreCase(service.protocol())
                && instance.secure();
        boolean developmentHttp = allowDevelopmentHttp
                && "http".equalsIgnoreCase(service.protocol())
                && !instance.secure();
        if (!"HTTP_PROVIDER".equals(service.serviceKind())
                || (!secureHttp && !developmentHttp)) {
            throw new IllegalArgumentException(
                    "Tianshu service is not an HTTP provider"
            );
        }
        Instant observedAt = Objects.requireNonNull(
                snapshot.observedAt(),
                "snapshot.observedAt"
        );
        if (!snapshot.instances().contains(instance)) {
            throw new IllegalArgumentException(
                    "Tianshu instance is not part of the supplied snapshot"
            );
        }
        if (instance.normalizedStatus() != DdcInstanceStatus.ONLINE
                || instance.expireAt() == null
                || !instance.expireAt().isAfter(observedAt)) {
            throw new IllegalArgumentException(
                    "Tianshu instance is not healthy and unexpired"
            );
        }
        return new GatewayOpenApiSyncCandidateDTO(
                applicationId,
                service.bizCode(),
                service.appCode(),
                manifest.buildId(),
                manifest.artifactVersion(),
                normalizedGroup,
                service.serviceName(),
                service.group(),
                service.version(),
                instance.instanceId(),
                instance.host(),
                instance.port(),
                instance.secure(),
                manifest.pathTemplate(),
                URI.create(manifest.resourceUri()),
                observedAt,
                instance.expireAt(),
                developmentHttp
        );
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " is blank or exceeds " + max + " characters"
            );
        }
        return normalized;
    }

    private static String host(String value) {
        String normalized = required(value, "host", 256);
        if (normalized.startsWith("[") || normalized.endsWith("]")
                || normalized.chars().anyMatch(Character::isWhitespace)
                || normalized.indexOf('/') >= 0
                || normalized.indexOf('?') >= 0
                || normalized.indexOf('#') >= 0
                || normalized.indexOf('@') >= 0) {
            throw new IllegalArgumentException(
                    "host must be a bare DNS name or IP address"
            );
        }
        return normalized;
    }

    private static URI resourceUri(URI value) {
        URI normalized = Objects.requireNonNull(value, "resourceUri")
                .normalize();
        if (!normalized.isAbsolute()
                || !"https".equalsIgnoreCase(normalized.getScheme())
                || normalized.getHost() == null
                || normalized.getUserInfo() != null
                || normalized.getQuery() != null
                || normalized.getFragment() != null
                || !normalized.equals(value)) {
            throw new IllegalArgumentException(
                    "resourceUri must be an absolute normalized HTTPS URI "
                            + "without credentials, query or fragment"
            );
        }
        return normalized;
    }
}
