package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRpcDescriptor;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class GatewayRuleCompiler {

    public static final int INLINE_LIMIT_BYTES = 512 * 1024;

    public static final int CHUNK_LIMIT_BYTES = 256 * 1024;

    private static final Set<String> SUPPORTED_POLICIES = Set.of(
            "LOAD_BALANCE",
            "PROVIDER_OVERRIDE",
            "RATE_LIMIT",
            "CIRCUIT_BREAKER",
            "RETRY",
            "TIMEOUT",
            "BULKHEAD",
            "REQUEST_SIZE",
            "RESPONSE_SIZE",
            "SECURITY",
            "AUTHENTICATION",
            "HEADER_TOKEN",
            "SIGNATURE",
            "IP_ACL",
            "CORS"
    );

    private final GatewayRuleCanonicalizer canonicalizer;

    private final GatewayRouteTransportPolicyValidator transportValidator =
            new GatewayRouteTransportPolicyValidator();

    public GatewayRuleCompiler(GatewayRuleCanonicalizer canonicalizer) {
        this.canonicalizer = canonicalizer;
    }

    public CompiledGatewayRelease compile(
            String releaseId,
            Instant generatedAt,
            GatewayRuleContent content) {
        validate(content);
        GatewayRuleSnapshot snapshot = canonicalizer.snapshot(
                releaseId,
                generatedAt,
                content
        );
        String snapshotJson = canonicalizer.json(snapshot);
        byte[] bytes = snapshotJson.getBytes(StandardCharsets.UTF_8);
        Map<String, String> chunks = new LinkedHashMap<>();
        List<GatewayRuleChunkRef> references = new ArrayList<>();
        GatewayRuleActivationMode mode;
        String inline;
        if (bytes.length <= INLINE_LIMIT_BYTES) {
            mode = GatewayRuleActivationMode.INLINE;
            inline = snapshotJson;
        } else {
            mode = GatewayRuleActivationMode.CHUNKED;
            inline = null;
            for (int offset = 0, index = 0;
                 offset < bytes.length;
                 offset += CHUNK_LIMIT_BYTES, index++) {
                int length = Math.min(CHUNK_LIMIT_BYTES, bytes.length - offset);
                byte[] part = java.util.Arrays.copyOfRange(
                        bytes,
                        offset,
                        offset + length
                );
                String key = "gateway.rules.chunk."
                        + releaseId
                        + "."
                        + index;
                chunks.put(key, Base64.getEncoder().encodeToString(part));
                references.add(new GatewayRuleChunkRef(
                        key,
                        index,
                        length,
                        GatewayRuleCanonicalizer.sha256(part)
                ));
            }
        }
        GatewayRuleActivation activation = new GatewayRuleActivation(
                "v1",
                releaseId,
                mode,
                snapshot.ruleSchemaVersion(),
                bytes.length,
                snapshot.ruleContentSha256(),
                snapshot.artifactSha256(),
                inline,
                references
        );
        return new CompiledGatewayRelease(
                snapshot,
                snapshotJson,
                activation,
                canonicalizer.json(activation),
                chunks
        );
    }

    private void validate(GatewayRuleContent content) {
        unique(content.operations(), GatewayRuntimeOperation::operationId);
        unique(content.operations(), GatewayRuntimeOperation::operationKey);
        unique(content.routes(), GatewayRuntimeRoute::routeId);
        List<GatewayRuntimePolicy> policies = allPolicies(content);
        unique(policies, GatewayRuntimePolicy::policyId);
        unique(content.rpcDescriptors(), GatewayRpcDescriptor::descriptorId);
        Set<String> policyIds = policies.stream()
                .map(GatewayRuntimePolicy::policyId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, GatewayRuntimePolicy> policiesById = policies.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        GatewayRuntimePolicy::policyId,
                        Function.identity()
                ));
        Map<String, GatewayRuntimeOperation> operations = content.operations()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        GatewayRuntimeOperation::operationId,
                        Function.identity()
                ));
        content.routes().forEach(route -> {
            GatewayRuntimeOperation operation = operations.get(
                    route.operationId()
            );
            if (operation == null) {
                return;
            }
            List<GatewayRouteTransportPolicyValidator.ValidationIssue> issues =
                    transportValidator.validate(route, operation);
            if (!issues.isEmpty()) {
                GatewayRouteTransportPolicyValidator.ValidationIssue issue =
                        issues.getFirst();
                throw invalid(
                        issue.code()
                                + " at routes."
                                + route.routeId()
                                + "."
                                + issue.path()
                                + ": "
                                + issue.message()
                );
            }
        });
        policies.forEach(policy -> {
            if (!SUPPORTED_POLICIES.contains(policy.type())) {
                throw invalid("unsupported policy type " + policy.type());
            }
        });
        content.operations().forEach(operation -> {
            if (!policyIds.containsAll(operation.policyRefs())) {
                throw invalid(
                        "operation references an unknown policy: "
                                + operation.operationId()
                );
            }
            if (operation.protocol()
                    != operation.providerService().protocol()) {
                throw invalid(
                        "operation/provider protocol mismatch: "
                                + operation.operationId()
                );
            }
            if (operation.providerService().serviceName().contains("://")) {
                throw invalid("static provider URL is forbidden");
            }
            if (Boolean.parseBoolean(
                    operation.attributes().get("streaming"))) {
                throw invalid("streaming operation is not supported in v1");
            }
            boolean retryReferenced = operation.policyRefs().stream()
                    .map(policiesById::get)
                    .anyMatch(policy -> "RETRY".equals(policy.type()));
            if (retryReferenced && !Boolean.parseBoolean(
                    operation.attributes().get("idempotent"))) {
                throw invalid(
                        "Retry requires an explicitly idempotent operation: "
                                + operation.operationId()
                );
            }
        });
        List<RuntimeHttpRoute> runtimeRoutes = content.routes().stream()
                .filter(GatewayRuntimeRoute::enabled)
                .map(route -> runtimeRoute(content, route, operations))
                .toList();
        new HttpRouteCompiler().compile(runtimeRoutes);
        validateDescriptors(content.rpcDescriptors());
    }

    private RuntimeHttpRoute runtimeRoute(
            GatewayRuleContent content,
            GatewayRuntimeRoute route,
            Map<String, GatewayRuntimeOperation> operations) {
        GatewayRuntimeOperation operation = operations.get(route.operationId());
        if (operation == null) {
            throw invalid("route references unknown operation");
        }
        if (route.accessZones().contains(AccessZone.PUBLIC)
                && !operation.externalAccessible()) {
            throw invalid(
                    "PUBLIC route references an internal-only operation"
            );
        }
        GatewayProviderServiceRef service = operation.providerService();
        ProviderProtocolType protocol = service.protocol()
                == GatewayProtocol.HTTP
                ? ProviderProtocolType.HTTP
                : ProviderProtocolType.RPC;
        return new RuntimeHttpRoute(
                route.routeId(),
                operation.operationId(),
                content.gatewayGroupId(),
                route.accessZones(),
                route.host(),
                Set.of(route.httpMethod()),
                route.pathPattern(),
                operation.externalAccessible(),
                new ProviderServiceKey(
                        service.env(),
                        service.namespace(),
                        protocol,
                        service.serviceName(),
                        service.group(),
                        service.version(),
                        service.transport()
                ),
                operation.policyRefs(),
                route.priority(),
                GatewayResponseMode.valueOf(operation.responseMode()),
                Map.of()
        );
    }

    private void validateDescriptors(List<GatewayRpcDescriptor> descriptors) {
        for (GatewayRpcDescriptor descriptor : descriptors) {
            byte[] value;
            try {
                value = Base64.getDecoder().decode(
                        descriptor.base64DescriptorSet()
                );
            } catch (IllegalArgumentException invalidBase64) {
                throw invalid("RPC descriptor is not valid base64");
            }
            if (!GatewayRuleCanonicalizer.sha256(value)
                    .equals(descriptor.sha256())) {
                throw invalid("RPC descriptor checksum mismatch");
            }
        }
    }

    private List<GatewayRuntimePolicy> allPolicies(
            GatewayRuleContent content) {
        List<GatewayRuntimePolicy> result = new ArrayList<>();
        result.addAll(content.providerPolicies());
        result.addAll(content.trafficPolicies());
        result.addAll(content.securityPolicies());
        result.addAll(content.corsPolicies());
        return List.copyOf(result);
    }

    private <T> void unique(List<T> values, Function<T, String> key) {
        Set<String> seen = new HashSet<>();
        values.forEach(value -> {
            if (!seen.add(key.apply(value))) {
                throw invalid("duplicate rule identifier " + key.apply(value));
            }
        });
    }

    private IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException(
                "GATEWAY_RELEASE_VALIDATION_FAILED: " + detail
        );
    }
}
