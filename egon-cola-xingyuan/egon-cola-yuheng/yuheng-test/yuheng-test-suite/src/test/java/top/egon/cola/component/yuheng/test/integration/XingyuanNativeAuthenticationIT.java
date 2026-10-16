package top.egon.cola.component.yuheng.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Validation;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpClientCredentialsRequestEntityConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest;
import top.egon.cola.platform.tianquan.shoubing.starter.client.ServiceAuthorizationKey;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcClientCredentialInterceptorFactory;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientFactory;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientSettings;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentityFactory;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.tianshu.model.registry.DdcServiceInstance;
import top.egon.cola.component.tianshu.model.management.DdcInstanceStatus;
import com.google.protobuf.Message;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient.activeNativeRuntimeRoles;
import static top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class XingyuanNativeAuthenticationIT {

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void verifyNativeAuthentication() throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(bytes);
        String digest = UnifiedIdentityLiveClient.sha256(bytes); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory"));
        XingyuanResourceAuthorizationIT.requireEvidenceDirectory(root, evidence);
        if (evidence.equals(root.resolve("evidence"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var intent = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("intent.json"), evidence, 128 * 1024));
        if (!manifest.path("runId").asText().equals(intent.path("runId").asText()) || !digest.equals(intent.path("manifestSha256").asText())
                || !"full".equals(intent.path("caseSet").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var roles = activeNativeRuntimeRoles(root, manifest, digest);
        if (!roles.equals(List.of("native-light", "native-web", "native-service"))) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        XingyuanResourceAuthorizationIT.requireRunningProcesses(root, manifestFile, manifest, digest, evidence, Set.copyOf(roles));
        Path buildFile = evidence.resolve("native-generated-build.json");
        var build = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "observe-native-build", manifestFile.toString(), buildFile.toString()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!build.waitFor(manifest.path("deadlines").path("operationSeconds").intValue(), TimeUnit.SECONDS) || build.exitValue() != 0) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        } finally { if (build.isAlive()) { build.destroy(); build.waitFor(10, TimeUnit.SECONDS); } }
        var generated = requireNativeEvidence(root, evidence, manifest, digest, "native-generated-build.json");
        if (!"FOUR_CURRENT_GENERATED_NATIVE_BUILD_AND_OPEN_PRESERVATION".equals(generated.path("scope").asText())
                || generated.path("sourceBuild").path("artifacts").size() != 4 || generated.path("sourceBuild").path("generatedFamilies").size() != 7
                || generated.path("sourceBuild").path("openSources").size() != 3) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        publishLiveCase(root, evidence, manifest, digest, "LIVE-012", "native", "verifyNativeAuthentication", List.of("native-generated-build.json"), false);

        UnifiedIdentityLiveClient.isolated();
        var cache = requireNativeEvidence(root, evidence, manifest, digest, "native-service-cache.json");
        var cacheExecution = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("junit-service-cache.json"), evidence, 128 * 1024));
        if (!"top.egon.cola.component.yuheng.test.integration.XingyuanNativeAuthenticationIT".equals(cacheExecution.path("classname").asText())
                || cacheExecution.path("testCount").asInt() != 1 || cacheExecution.path("methods").size() != 1
                || !"configuredNativeServiceClientsRenewThroughTheRealAsWithSingleFlight".equals(cacheExecution.path("methods").get(0).asText())
                || !cacheExecution.path("reportSha256").asText().matches("[0-9a-f]{64}") || !"SYSTEM_UTC".equals(cache.path("clock").asText())
                || cache.path("excludedRuntimeRoles").size() != 1 || !"native-agent".equals(cache.path("excludedRuntimeRoles").get(0).asText())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var cacheRoles = new HashSet<String>();
        for (var proof : cache.path("proofs")) {
            var value = mapper.treeToValue(proof, ServiceCacheProofVO.class);
            try (var validators = Validation.buildDefaultValidatorFactory()) { new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(value); }
            if (value.initialTokenReference().equals(value.renewedTokenReference()) || !value.refreshTokenAbsent()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            cacheRoles.add(value.role());
        }
        if (!cacheRoles.equals(Set.copyOf(roles))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        actualApplicationsReadTheirPeersAndCallThePlatform();
        probeGeneratedPeerContracts(false);
        XingyuanResourceAuthorizationIT.observeNativeRpcAuditOwner(root, evidence, manifestFile, manifest, digest);
        probeGeneratedPeerContracts(true);
        var application = requireNativeEvidence(root, evidence, manifest, digest, "native-app-peer-platform.json");
        if (!application.path("lightActualTenantReadsConfirmed").asBoolean() || !application.path("serviceActualTokenReuseConfirmed").asBoolean()
                || application.path("proofs").size() != 2) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        publishLiveCase(root, evidence, manifest, digest, "LIVE-011", "native", "verifyNativeAuthentication",
                List.of("native-service-cache.json", "junit-service-cache.json", "native-app-peer-platform.json", "native-rpc-contract-probe.json"), false);

        var resource = new XingyuanResourceAuthorizationIT();
        resource.nativeLightGraphQlAliasesUseTheSameVerifiedTenantAsRest();
        resource.actualNativeWebGraphQlKeepsItsNonNullContractAndVerifiedTenant();
        var field = requireNativeEvidence(root, evidence, manifest, digest, "native-field-write.json");
        if (!field.path("zeroWritesDuringFieldDenial").asBoolean() || !field.path("crossTenantWriteUnaffected").asBoolean()
                || field.path("ownerProofs").size() != 5 || field.path("counterProofs").isEmpty()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        requireNativeEvidence(root, evidence, manifest, digest, "native-tenant-isolation.json");
        XingyuanResourceAuthorizationIT.requireGraphQlAsyncEvidence(requireNativeEvidence(root, evidence, manifest, digest, "native-light-graphql.json"), "native-light");
        XingyuanResourceAuthorizationIT.requireGraphQlAsyncEvidence(requireNativeEvidence(root, evidence, manifest, digest, "native-web-graphql.json"), "native-web");
        publishLiveCase(root, evidence, manifest, digest, "LIVE-013", "native", "verifyNativeAuthentication",
                List.of("native-field-write.json", "native-tenant-isolation.json", "native-light-graphql.json", "native-web-graphql.json", "native-generated-build.json"), false);
        var rpc = requireNativeEvidence(root, evidence, manifest, digest, "native-rpc-contract-probe.json");
        var data = requireNativeEvidence(root, evidence, manifest, digest, "native-rpc-user-read.json");
        if (rpc.path("proofs").size() != 4 || data.path("proofs").size() != 2 || !data.path("crossTenantIsolationConfirmed").asBoolean()) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        for (var proof : rpc.path("proofs")) {
            if (!"UNAUTHENTICATED".equals(proof.path("missingTokenStatus").asText())
                    || !"PERMISSION_DENIED".equals(proof.path("serviceSecurity").path("scopeDeniedStatus").asText())
                    || !"UNAUTHENTICATED".equals(proof.path("serviceSecurity").path("revokedStatus").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        publishLiveCase(root, evidence, manifest, digest, "LIVE-014", "native", "verifyNativeAuthentication",
                List.of("native-app-peer-platform.json", "native-rpc-contract-probe.json", "native-rpc-user-read.json", "protocol-native-rpc-audits.json", "native-rpc-audit-owner.json"), false);
        XingyuanResourceAuthorizationIT.requireRunningProcesses(root, manifestFile, manifest, digest, evidence, Set.copyOf(roles));
        if (!digest.equals(fileReference(manifestFile)) || !activeNativeRuntimeRoles(root, manifest, digest).equals(roles)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        publishLiveCase(root, evidence, manifest, digest, "LIVE-015", "native", "verifyNativeAuthentication", List.of("cases/native/LIVE-012.json"), true);
    }

    static JsonNode requireNativeEvidence(Path root, Path evidence, JsonNode manifest, String digest, String filename) throws Exception {
        XingyuanResourceAuthorizationIT.requireEvidenceDirectory(root, evidence);
        var value = UnifiedIdentityLiveClient.strictJson().readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve(filename), evidence, 128 * 1024));
        if (!value.path("schemaVersion").isInt() || value.path("schemaVersion").intValue() != 1 || !manifest.path("runId").asText().equals(value.path("runId").asText())
                || !digest.equals(value.path("manifestSha256").asText()) || !"PASSED".equals(value.path("status").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return value;
    }

    static void publishLiveCase(Path root, Path evidence, JsonNode manifest, String digest, String caseId, String runner, String method,
            List<String> filenames, boolean deferred) throws Exception {
        XingyuanResourceAuthorizationIT.requireEvidenceDirectory(root, evidence);
        if (evidence.equals(root.resolve("evidence")) || !caseId.matches("LIVE-(00[1-9]|01[0-9]|02[0-4])") || !Set.of("native", "resource").contains(runner)
                || !method.equals(runner.equals("native") ? "verifyNativeAuthentication" : "verifyResourceAuthorization")
                || runner.equals("native") != Set.of("LIVE-011", "LIVE-012", "LIVE-013", "LIVE-014", "LIVE-015").contains(caseId)
                || deferred && (!"LIVE-015".equals(caseId) || !"native".equals(runner)) || filenames.isEmpty() || filenames.size() > 32
                || deferred && !filenames.equals(List.of("cases/native/LIVE-012.json"))
                || new HashSet<>(filenames).size() != filenames.size()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var mapper = UnifiedIdentityLiveClient.strictJson(); var value = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText())
                .put("manifestSha256", digest).put("caseId", caseId).put("runner", runner).put("status", deferred ? "UNVERIFIED" : "PASSED")
                .put("executedAt", Instant.now().toString()).put("testClass", "top.egon.cola.component.yuheng.test.integration."
                        + (runner.equals("native") ? "XingyuanNativeAuthenticationIT" : "XingyuanResourceAuthorizationIT")).put("testMethod", method);
        var proofs = value.putArray("proofs");
        for (String filename : filenames) {
            Path relative = Path.of(filename); Path file = evidence.resolve(relative);
            if (relative.isAbsolute() || !relative.normalize().equals(relative) || relative.startsWith("..") || !file.startsWith(evidence)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(file, evidence, 128 * 1024);
            if (bytes.length == 0) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            proofs.addObject().put("path", root.relativize(file).toString()).put("sha256", UnifiedIdentityLiveClient.sha256(bytes));
        }
        if (deferred) {
            var gap = value.putObject("deferred").put("reasonCode", "AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED").put("acceptedOn", "2026-10-15")
                    .put("scope", "AGENT_RUNTIME_ONLY").put("sourceBuildCase", "LIVE-012");
            gap.putArray("preservedObligations").add("SOURCE_INTEGRATION").add("SIX_LAYER_ARCHITECTURE").add("SECURITY_DEFAULT_TESTS").add("GENERATED_BUILD");
        }
        for (Path directory : List.of(evidence.resolve("cases"), evidence.resolve("cases/" + runner))) {
            if (!Files.exists(directory, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(directory, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
            }
            if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory) || !Files.getPosixFilePermissions(directory).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))
                    || !Files.getOwner(directory).equals(Files.getOwner(evidence))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        publishNewNativeEvidence(root, evidence.resolve("cases/" + runner + '/' + caseId + ".json"), value);
    }

    @Test
    void liveCasePublicationBindsActualPrivateBytesAndOnlyTheAcceptedAgentGap(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); var privateDirectory = java.nio.file.attribute.PosixFilePermissions.fromString("rwx------");
        Files.setPosixFilePermissions(root, privateDirectory);
        Path all = Files.createDirectory(root.resolve("evidence"), java.nio.file.attribute.PosixFilePermissions.asFileAttribute(privateDirectory));
        Path evidence = Files.createDirectory(all.resolve("verify-" + UUID.randomUUID()), java.nio.file.attribute.PosixFilePermissions.asFileAttribute(privateDirectory));
        var mapper = UnifiedIdentityLiveClient.strictJson(); var manifest = mapper.createObjectNode().put("runId", "controlled"); String digest = "a".repeat(64);
        publishNewNativeEvidence(root, evidence.resolve("actual-build.json"), Map.of("status", "CONTROLLED_SOURCE_ONLY"));
        publishLiveCase(root, evidence, manifest, digest, "LIVE-012", "native", "verifyNativeAuthentication", List.of("actual-build.json"), false);
        Path sourceCase = evidence.resolve("cases/native/LIVE-012.json"); var source = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(sourceCase, evidence, 128 * 1024));
        assertThat(source.path("proofs").get(0).path("sha256").asText()).isEqualTo(fileReference(evidence.resolve("actual-build.json")));
        assertThat(source.path("proofs").get(0).path("path").asText()).isEqualTo(root.relativize(evidence.resolve("actual-build.json")).toString());
        publishLiveCase(root, evidence, manifest, digest, "LIVE-015", "native", "verifyNativeAuthentication", List.of("cases/native/LIVE-012.json"), true);
        var gap = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("cases/native/LIVE-015.json"), evidence, 128 * 1024));
        assertThat(gap.path("status").asText()).isEqualTo("UNVERIFIED");
        assertThat(gap.path("deferred").path("scope").asText()).isEqualTo("AGENT_RUNTIME_ONLY");
        assertThat(gap.path("deferred").path("sourceBuildCase").asText()).isEqualTo("LIVE-012");
        assertThat(gap.path("proofs").get(0).path("sha256").asText()).isEqualTo(fileReference(sourceCase));
        assertThatThrownBy(() -> publishLiveCase(root, evidence, manifest, digest, "LIVE-012", "native", "verifyNativeAuthentication", List.of("actual-build.json"), false))
                .isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
        assertThatThrownBy(() -> publishLiveCase(root, evidence, manifest, digest, "LIVE-016", "native", "verifyNativeAuthentication", List.of("actual-build.json"), false)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> publishLiveCase(root, evidence, manifest, digest, "LIVE-013", "native", "verifyNativeAuthentication", List.of("../actual-build.json"), false)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> publishLiveCase(root, evidence, manifest, digest, "LIVE-013", "native", "verifyNativeAuthentication", List.of("actual-build.json"), true)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> publishLiveCase(root, evidence, manifest, digest, "LIVE-013", "native", "verifyNativeAuthentication", List.of("actual-build.json", "actual-build.json"), false)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    record RuntimeProbeRequestVO(int schemaVersion, String runId, String manifestSha256, String role, String fixtureSha256,
                                 String artifactSha256, String nonce, String operation, String tenantCode, String entityId,
                                 @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant issuedAt,
                                 @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant expiresAt) { }

    static JsonNode validateRuntimeProbeResult(RuntimeProbeRequestVO request, JsonNode value, Instant now) {
        Set<String> fields = Set.of("schemaVersion", "runId", "manifestSha256", "role", "fixtureSha256", "artifactSha256", "nonce", "operation", "status", "completedAt", "result");
        var actual = new HashSet<String>(); value.fieldNames().forEachRemaining(actual::add);
        if (!value.isObject() || !actual.equals(fields) || !value.path("schemaVersion").isIntegralNumber() || !value.path("schemaVersion").canConvertToInt() || value.path("schemaVersion").intValue() != 1
                || !request.runId().equals(value.path("runId").asText()) || !request.manifestSha256().equals(value.path("manifestSha256").asText())
                || !request.role().equals(value.path("role").asText()) || !request.fixtureSha256().equals(value.path("fixtureSha256").asText())
                || !request.artifactSha256().equals(value.path("artifactSha256").asText()) || !request.nonce().equals(value.path("nonce").asText())
                || !request.operation().equals(value.path("operation").asText())) { throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE"); }
        String timestamp = value.path("completedAt").asText();
        Instant completed;
        try { completed = Instant.parse(timestamp); }
        catch (java.time.format.DateTimeParseException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        if (!timestamp.endsWith("Z") || completed.isBefore(request.issuedAt()) || completed.isAfter(request.expiresAt()) || completed.isAfter(now)) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        if ("UNKNOWN_COMMIT".equals(value.path("status").asText())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        if (!"COMPLETED".equals(value.path("status").asText()) || !value.path("result").isObject()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var result = value.path("result"); var resultFields = new HashSet<String>(); result.fieldNames().forEachRemaining(resultFields::add);
        if ("SECURITY_METRICS".equals(request.operation())) {
            if (!"yuheng".equals(request.role()) || !resultFields.equals(Set.of("samples")) || !result.path("samples").isArray() || result.path("samples").size() > 64) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            Set<String> identities = new HashSet<>(); Set<String> timers = Set.of("jwt.verify.duration", "token.acquire.duration", "receipt.age");
            for (var sample : result.path("samples")) {
                var sampleFields = new HashSet<String>(); sample.fieldNames().forEachRemaining(sampleFields::add);
                String signal = sample.path("signal").asText(), category = sample.path("result").asText();
                var shape = top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO.builder()
                        .signal(signal).result(category).duration(timers.contains(signal) ? Duration.ZERO : null).build();
                String type = timers.contains(signal) ? "TIMER" : signal.equals("spool.bytes") ? "GAUGE" : "COUNTER";
                if (!sample.isObject() || !sampleFields.equals(Set.of("signal", "result", "receiver", "type", "count", "value")) || !shape.isShapeValid()
                        || !"YUHENG_ADMIN".equals(sample.path("receiver").asText()) || !type.equals(sample.path("type").asText()) || !identities.add(signal + ':' + category)
                        || !sample.path("value").isNumber() || !Double.isFinite(sample.path("value").doubleValue()) || sample.path("value").doubleValue() < 0) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
                long count = XingyuanResourceAuthorizationIT.transactionProofLong(sample, "count");
                if (type.equals("COUNTER") && sample.path("value").doubleValue() != 0 || type.equals("GAUGE") && count != 0) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
            }
        } else if (Set.of("SHARED_STATE_PAUSE", "SHARED_STATE_RESUME").contains(request.operation())) {
            boolean pause = "SHARED_STATE_PAUSE".equals(request.operation());
            if (!"yuheng".equals(request.role()) || !resultFields.equals(pause ? Set.of("state", "eventLoopCount", "resumesAt") : Set.of("state", "eventLoopCount"))
                    || !(pause ? "PAUSED" : "RUNNING").equals(result.path("state").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            long loops = XingyuanResourceAuthorizationIT.transactionProofLong(result, "eventLoopCount");
            if (loops > 64 || pause && loops < 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            if (pause) {
                Instant resumes = XingyuanResourceAuthorizationIT.transactionProofInstant(result, "resumesAt");
                if (!resumes.isAfter(completed) || resumes.isAfter(request.expiresAt()) || resumes.isAfter(completed.plusSeconds(30))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            }
        } else if ("GRAPHQL_CONTEXT_COUNTS".equals(request.operation())) {
            var asyncFields = new HashSet<String>(); result.path("counts").fieldNames().forEachRemaining(asyncFields::add);
            if (!Set.of("native-light", "native-web").contains(request.role()) || !resultFields.equals(Set.of("counts"))
                    || !result.path("counts").isObject() || !asyncFields.equals(Set.of("differentThread", "matchedContext", "finallyRestored", "failure"))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            for (String key : asyncFields) { XingyuanResourceAuthorizationIT.transactionProofLong(result.path("counts"), key); }
        } else if (Set.of("BUSINESS_COUNTS", "KNOWLEDGE_COUNTS").contains(request.operation())) {
            boolean knowledge = "KNOWLEDGE_COUNTS".equals(request.operation());
            if (knowledge && !"yuheng".equals(request.role())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            Set<String> expected = knowledge ? Set.of("listKnowledgeBases", "createKnowledgeBase", "getKnowledgeBase") : switch (request.role()) {
                case "native-light" -> Set.of("create", "get");
                case "native-web" -> Set.of("createUser", "getUser");
                case "native-service" -> Set.of("create", "get", "page", "schedule");
                case "shoubing", "yuheng" -> Set.of("list");
                case "jianshen" -> Set.of("roles");
                case "tianshu" -> Set.of("pageServices");
                case "llm" -> Set.of("listLlmModels", "createLlmChatCompletion", "createLlmEmbeddings", "createLlmResponse", "createLlmMessage");
                default -> throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            };
            var countFields = new HashSet<String>(); result.path("counts").fieldNames().forEachRemaining(countFields::add);
            if (!resultFields.equals(Set.of("counts")) || !result.path("counts").isObject() || !expected.equals(countFields)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            for (String method : expected) { XingyuanResourceAuthorizationIT.transactionProofLong(result.path("counts"), method); }
        } else if ("PLATFORM_SNAPSHOT".equals(request.operation())) {
            if (!resultFields.equals(Set.of("snapshotChecksum", "authVersion", "policyVersion"))
                    || !result.path("snapshotChecksum").asText().matches("(?:sha256:)?[0-9a-f]{64}")
                    || !positiveLong(result.path("authVersion")) || !positiveLong(result.path("policyVersion"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        } else if ("SERVICE_AUTHORIZATION".equals(request.operation())) {
            if (!resultFields.equals(Set.of("tokenReference", "clientReference", "issuer", "expiresAt"))
                    || !result.path("tokenReference").asText().matches("[0-9a-f]{64}") || !result.path("clientReference").asText().matches("[0-9a-f]{64}")
                    || !result.path("issuer").asText().startsWith("https://127.0.0.1:28443/tenants/")
                    || !result.path("expiresAt").asText().endsWith("Z") || !Instant.parse(result.path("expiresAt").asText()).isAfter(completed)) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        } else {
            if (!Set.of("COURSE_CREATE", "SERVICE_USER_READ", "WEB_COURSE_READ").contains(request.operation())
                    || !resultFields.equals(Set.of("entityId", "dataSha256")) || !result.path("entityId").isTextual()
                    || !result.path("entityId").asText().matches("[1-9][0-9]{0,18}")
                    || !result.path("dataSha256").asText().matches("[0-9a-f]{64}")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            try { if (Long.parseLong(result.path("entityId").asText()) <= 0) { throw new NumberFormatException(); } }
            catch (NumberFormatException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            if (request.entityId() != null && !request.entityId().equals(result.path("entityId").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        return result;
    }

    private static boolean positiveLong(JsonNode value) { return value.isIntegralNumber() && value.canConvertToLong() && value.longValue() > 0; }

    static void publishNewNativeEvidence(Path root, Path file, Object value) throws Exception {
        if (!file.isAbsolute() || !file.equals(file.normalize()) || !file.startsWith(root.resolve("evidence"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        for (Path directory = file.getParent(); directory != null && directory.startsWith(root); directory = directory.getParent()) {
            if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    || !Files.getOwner(directory).equals(Files.getOwner(root))
                    || !Files.getPosixFilePermissions(directory).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        byte[] bytes = UnifiedIdentityLiveClient.strictJson().writeValueAsBytes(value);
        Path temporary = Files.createTempFile(file.getParent(), ".probe-", ".tmp", java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
        try {
            try (var channel = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) { channel.write(buffer); } channel.force(true);
            }
            Files.createLink(file, temporary);
            try (var directory = java.nio.channels.FileChannel.open(file.getParent(), java.nio.file.StandardOpenOption.READ)) { directory.force(true); }
        } finally { Files.deleteIfExists(temporary); }
    }

    static JsonNode invokeApplicationProbe(Path root, Path evidence, Path manifestFile, JsonNode manifest, String digest,
                                                   String role, String tenant, String operation, String entity) throws Exception {
        XingyuanResourceAuthorizationIT.requireRunningProcesses(root, manifestFile, manifest, digest, evidence, Set.of(role));
        Path fixture = root.resolve("artifacts/task-runtime-probe.jar");
        byte[] fixtureBytes = UnifiedIdentityLiveClient.protectedBytes(fixture, root.resolve("artifacts"), 2 * 1024 * 1024);
        String fixtureHash = UnifiedIdentityLiveClient.sha256(fixtureBytes);
        long matches = java.util.stream.StreamSupport.stream(manifest.path("policyFiles").spliterator(), false)
                .filter(value -> fixture.toString().equals(value.path("path").asText()) && fixtureHash.equals(value.path("sha256").asText())).count();
        boolean yuhengObservation = "yuheng".equals(role) && Set.of("SECURITY_METRICS", "SHARED_STATE_PAUSE", "SHARED_STATE_RESUME").contains(operation);
        boolean graphQlObservation = Set.of("native-light", "native-web").contains(role) && "GRAPHQL_CONTEXT_COUNTS".equals(operation);
        if (matches != 1 || !Set.of("native-light", "native-web", "native-service", "shoubing", "jianshen", "tianshu", "yuheng", "llm").contains(role)
                || !Set.of("default", "tenant-b").contains(tenant) || !Set.of("native-web", "native-service").contains(role)
                && !operation.equals("BUSINESS_COUNTS") && !yuhengObservation && !graphQlObservation) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var mapper = UnifiedIdentityLiveClient.strictJson(); Path directory = root.resolve("evidence/runtime-probe/" + role);
        var ready = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(directory.resolve("ready.json"), root.resolve("evidence"), 16 * 1024));
        String artifactHash = manifest.path("artifacts").path(role).path("sha256").asText();
        var record = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/processes.json"), root.resolve("evidence"), 128 * 1024));
        var processes = java.util.stream.StreamSupport.stream(record.path("processes").spliterator(), false).filter(value -> role.equals(value.path("id").asText())).toList();
        if (processes.size() != 1 || !manifest.path("runId").asText().equals(ready.path("runId").asText()) || !digest.equals(ready.path("manifestSha256").asText())
                || !role.equals(ready.path("role").asText()) || !fixtureHash.equals(ready.path("fixtureSha256").asText()) || !artifactHash.equals(ready.path("artifactSha256").asText())
                || !"READY".equals(ready.path("status").asText()) || !positiveLong(ready.path("pid")) || ready.path("pid").longValue() != processes.getFirst().path("pid").longValue()
                || !ready.path("processStartedAt").asText().equals(processes.getFirst().path("startInstant").asText())) { throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE"); }
        Instant issued = Instant.now(); var request = new RuntimeProbeRequestVO(1, manifest.path("runId").asText(), digest, role, fixtureHash, artifactHash,
                java.util.UUID.randomUUID().toString(), operation, tenant, entity, issued, issued.plusSeconds(60));
        Path requests = directory.resolve("requests");
        // A protected existing ready file proves these directories were initialized by the running owner.
        if (!Files.isDirectory(requests, java.nio.file.LinkOption.NOFOLLOW_LINKS) || !Files.getPosixFilePermissions(requests).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))) {
            throw new IllegalStateException("XINGYUAN_TARGET_UNSAFE");
        }
        Path requestFile = requests.resolve(request.nonce() + ".json");
        publishNewNativeEvidence(root, requestFile, request);
        Path resultFile = directory.resolve("results/" + request.nonce() + ".json");
        while (!Files.exists(resultFile, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            if (!request.expiresAt().isAfter(Instant.now())) { throw new IllegalStateException("COURSE_CREATE".equals(operation) ? "XINGYUAN_COMMIT_UNKNOWN" : "XINGYUAN_DEPENDENCY_UNAVAILABLE"); }
            Thread.sleep(100);
        }
        byte[] resultBytes = UnifiedIdentityLiveClient.protectedBytes(resultFile, root.resolve("evidence"), 128 * 1024);
        var envelope = mapper.readTree(resultBytes); var result = validateRuntimeProbeResult(request, envelope, Instant.now());
        XingyuanResourceAuthorizationIT.requireRunningProcesses(root, manifestFile, manifest, digest, evidence, Set.of(role));
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", request.runId()).put("manifestSha256", digest)
                .put("scope", "ACTUAL_APP_OWNER_ONLY").put("role", role).put("operation", operation).put("tenantCode", tenant).put("nonce", request.nonce())
                .put("ownerProofSha256", UnifiedIdentityLiveClient.sha256(resultBytes)).put("recordedAt", Instant.now().toString());
        proof.set("result", result);
        publishNewNativeEvidence(root, evidence.resolve("native-app-" + request.nonce() + ".json"), proof);
        return result;
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualApplicationsReadTheirPeersAndCallThePlatform() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(bytes);
        String digest = UnifiedIdentityLiveClient.sha256(bytes); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        XingyuanResourceAuthorizationIT.requireEvidenceDirectory(root, evidence);
        var proofs = mapper.createArrayNode();
        try (var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue())).build()) {
            var web = new XingyuanResourceAuthorizationIT.NativeResourceHttpProbe("native-web", root, manifestFile, digest, http,
                    Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()));
            for (String tenant : List.of("default", "tenant-b")) {
                var reference = client.formal(tenant, client.resourceForProcess("native-web"), "admin"); client.verifiedClaims(reference);
                var initialAuthorization = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-service", tenant, "SERVICE_AUTHORIZATION", null);
                String name = "xingyuan-it-" + manifest.path("runId").asText() + '-' + tenant + '-' + java.util.UUID.randomUUID();
                var user = web.request("POST", "/api/v1/users", reference, mapper.createObjectNode().put("name", name).put("email", name + "@xingyuan.invalid"), null, 201);
                String userId = Long.toString(XingyuanResourceAuthorizationIT.responseId(user));
                var observedUser = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-service", tenant, "SERVICE_USER_READ", userId);
                var data = mapper.createObjectNode().put("id", userId).put("name", user.path("name").asText()).put("status", user.path("status").asText());
                if (!UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(data)).equals(observedUser.path("dataSha256").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var createdCourse = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-service", tenant, "COURSE_CREATE", null);
                var readCourse = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-web", tenant, "WEB_COURSE_READ", createdCourse.path("entityId").asText());
                if (!createdCourse.equals(readCourse)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var authorization = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-service", tenant, "SERVICE_AUTHORIZATION", null);
                if (!initialAuthorization.path("tokenReference").equals(authorization.path("tokenReference"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var snapshot = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-web", tenant, "PLATFORM_SNAPSHOT", null);
                proofs.add(mapper.createObjectNode().put("tenantCode", tenant).put("userReadReference", observedUser.path("dataSha256").asText())
                        .put("courseReference", createdCourse.path("dataSha256").asText()).put("serviceTokenReference", authorization.path("tokenReference").asText())
                        .put("snapshotChecksum", snapshot.path("snapshotChecksum").asText()));
            }
        }
        var lightA = client.formal("default", client.resourceForProcess("native-light"), "admin");
        var lightB = client.formal("tenant-b", client.resourceForProcess("native-light"), "admin");
        client.bindNativeTenantReadEndpoints(lightA, lightB);
        if (client.read(lightA) != 200 || client.read(lightB) != 200) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("lightActualTenantReadsConfirmed", true).put("serviceActualTokenReuseConfirmed", true)
                .put("scope", "ACTUAL_APP_PEER_AND_PLATFORM_CALLS_ONLY").put("status", "PASSED").put("recordedAt", Instant.now().toString());
        proof.set("proofs", proofs);
        Path output = evidence.resolve("native-app-peer-platform.json");
        publishNewNativeEvidence(root, output, proof);
    }


    @Test
    void applicationProbeProofsRequireTheExactOwnerCommandAndRetainUnknownCommits() throws Exception {
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var request = new RuntimeProbeRequestVO(1, "run", "a".repeat(64), "native-web", "b".repeat(64), "c".repeat(64),
                java.util.UUID.randomUUID().toString(), "WEB_COURSE_READ", "default", "701", now, now.plusSeconds(60));
        var mapper = UnifiedIdentityLiveClient.strictJson();
        var envelope = mapper.createObjectNode().put("schemaVersion", 1).put("runId", request.runId()).put("manifestSha256", request.manifestSha256())
                .put("role", request.role()).put("fixtureSha256", request.fixtureSha256()).put("artifactSha256", request.artifactSha256())
                .put("nonce", request.nonce()).put("operation", request.operation()).put("status", "COMPLETED").put("completedAt", now.plusSeconds(1).toString());
        envelope.putObject("result").put("entityId", "701").put("dataSha256", "d".repeat(64));
        assertThat(validateRuntimeProbeResult(request, envelope, now.plusSeconds(2))).isEqualTo(envelope.path("result"));
        for (String field : List.of("runId", "manifestSha256", "role", "fixtureSha256", "artifactSha256", "nonce", "operation")) {
            var wrong = envelope.deepCopy().put(field, "foreign");
            assertThatThrownBy(() -> validateRuntimeProbeResult(request, wrong, now.plusSeconds(2))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var numeric = envelope.deepCopy().put("schemaVersion", 1.5);
        assertThatThrownBy(() -> validateRuntimeProbeResult(request, numeric, now.plusSeconds(2))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var leaked = envelope.deepCopy().put("accessToken", "forbidden");
        assertThatThrownBy(() -> validateRuntimeProbeResult(request, leaked, now.plusSeconds(2))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var unknown = envelope.deepCopy().put("status", "UNKNOWN_COMMIT");
        assertThatThrownBy(() -> validateRuntimeProbeResult(request, unknown, now.plusSeconds(2))).hasMessage("XINGYUAN_COMMIT_UNKNOWN");
        var foreignEntity = envelope.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) foreignEntity.path("result")).put("entityId", "702");
        assertThatThrownBy(() -> validateRuntimeProbeResult(request, foreignEntity, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThat(mapper.valueToTree(request).path("issuedAt").isTextual()).isTrue();
        assertThat(mapper.valueToTree(request).path("issuedAt").asText()).isEqualTo(now.toString());
    }

    @Test
    void asyncCounterProofsStayOnTheTwoApprovedHostsAndCannotCarryIdentityFields() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.parse("2026-10-15T00:00:00Z");
        for (String role : List.of("native-light", "native-web", "native-service", "yuheng")) {
            var request = new RuntimeProbeRequestVO(1, "run", "a".repeat(64), role, "b".repeat(64), "c".repeat(64), UUID.randomUUID().toString(),
                    "GRAPHQL_CONTEXT_COUNTS", "default", null, now, now.plusSeconds(60));
            var envelope = mapper.createObjectNode().put("schemaVersion", 1).put("runId", request.runId()).put("manifestSha256", request.manifestSha256())
                    .put("role", role).put("fixtureSha256", request.fixtureSha256()).put("artifactSha256", request.artifactSha256())
                    .put("nonce", request.nonce()).put("operation", request.operation()).put("status", "COMPLETED").put("completedAt", now.plusSeconds(1).toString());
            envelope.putObject("result").putObject("counts").put("differentThread", 2).put("matchedContext", 2).put("finallyRestored", 2).put("failure", 0);
            if (Set.of("native-light", "native-web").contains(role)) {
                assertThatCode(() -> validateRuntimeProbeResult(request, envelope, now.plusSeconds(2))).doesNotThrowAnyException();
                for (String fault : List.of("negative", "missing", "secret")) {
                    var wrong = envelope.deepCopy(); var counts = wrong.withObject("result").withObject("counts");
                    if (fault.equals("negative")) { counts.put("failure", -1); }
                    else if (fault.equals("missing")) { counts.remove("finallyRestored"); }
                    else { counts.put("subject", "forbidden"); }
                    assertThatThrownBy(() -> validateRuntimeProbeResult(request, wrong, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
                }
            } else { assertThatThrownBy(() -> validateRuntimeProbeResult(request, envelope, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED"); }
        }
    }

    @Test
    void businessCounterResultsRequireExactMethodsAndNonnegativeIntegralCounts() {
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var request = new RuntimeProbeRequestVO(1, "run", "a".repeat(64), "native-light", "b".repeat(64), "c".repeat(64),
                java.util.UUID.randomUUID().toString(), "BUSINESS_COUNTS", "default", null, now, now.plusSeconds(60));
        var mapper = UnifiedIdentityLiveClient.strictJson();
        var envelope = mapper.createObjectNode().put("schemaVersion", 1).put("runId", request.runId()).put("manifestSha256", request.manifestSha256())
                .put("role", request.role()).put("fixtureSha256", request.fixtureSha256()).put("artifactSha256", request.artifactSha256())
                .put("nonce", request.nonce()).put("operation", request.operation()).put("status", "COMPLETED").put("completedAt", now.plusSeconds(1).toString());
        envelope.putObject("result").putObject("counts").put("create", 0L).put("get", 1L);
        assertThatCode(() -> validateRuntimeProbeResult(request, envelope, now.plusSeconds(2))).doesNotThrowAnyException();
        for (String fault : List.of("negative", "float", "string", "overflow", "extra", "missing", "secret")) {
            var wrong = envelope.deepCopy(); var result = wrong.withObject("result"); var counts = result.withObject("counts");
            if (fault.equals("negative")) { counts.put("get", -1); }
            else if (fault.equals("float")) { counts.put("get", 1.5); }
            else if (fault.equals("string")) { counts.put("get", "1"); }
            else if (fault.equals("overflow")) { counts.put("get", new java.math.BigInteger("9223372036854775808")); }
            else if (fault.equals("extra")) { counts.put("unknown", 0); }
            else if (fault.equals("missing")) { counts.remove("get"); }
            else { result.put("accessToken", "forbidden"); }
            assertThatThrownBy(() -> validateRuntimeProbeResult(request, wrong, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        var before = mapper.createObjectNode(); before.putObject("counts").put("get", 5).put("create", 2);
        var after = before.deepCopy(); after.withObject("counts").put("get", 6);
        assertThatCode(() -> requireOnlyBusinessMethodDelta(before, after, "get", 1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireOnlyBusinessMethodDelta(before, after, "get", 0)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        after.withObject("counts").put("create", 3);
        assertThatThrownBy(() -> requireOnlyBusinessMethodDelta(before, after, "get", 1)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var aliases = before.deepCopy(); aliases.withObject("counts").put("get", 7);
        assertThatCode(() -> requireOnlyBusinessMethodDelta(before, aliases, "get", 2)).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireOnlyBusinessMethodDelta(before, aliases, "get", 3)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var overflow = before.deepCopy(); overflow.withObject("counts").put("get", Long.MAX_VALUE);
        assertThatThrownBy(() -> requireOnlyBusinessMethodDelta(overflow, aliases, "get", 2)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var knowledgeRequest = new RuntimeProbeRequestVO(1, "run", "a".repeat(64), "yuheng", "b".repeat(64), "c".repeat(64),
                request.nonce(), "KNOWLEDGE_COUNTS", "default", null, now, now.plusSeconds(60));
        var knowledge = envelope.deepCopy().put("role", "yuheng").put("operation", "KNOWLEDGE_COUNTS");
        knowledge.withObject("result").withObject("counts").removeAll().put("listKnowledgeBases", 0).put("createKnowledgeBase", 0).put("getKnowledgeBase", 0);
        assertThatCode(() -> validateRuntimeProbeResult(knowledgeRequest, knowledge, now.plusSeconds(2))).doesNotThrowAnyException();
        var foreignKnowledge = knowledge.deepCopy(); foreignKnowledge.withObject("result").withObject("counts").put("readDocuments", 0);
        assertThatThrownBy(() -> validateRuntimeProbeResult(knowledgeRequest, foreignKnowledge, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    void taskMetricsAndSharedStateControlProofsHaveOnlyBoundedYuhengFacts() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.parse("2026-10-15T00:00:00Z");
        for (String operation : List.of("SECURITY_METRICS", "SHARED_STATE_PAUSE", "SHARED_STATE_RESUME")) {
            var request = new RuntimeProbeRequestVO(1, "run", "a".repeat(64), "yuheng", "b".repeat(64), "c".repeat(64), UUID.randomUUID().toString(), operation, "default", null, now, now.plusSeconds(60));
            var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "run").put("manifestSha256", request.manifestSha256()).put("role", "yuheng")
                    .put("fixtureSha256", request.fixtureSha256()).put("artifactSha256", request.artifactSha256()).put("nonce", request.nonce()).put("operation", operation)
                    .put("status", "COMPLETED").put("completedAt", now.plusSeconds(1).toString());
            var result = proof.putObject("result");
            if (operation.equals("SECURITY_METRICS")) {
                result.putArray("samples").addObject().put("signal", "oauth.authentication").put("result", "VALID").put("receiver", "YUHENG_ADMIN").put("type", "COUNTER").put("count", 3).put("value", 0);
            } else {
                result.put("state", operation.equals("SHARED_STATE_PAUSE") ? "PAUSED" : "RUNNING").put("eventLoopCount", 2);
                if (operation.equals("SHARED_STATE_PAUSE")) { result.put("resumesAt", now.plusSeconds(30).toString()); }
            }
            assertThatCode(() -> validateRuntimeProbeResult(request, proof, now.plusSeconds(2))).doesNotThrowAnyException();
            var extra = proof.deepCopy(); extra.withObject("result").put("token", "forbidden");
            assertThatThrownBy(() -> validateRuntimeProbeResult(request, extra, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
            if (operation.equals("SECURITY_METRICS")) {
                for (String fault : List.of("identity", "receiver", "unknown", "nan", "wrongType")) {
                    var invalid = proof.deepCopy(); var sample = (com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("result").path("samples").get(0);
                    if (fault.equals("identity")) { sample.put("subject", "forbidden"); }
                    else if (fault.equals("receiver")) { sample.put("receiver", "APPLICATION"); }
                    else if (fault.equals("unknown")) { sample.put("signal", "unbounded"); }
                    else if (fault.equals("nan")) { sample.put("value", Double.NaN); }
                    else { sample.put("type", "GAUGE"); }
                    assertThatThrownBy(() -> validateRuntimeProbeResult(request, invalid, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
                }
            } else {
                var excessive = proof.deepCopy(); excessive.withObject("result").put("eventLoopCount", 65);
                assertThatThrownBy(() -> validateRuntimeProbeResult(request, excessive, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
                if (operation.equals("SHARED_STATE_PAUSE")) {
                    var late = proof.deepCopy(); late.withObject("result").put("resumesAt", now.plusSeconds(40).toString());
                    assertThatThrownBy(() -> validateRuntimeProbeResult(request, late, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
                }
            }
        }
    }

    @Test
    void applicationProbeEvidenceIsPrivateAtomicAndCannotReplaceAnotherResult(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); Files.setPosixFilePermissions(root, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        Path evidence = Files.createDirectory(root.resolve("evidence"), java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
        Path file = evidence.resolve("owned.json"); publishNewNativeEvidence(root, file, Map.of("status", "controlled"));
        byte[] before = Files.readAllBytes(file);
        assertThat(Files.getPosixFilePermissions(file)).isEqualTo(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        assertThatThrownBy(() -> publishNewNativeEvidence(root, file, Map.of("status", "replacement"))).isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
        assertThat(Files.readAllBytes(file)).isEqualTo(before);
        Path link = evidence.resolve("link.json"); Files.createSymbolicLink(link, file);
        assertThatThrownBy(() -> publishNewNativeEvidence(root, link, Map.of())).isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
        Files.setPosixFilePermissions(evidence, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        assertThatThrownBy(() -> publishNewNativeEvidence(root, evidence.resolve("unsafe.json"), Map.of())).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void runtimeRoleSelectionRequiresTheAcceptedAgentPolicyAndMatchingProfileProof(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); Files.setPosixFilePermissions(root, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        for (String name : List.of("policies", "evidence")) { Files.createDirectory(root.resolve(name), java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))); }
        var mapper = UnifiedIdentityLiveClient.strictJson(); var manifest = mapper.createObjectNode().put("runId", "controlled"); manifest.putArray("policyFiles");
        assertThat(activeNativeRuntimeRoles(root, manifest, "a".repeat(64))).containsExactly("native-light", "native-web", "native-service", "native-agent");
        var policy = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "controlled").put("role", "native-agent").put("status", "UNVERIFIED")
                .put("reasonCode", "AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED").put("acceptedOn", "2026-10-15");
        Path file = root.resolve("policies/agent-runtime-deferred.json"); Files.write(file, mapper.writeValueAsBytes(policy)); Files.setPosixFilePermissions(file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        assertThatThrownBy(() -> activeNativeRuntimeRoles(root, manifest, "a".repeat(64))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        String hash = fileReference(file); ((com.fasterxml.jackson.databind.node.ArrayNode) manifest.path("policyFiles")).addObject().put("path", file.toString()).put("sha256", hash);
        var ready = mapper.createObjectNode().put("runId", "controlled").put("manifestSha256", "a".repeat(64)); ready.putObject("runtimeExclusions").put("native-agent", hash);
        publishNewNativeEvidence(root, root.resolve("evidence/profiles-ready.json"), ready);
        assertThat(activeNativeRuntimeRoles(root, manifest, "a".repeat(64))).containsExactly("native-light", "native-web", "native-service");
        manifest.putObject("deadlines").put("operationSeconds", 900);
        var constructor = UnifiedIdentityLiveClient.IsolatedResourceProbe.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        var client = (UnifiedIdentityLiveClient.IsolatedResourceProbe) constructor.newInstance(root, manifest, mapper.createObjectNode(), mapper.createObjectNode(),
                "a".repeat(64), null, Duration.ofSeconds(30), null);
        var cacheReady = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "controlled").put("manifestSha256", "a".repeat(64))
                .put("status", "READY").put("phase", "INITIAL_VERIFIED").put("recordedAt", Instant.now().toString()).put("requestCount", 3).put("profileCount", 3);
        assertThatCode(() -> client.publishNativeServiceCacheReady(root.resolve("evidence"), cacheReady)).doesNotThrowAnyException();
        var cache = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "controlled").put("manifestSha256", "a".repeat(64))
                .put("status", "PASSED").put("clock", "SYSTEM_UTC").put("completedAt", Instant.now().toString());
        cache.putArray("excludedRuntimeRoles").add("native-agent");
        var observations = cache.putArray("proofs"); int ordinal = 1;
        for (String role : List.of("native-light", "native-web", "native-service")) {
            observations.add(mapper.valueToTree(new ServiceCacheProofVO(role, Integer.toString(ordinal++).repeat(64), "b".repeat(64), "c".repeat(64),
                    1, 2, 8, 8, true, Instant.now().plusSeconds(300), Instant.now())));
        }
        assertThatCode(() -> client.publishNativeServiceCacheProof(root.resolve("evidence"), cache)).doesNotThrowAnyException();
        var forged = cache.deepCopy(); forged.withArray("excludedRuntimeRoles").removeAll().add("native-service");
        assertThatThrownBy(() -> client.publishNativeServiceCacheProof(root.resolve("evidence"), forged)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> activeNativeRuntimeRoles(root, manifest, "b".repeat(64))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        policy.put("role", "native-service"); Files.write(file, mapper.writeValueAsBytes(policy));
        assertThatThrownBy(() -> activeNativeRuntimeRoles(root, manifest, "a".repeat(64))).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    static String nativeFacadeEntry(List<String> entries, String namespace) {
        if (namespace == null || !namespace.matches("[a-z][a-z0-9-]{2,127}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var candidates = entries.stream().filter(value -> value.startsWith("BOOT-INF/lib/" + namespace + "-facade-") && value.endsWith(".jar")
                && !value.endsWith("-sources.jar") && !value.endsWith("-javadoc.jar") && !value.contains("..")
                && value.substring("BOOT-INF/lib/".length()).indexOf('/') < 0).toList();
        if (candidates.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return candidates.getFirst();
    }

    @Test
    void actualRpcProofPublisherKeepsBothObservedRequestsAndRejectsDuplicateOrReplacedTraces(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); Files.setPosixFilePermissions(root, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        Path evidence = Files.createDirectory(root.resolve("evidence"), java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
        var mapper = UnifiedIdentityLiveClient.strictJson(); var manifest = mapper.createObjectNode().put("runId", "controlled"); manifest.putObject("deadlines").put("operationSeconds", 120);
        var registry = mapper.createObjectNode(); registry.putObject("tenantIds").put("default", "1001").put("tenant-b", "1002");
        var constructor = UnifiedIdentityLiveClient.IsolatedResourceProbe.class.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        var client = (UnifiedIdentityLiveClient.IsolatedResourceProbe) constructor.newInstance(root, manifest, registry, mapper.createObjectNode(), "a".repeat(64), null, Duration.ofSeconds(30), null);
        Instant now = Instant.now(); var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "controlled").put("manifestSha256", "a".repeat(64))
                .put("status", "PASSED").put("scope", "STANDALONE_SERVICE_TO_WEB_DATA").put("recordedAt", now.toString()).put("crossTenantIsolationConfirmed", true);
        var items = proof.putArray("proofs"); int index = 0;
        for (String tenant : List.of("1001", "1002")) {
            var item = items.addObject().put("consumerRole", "native-service").put("providerRole", "native-web").put("tenantId", tenant)
                    .put("methodReference", "b".repeat(64)).put("descriptorReference", "c".repeat(64)).put("leaseReference", "d".repeat(64))
                    .put("leaseRevision", 1).put("leaseExpiresAt", now.plusSeconds(60).toString()).put("businessStatus", "USER_DATA_MATCHED")
                    .put("missingTokenStatus", "UNAUTHENTICATED").put("responseReference", "e".repeat(64)).put("httpReference", "f".repeat(64));
            item.putObject("beforeBusiness").putObject("counts").put("createUser", 0).put("getUser", 0);
            item.putObject("successfulBusiness").putObject("counts").put("createUser", 0).put("getUser", 1);
            int count = tenant.equals("1002") ? 2 : 1;
            item.putObject("beforeMissingToken").putObject("counts").put("createUser", 0).put("getUser", count);
            item.putObject("afterMissingToken").putObject("counts").put("createUser", 0).put("getUser", count);
            item.putObject("successfulRequest").put("traceId", String.format(java.util.Locale.ROOT, "%032x", ++index)).put("status", "OK")
                    .put("startedAt", now.minusSeconds(2).toString()).put("completedAt", now.minusSeconds(1).toString());
            item.putObject("missingTokenRequest").put("traceId", String.format(java.util.Locale.ROOT, "%032x", ++index)).put("status", "UNAUTHENTICATED")
                    .put("startedAt", now.minusSeconds(1).toString()).put("completedAt", now.toString());
        }
        for (String fault : List.of("missing", "duplicate", "status", "future", "secret")) {
            var invalid = proof.deepCopy(); var first = (com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("proofs").get(0);
            if (fault.equals("missing")) { first.remove("successfulRequest"); }
            else if (fault.equals("duplicate")) { ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("proofs").get(1).path("missingTokenRequest")).put("traceId", "0".repeat(31) + '1'); }
            else if (fault.equals("status")) { first.withObject("missingTokenRequest").put("status", "OK"); }
            else if (fault.equals("future")) { first.withObject("successfulRequest").put("completedAt", now.plusSeconds(1).toString()); }
            else { first.withObject("successfulRequest").put("token", "untrusted"); }
            assertThatThrownBy(() -> client.publishNativeRpcProtocolProof(evidence, invalid)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        assertThatCode(() -> client.publishNativeRpcProtocolProof(evidence, proof)).doesNotThrowAnyException();
        assertThat(mapper.readTree(Files.readAllBytes(evidence.resolve("native-rpc-user-read.json")))).isEqualTo(proof);
    }

    @Test
    void nativeFacadeSelectionRejectsForeignNamespaceAndAmbiguousArchives() {
        String owned = "BOOT-INF/lib/owned-native-facade-0.1.jar";
        assertThat(nativeFacadeEntry(List.of(owned), "owned-native")).isEqualTo(owned);
        assertThatThrownBy(() -> nativeFacadeEntry(List.of("BOOT-INF/lib/foreign-native-facade-0.1.jar"), "owned-native"))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> nativeFacadeEntry(List.of(owned, "BOOT-INF/lib/owned-native-facade-0.2.jar"), "owned-native"))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> nativeFacadeEntry(List.of("BOOT-INF/lib/owned-native-facade-0.1-sources.jar"), "owned-native"))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> nativeFacadeEntry(List.of("BOOT-INF/lib/owned-native-facade-../foreign.jar"), "owned-native"))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void peerLeaseRejectsForeignAddressExpiredAndAmbiguousInstancesBeforeAChannelExists() {
        Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var key = new DdcServiceKey("owned-biz", "test", "owned-app", DdcServiceKind.RPC_PROVIDER, "owned.Service", "course", "1.0.0", "grpc");
        java.util.function.Function<String, DdcServiceInstance> endpoint = host -> new DdcServiceInstance("owned-instance", "owned-lease", key,
                host, 29312, true, Map.of(), 60, 20, now.minusSeconds(10), now.minusSeconds(1), now.plusSeconds(60), "ONLINE", 1,
                "owned-resource", 0L, "owned-credential", now.plusSeconds(300));
        var valid = new DdcServiceSnapshot(key, 1, List.of(endpoint.apply("127.0.0.1")), now);
        assertThat(requirePeerLease(valid, key, 29312, "owned-resource", now)).isEqualTo(valid.instances().getFirst());
        assertThatThrownBy(() -> requirePeerLease(new DdcServiceSnapshot(key, 1, List.of(endpoint.apply("192.0.2.1")), now), key, 29312, "owned-resource", now))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> requirePeerLease(valid, key, 29311, "owned-resource", now)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> requirePeerLease(valid, key, 29312, "foreign-resource", now)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> requirePeerLease(valid, key, 29312, "owned-resource", now.plusSeconds(61))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> requirePeerLease(new DdcServiceSnapshot(key, 1, List.of(endpoint.apply("127.0.0.1"), endpoint.apply("127.0.0.1")), now),
                key, 29312, "owned-resource", now)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "xingyuan.isolated.contract-artifact", matches = ".+")
    void generatedArtifactContractLoadsOfflineWithoutStartingAProvider(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath();
        Files.setPosixFilePermissions(root, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        Path artifact = Path.of(System.getProperty("xingyuan.isolated.contract-artifact"));
        String namespace = System.getProperty("xingyuan.isolated.contract-namespace");
        String role = System.getProperty("xingyuan.isolated.contract-role", "native-service");
        String suffix = role.equals("native-service") ? "/facade/course/CourseFacade.class" : "/facade/user/UserFacade.class";
        var mapper = UnifiedIdentityLiveClient.strictJson();
        var manifest = mapper.createObjectNode().put("repoRoot", System.getProperty("xingyuan.isolated.repository-root"));
        manifest.putObject("artifacts").putObject(role).put("path", artifact.toString()).put("sha256", fileReference(artifact));
        try (var factory = Validation.buildDefaultValidatorFactory();
             var contract = generatedContract(root, manifest, role, namespace, suffix, factory.getValidator())) {
            assertThat(contract.contract.methods()).isNotEmpty();
            String operation = role.equals("native-service") ? "GetCourse" : "GetUser";
            var method = contract.contract.methods().stream().filter(value -> operation.equals(value.methodName())).findFirst().orElseThrow();
            var request = rpcIdRequest(method, role.equals("native-service") ? "course_id" : "user_id", Long.MAX_VALUE);
            assertThat(method.requestType().isInstance(request)).isTrue();
            assertThat(request.getDescriptorForType().getFullName()).isEqualTo(method.protoMethod().getInputType().getFullName());
            assertThat(method.fullMethodName()).isEqualTo(contract.contract.serviceName() + "/" + operation);
            var noTransport = new top.egon.cola.component.rpc.consumer.channel.RpcInvocationChannelProvider() {
                @Override public io.grpc.ManagedChannel currentChannel(Set<io.grpc.ManagedChannel> excluded) { throw new AssertionError("Offline contract check must not acquire a Channel"); }
                @Override public void recordFailure(io.grpc.ManagedChannel channel) { throw new AssertionError("Offline contract check must not invoke transport"); }
                @Override public int maxAttempts() { return 1; }
            };
            var proxy = new top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory(
                    new RpcContractValidator(new top.egon.cola.component.common.core.validation.ValidationUtils(factory.getValidator())), noTransport,
                    new top.egon.cola.component.rpc.context.identity.RpcProcessIdentity("controlled-offline-proxy", "test", "127.0.0.1", ProcessHandle.current().pid(), "controlled"),
                    new top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper(), 1000).create(contract.contract.contractType(), 1000);
            assertThat(contract.contract.contractType().isInstance(proxy)).isTrue();
        }
    }

    record RpcProbeTargetVO(@NotBlank @Pattern(regexp = "native-web|native-service") String consumerRole,
                            @NotBlank @Pattern(regexp = "native-web|native-service") String providerRole,
                            @NotBlank String facadeSuffix, @NotBlank String operation, @NotBlank String idField,
                            @NotBlank String notFoundCode, @NotBlank String targetPrefix, @Min(29311) @Max(29312) int port) { }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualGeneratedPeerContractsUseHmacDiscoveryAndServiceCredentials() throws Exception {
        probeGeneratedPeerContracts(false);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void nativeServiceReadsUsersCreatedByTheActualNativeWebApi() throws Exception {
        probeGeneratedPeerContracts(true);
    }

    private void probeGeneratedPeerContracts(boolean createWebUsers) throws Exception {
        var client = UnifiedIdentityLiveClient.isolated();
        var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024);
        var manifest = mapper.readTree(bytes);
        String digest = UnifiedIdentityLiveClient.sha256(bytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        XingyuanResourceAuthorizationIT.requireEvidenceDirectory(root, evidence);
        XingyuanResourceAuthorizationIT.requireRunningProcesses(root, manifestFile, manifest, digest, evidence,
                Set.of("tianshu", "native-web", "native-service"));
        initializeStandaloneRpcInvocationIds(root, manifest, digest);
        var registry = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024));
        var proofs = mapper.createArrayNode();
        for (var target : List.of(
                new RpcProbeTargetVO("native-web", "native-service", "/facade/course/CourseFacade.class", "GetCourse", "course_id", "COURSE_NOT_FOUND", "organization.integrations.evaluation", 29312),
                new RpcProbeTargetVO("native-service", "native-web", "/facade/user/UserFacade.class", "GetUser", "user_id", "ORG_NOT_FOUND", "app.integrations.organization", 29311))) {
            if (createWebUsers && !target.providerRole().equals("native-web")) { continue; }
            String namespace = client.nativeApplicationCode(target.providerRole());
            var properties = XingyuanResourceAuthorizationIT.verifiedProfile(root, manifest, digest, target.consumerRole());
            var flat = new LinkedHashMap<String, Object>(); properties.forEach((key, value) -> flat.put(key.toString(), value)); properties.clear();
            var binder = new Binder(new MapConfigurationPropertySource(flat));
            var core = binder.bind("egon.cola.component.rpc", Bindable.of(EgonRpcProperties.class)).orElseThrow(() -> new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"));
            var ddc = binder.bind("egon.cola.component.tianshu", Bindable.of(DdcProperties.class)).orElseThrow(() -> new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"));
            var storage = binder.bind("egon.cola.component.tianshu", Bindable.of(DdcTenantProperties.class)).orElseThrow(() -> new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"));
            var transport = binder.bind("egon.cola.component.tianshu.rpc", Bindable.of(DdcRpcProperties.class)).orElseThrow(() -> new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"));
            if (!transport.getAuth().isEnabled() || !transport.getTls().isEnabled() || !"127.0.0.1:29080".equals(transport.requireTarget())) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            requireTaskRpcTls(root, manifest, transport.transportSecurity());
            var environment = new org.springframework.core.env.StandardEnvironment();
            environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("reviewed-native-profile", flat));
            core.getIdentity().setInstanceId("xingyuan-it-probe-" + java.util.UUID.randomUUID());
            var identity = new RpcProcessIdentityFactory(environment, core).create();
            var tls = new RpcTransportSecurity(core.getTls().isEnabled(), core.getTls().isDevelopmentPlaintext(), core.getTls().getCertificateChainPath(),
                    core.getTls().getPrivateKeyPath(), core.getTls().getTrustCertificateCollectionPath());
            requireTaskRpcTls(root, manifest, tls);
            var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                    .filter(value -> namespace.equals(value.path("rbacApplicationCode").asText()) && "ACTIVE".equals(value.path("status").asText())).toList();
            if (resources.size() != 1) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            var providerProfile = XingyuanResourceAuthorizationIT.verifiedProfile(root, manifest, digest, target.providerRole());
            String registrationUri = providerProfile.getProperty("egon.cola.component.tianshu.registration-resource-uri",
                    resources.getFirst().path("resourceUri").asText());
            providerProfile.clear();
            var registrationResources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                    .filter(value -> registrationUri.equals(value.path("resourceUri").asText()) && "ACTIVE".equals(value.path("status").asText())).toList();
            if (registrationResources.size() != 1) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            try (var validators = Validation.buildDefaultValidatorFactory();
                 var source = serviceContext(root, manifest, digest, registry, target.consumerRole());
                 var contract = generatedContract(root, manifest, target.providerRole(), namespace, target.facadeSuffix(), validators.getValidator());
                 var directory = new DdcRpcClientFactory(transport, ddc, storage, identity).registryClient()) {
                new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(target);
                var methods = contract.contract.methods().stream().filter(value -> target.operation().equals(value.methodName())).toList();
                if (methods.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                RpcMethodDescriptor method = methods.getFirst();
                var candidates = source.properties.getRpc().getConsumer().getBindings().values().stream()
                        .filter(value -> method.fullMethodName().equals(value.getFullMethodName())).toList();
                if (candidates.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var binding = candidates.getFirst();
                new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(binding);
                if (binding.getMode() != IdpStarterProperties.RpcConsumerModeEnum.SERVICE || binding.getScopeContext() != ServiceTokenContext.TENANT
                        || !binding.getServiceName().equals(contract.contract.serviceName()) || !binding.getGroup().equals(contract.contract.group())
                        || !binding.getVersion().equals(contract.contract.version())
                        || !identity.applicationName().equals(binding.getProcessIdentity().get("applicationName")) || !identity.env().equals(binding.getProcessIdentity().get("env"))
                        || !binding.getScopes().equals(Set.of(namespace + ":" + (target.operation().equals("GetCourse") ? "course:read" : "user:read")))
                        || !binding.getResourceUri().toASCIIString().equals(resources.getFirst().path("resourceUri").asText())) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
                String biz = environment.getProperty(target.targetPrefix() + ".biz-code");
                String app = environment.getProperty(target.targetPrefix() + ".app-code");
                var key = new DdcServiceKey(biz, identity.env(), app, DdcServiceKind.RPC_PROVIDER, contract.contract.serviceName(), binding.getGroup(), binding.getVersion(), "grpc");
                var snapshot = directory.client().getInstances(key);
                var endpoint = requirePeerLease(snapshot, key, target.port(), registrationResources.getFirst().path("resourceServerId").asText(), Instant.now());
                var settings = RpcDirectClientSettings.defaults(endpoint.host() + ":" + endpoint.port(), identity, tls,
                        Math.min(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()).toMillis(), core.getConsumer().getDefaultTimeoutMs()));
                var interceptor = IdpRpcClientCredentialInterceptorFactory.createConfigured(source.client, source.properties, validators.getValidator());
                JsonNode firstWebUser = null;
                for (String realm : List.of("default", "tenant-b")) {
                    String tenant = registry.path("tenantIds").path(realm).asText();
                    JsonNode created = null;
                    long requestedId = Long.MAX_VALUE;
                    if (createWebUsers) {
                        var reference = client.formal(realm, client.resourceForProcess("native-web"), "admin");
                        client.verifiedClaims(reference);
                        try (var transportHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()))
                                .followRedirects(HttpClient.Redirect.NEVER).build()) {
                            var http = new XingyuanResourceAuthorizationIT.NativeResourceHttpProbe("native-web", root, manifestFile, digest,
                                    transportHttp, Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()));
                            String marker = "xingyuan-it-" + manifest.path("runId").asText() + "-" + realm + "-" + java.util.UUID.randomUUID();
                            var body = mapper.createObjectNode().put("name", marker).put("email", marker + "@xingyuan.invalid");
                            created = http.request("POST", "/api/v1/users", reference, body, null, 201);
                            if (!body.path("name").asText().equals(created.path("name").asText()) || !body.path("email").asText().equals(created.path("email").asText())) {
                                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                            }
                            requestedId = XingyuanResourceAuthorizationIT.responseId(created);
                            assertThat(http.request("GET", "/api/v1/users/" + requestedId, reference, null, null, 200)).isEqualTo(created);
                        }
                    }
                    var message = rpcIdRequest(method, target.idField(), requestedId);
                    String registration = binding.getTenantRegistrationIds().get(tenant);
                    var request = new IdpServiceTokenRequest(registration, binding.getAppId(), binding.getResourceUri(), ServiceTokenContext.TENANT, tenant, binding.getScopes());
                    var token = source.client.authorize(request);
                    var tokenFacts = java.util.stream.StreamSupport.stream(registry.path("clients").spliterator(), false)
                            .filter(value -> registration.equals(value.path("registrationId").asText())).toList();
                    if (tokenFacts.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    verifyServiceClaims(source, ServiceProbeBO.builder().context(source).request(request).fact(tokenFacts.getFirst())
                            .key(keyReference(request, source.registrations.get(registration), source.properties)).build(), token);
                    var verified = source.verifier.authenticate(token.getTokenValue());
                    if (!tenant.equals(verified.getTenantId()) || !"SERVICE".equals(verified.getPrincipalType().name())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    var principal = org.mapstruct.factory.Mappers.getMapper(VerifiedPrincipalConverter.class).toServicePrincipal(verified.getJwt(), binding.getResourceUri());
                    try (var direct = new RpcDirectClientFactory().create(contract.contract.contractType(), settings, List.of(interceptor))) {
                        String businessMethod = target.providerRole().equals("native-web") ? "getUser" : "get";
                        var beforeBusiness = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, target.providerRole(), realm, "BUSINESS_COUNTS", null);
                        var priorPrincipal = IdpRpcSecurityContext.currentPrincipalOrNull();
                        var observed = observeRpcCall(() -> IdpRpcSecurityContext.with(principal, token.getTokenValue(), null, null)
                                .call(() -> (Message) method.javaMethod().invoke(direct.client(), message)), io.grpc.Status.Code.OK);
                        var response = observed.response();
                        assertThat(IdpRpcSecurityContext.currentPrincipalOrNull() == priorPrincipal).isTrue();
                        var successfulBusiness = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, target.providerRole(), realm, "BUSINESS_COUNTS", null);
                        requireOnlyBusinessMethodDelta(beforeBusiness, successfulBusiness, businessMethod, 1);
                        if (createWebUsers) {
                            requireUserResponse(response, created);
                            if (firstWebUser == null) { firstWebUser = created; }
                            else {
                                var foreign = rpcIdRequest(method, target.idField(), XingyuanResourceAuthorizationIT.responseId(firstWebUser));
                                var rejected = IdpRpcSecurityContext.with(principal, token.getTokenValue(), null, null)
                                        .call(() -> invokeGeneratedRpc(method, direct.client(), foreign));
                                requireNotFoundResponse(rejected, "ORG_NOT_FOUND");
                            }
                        } else { requireNotFoundResponse(response, target.notFoundCode()); }
                        var beforeAnonymous = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, target.providerRole(), realm, "BUSINESS_COUNTS", null);
                        RpcAuditObservationVO anonymousObservation;
                        try (var anonymous = new RpcDirectClientFactory().create(contract.contract.contractType(), settings, List.of())) {
                            anonymousObservation = observeRpcCall(() -> (Message) method.javaMethod().invoke(anonymous.client(), message), io.grpc.Status.Code.UNAUTHENTICATED).observation();
                        }
                        var afterAnonymous = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, target.providerRole(), realm, "BUSINESS_COUNTS", null);
                        requireOnlyBusinessMethodDelta(beforeAnonymous, afterAnonymous, businessMethod, 0);
                        var outcome = mapper.createObjectNode().put("consumerRole", target.consumerRole()).put("providerRole", target.providerRole()).put("tenantId", tenant)
                                .put("methodReference", UnifiedIdentityLiveClient.sha256(method.fullMethodName().getBytes(StandardCharsets.UTF_8)))
                                .put("descriptorReference", UnifiedIdentityLiveClient.sha256(method.protoMethod().getFile().toProto().toByteArray()))
                                .put("leaseReference", UnifiedIdentityLiveClient.sha256(endpoint.leaseId().getBytes(StandardCharsets.UTF_8)))
                                .put("leaseRevision", snapshot.revision()).put("leaseExpiresAt", endpoint.leaseExpireAt().toString())
                                .put("businessStatus", createWebUsers ? "USER_DATA_MATCHED" : target.notFoundCode()).put("missingTokenStatus", "UNAUTHENTICATED")
                                .put("responseReference", UnifiedIdentityLiveClient.sha256(response.toByteArray()));
                        outcome.set("beforeBusiness", beforeBusiness); outcome.set("successfulBusiness", successfulBusiness);
                        outcome.set("beforeMissingToken", beforeAnonymous); outcome.set("afterMissingToken", afterAnonymous);
                        outcome.set("successfulRequest", mapper.valueToTree(observed.observation())); outcome.set("missingTokenRequest", mapper.valueToTree(anonymousObservation));
                        if (!createWebUsers) {
                            outcome.set("serviceSecurity", actualServiceRpcDenials(root, evidence, manifestFile, manifest, digest, registry, target, contract.contract, settings, method, message, token, verified));
                        }
                        if (created != null) { outcome.put("httpReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(created))); }
                        proofs.add(outcome);
                    }
                }
            } finally { flat.clear(); }
        }
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("status", "PASSED").put("scope", createWebUsers ? "STANDALONE_SERVICE_TO_WEB_DATA" : "STANDALONE_NOT_FOUND_PROTOCOL").put("recordedAt", Instant.now().toString());
        proof.set("proofs", proofs);
        if (createWebUsers) { proof.put("crossTenantIsolationConfirmed", true); }
        client.publishNativeRpcProtocolProof(evidence, proof);
    }

    static void requireStandaloneRpcIdSeparation(Map<String, java.util.Properties> profiles) {
        if (profiles.isEmpty()) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        for (var profile : profiles.values()) {
            if ("false".equals(profile.getProperty("egon.cola.component.id.enabled", "true"))) { continue; }
            String value = profile.getProperty("egon.cola.component.id.machine-id");
            if (value == null || !value.matches("[0-9]{1,4}")) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
            int machineId = Integer.parseInt(value);
            if (machineId < 0 || machineId > 1023 || machineId == 711) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
    }

    private static void initializeStandaloneRpcInvocationIds(Path root, JsonNode manifest, String digest) throws Exception {
        var profiles = new LinkedHashMap<String, java.util.Properties>(); var nativeRoles = activeNativeRuntimeRoles(root, manifest, digest);
        try {
            for (var process : manifest.path("processes")) {
                String role = process.path("id").asText();
                if (!"JAVA".equals(process.path("kind").asText()) || role.equals("native-agent") && !nativeRoles.contains(role)) { continue; }
                profiles.put(role, XingyuanResourceAuthorizationIT.verifiedProfile(root, manifest, digest, role));
            }
            requireStandaloneRpcIdSeparation(profiles);
        } finally { profiles.values().forEach(java.util.Properties::clear); }
        // The existing short-lived owner fixtures reserve 711; runtime profiles must use their reviewed IDs.
        top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator.initialize(711L,
                top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator.DEFAULT_MAX_CLOCK_BACKWARD);
    }

    @Test
    void standaloneRpcIdsCannotCollideWithTheExistingOwnerFixtureOrUseAnUnconfiguredMachine() {
        var profile = new java.util.Properties(); profile.setProperty("egon.cola.component.id.machine-id", "12");
        assertThatCode(() -> requireStandaloneRpcIdSeparation(Map.of("native-web", profile))).doesNotThrowAnyException();
        for (String value : List.of("711", "0711", "1024", "-1", "${EGON_ID_MACHINE_ID}")) {
            profile.setProperty("egon.cola.component.id.machine-id", value);
            assertThatThrownBy(() -> requireStandaloneRpcIdSeparation(Map.of("native-web", profile))).isInstanceOf(IllegalArgumentException.class);
        }
        profile.clear();
        assertThatThrownBy(() -> requireStandaloneRpcIdSeparation(Map.of("native-web", profile))).hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
        profile.setProperty("egon.cola.component.id.enabled", "false");
        assertThatCode(() -> requireStandaloneRpcIdSeparation(Map.of("edge", profile))).doesNotThrowAnyException();
    }

    private static top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory actualBearerFactory(String bearer) {
        if (bearer == null || bearer.isBlank() || bearer.length() > 8192 || bearer.chars().anyMatch(Character::isISOControl)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return invocation -> {
            var headers = new io.grpc.Metadata(); headers.put(io.grpc.Metadata.Key.of("authorization", io.grpc.Metadata.ASCII_STRING_MARSHALLER), "Bearer " + bearer);
            return io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(headers);
        };
    }

    private static JsonNode actualServiceRpcDenials(Path root, Path evidence, Path manifestFile, JsonNode manifest, String digest, JsonNode registry,
            RpcProbeTargetVO target, RpcContractDescriptor contract, RpcDirectClientSettings settings, RpcMethodDescriptor read, Message message,
            OAuth2AccessToken token, top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO verified) throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); String realm = registry.path("tenantIds").path("default").asText().equals(verified.getTenantId()) ? "default" : "tenant-b";
        String writeName = target.providerRole().equals("native-service") ? "CreateCourse" : "CreateUser";
        var writes = contract.methods().stream().filter(value -> writeName.equals(value.methodName())).toList();
        if (writes.size() != 1 || !verified.getJwt().getExpiresAt().isAfter(Instant.now().plusSeconds(60))) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
        RpcMethodDescriptor write = writes.getFirst(); var empty = (Message) write.requestType().getMethod("getDefaultInstance").invoke(null);
        var source = actualBearerFactory(token.getTokenValue());
        var beforeScope = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, target.providerRole(), realm, "BUSINESS_COUNTS", null);
        RpcAuditObservationVO scopeObservation;
        try (var direct = new RpcDirectClientFactory().create(contract.contractType(), settings, List.of(source))) {
            scopeObservation = observeRpcCall(() -> (Message) write.javaMethod().invoke(direct.client(), empty), io.grpc.Status.Code.PERMISSION_DENIED).observation();
        }
        var afterScope = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, target.providerRole(), realm, "BUSINESS_COUNTS", null);
        String method = target.providerRole().equals("native-web") ? "getUser" : "get"; requireOnlyBusinessMethodDelta(beforeScope, afterScope, method, 0);
        var request = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest).put("status", "READY")
                .put("consumerRole", target.consumerRole()).put("providerRole", target.providerRole()).put("tenantCode", realm).put("tenantId", verified.getTenantId())
                .put("issuer", verified.getJwt().getIssuer().toString()).put("resourceUri", verified.getJwt().getAudience().getFirst()).put("clientId", verified.getJwt().getClaimAsString("client_id"))
                .put("clientPk", verified.getClientRegistrationId()).put("authorizationId", verified.getAuthorizationId())
                .put("authorizationReference", UnifiedIdentityLiveClient.sha256(verified.getAuthorizationId().getBytes(StandardCharsets.UTF_8))).put("tokenSha256", tokenReference(token))
                .put("methodReference", UnifiedIdentityLiveClient.sha256(read.fullMethodName().getBytes(StandardCharsets.UTF_8))).put("baselineRpcStatus", "OK")
                .put("expiresAt", verified.getJwt().getExpiresAt().toString()).put("recordedAt", Instant.now().toString());
        request.set("scopes", mapper.valueToTree(Set.of(verified.getJwt().getClaimAsString("scope").split(" "))));
        String hash = tokenReference(token); Path requestFile = evidence.resolve("service-revocation-request-" + hash + ".json"), resultFile = evidence.resolve("service-revocation-" + hash + ".json");
        publishNewNativeEvidence(root, requestFile, request);
        var owner = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "service-revoke", manifestFile.toString(), resultFile.toString(), hash, manifest.path("repoRoot").asText()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!owner.waitFor(manifest.path("deadlines").path("operationSeconds").intValue(), TimeUnit.SECONDS) || owner.exitValue() != 0) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        } finally { if (owner.isAlive()) { owner.destroy(); owner.waitFor(10, TimeUnit.SECONDS); } }
        byte[] confirmedBytes = UnifiedIdentityLiveClient.protectedBytes(resultFile, evidence, 128 * 1024); var confirmed = mapper.readTree(confirmedBytes);
        validateServiceRevocationConfirmation(request, confirmed, Instant.now());
        Path auditFile = evidence.resolve("service-revocation-audit-" + hash + ".json");
        byte[] auditBytes = UnifiedIdentityLiveClient.protectedBytes(auditFile, evidence, 128 * 1024); var audit = mapper.readTree(auditBytes);
        if (!audit.path("schemaVersion").isInt() || audit.path("schemaVersion").intValue() != 1 || !manifest.path("runId").asText().equals(audit.path("runId").asText())
                || !digest.equals(audit.path("manifestSha256").asText()) || !"PASSED".equals(audit.path("status").asText())
                || !"SERVICE_ACCESS_JTI_ORIGINAL_PG_REVOCATION_AUDIT_AND_APPLIED_DENY".equals(audit.path("scope").asText())
                || !hash.equals(audit.path("tokenReference").asText()) || !request.path("authorizationReference").asText().equals(audit.path("authorizationReference").asText())
                || !audit.path("denyApplied").isBoolean() || !audit.path("denyApplied").booleanValue()
                || !audit.path("revision").isIntegralNumber() || audit.path("revision").longValue() <= 0 || !audit.path("eventReference").asText().matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var beforeRevoked = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, target.providerRole(), realm, "BUSINESS_COUNTS", null);
        RpcAuditObservationVO revokedObservation;
        try (var direct = new RpcDirectClientFactory().create(contract.contractType(), settings, List.of(source))) {
            revokedObservation = observeRpcCall(() -> (Message) read.javaMethod().invoke(direct.client(), message), io.grpc.Status.Code.UNAUTHENTICATED).observation();
        }
        var afterRevoked = invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, target.providerRole(), realm, "BUSINESS_COUNTS", null);
        requireOnlyBusinessMethodDelta(beforeRevoked, afterRevoked, method, 0);
        if (!verified.getJwt().getExpiresAt().isAfter(Instant.now())) { throw new IllegalStateException("XINGYUAN_INPUT_EXPIRED"); }
        var proof = mapper.createObjectNode().put("scopeDeniedStatus", "PERMISSION_DENIED").put("revokedStatus", "UNAUTHENTICATED")
                .put("scopeMethodReference", UnifiedIdentityLiveClient.sha256(write.fullMethodName().getBytes(StandardCharsets.UTF_8))).put("tokenReference", hash)
                .put("authorizationReference", request.path("authorizationReference").asText()).put("ownerReceiptSha256", UnifiedIdentityLiveClient.sha256(confirmedBytes))
                .put("revocationAuditSha256", UnifiedIdentityLiveClient.sha256(auditBytes))
                .put("tokenExpiresAt", verified.getJwt().getExpiresAt().toString());
        proof.set("beforeScope", beforeScope); proof.set("afterScope", afterScope); proof.set("beforeRevoked", beforeRevoked); proof.set("afterRevoked", afterRevoked);
        proof.set("scopeDeniedRequest", mapper.valueToTree(scopeObservation)); proof.set("revokedRequest", mapper.valueToTree(revokedObservation));
        return proof;
    }

    record RpcAuditObservationVO(@NotBlank @Pattern(regexp = "[0-9a-f]{32}") String traceId, @NotNull io.grpc.Status.Code status,
                                 @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant startedAt,
                                 @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant completedAt) { }

    record RpcObservedResponseVO(Message response, RpcAuditObservationVO observation) { }

    static RpcObservedResponseVO observeRpcCall(java.util.concurrent.Callable<Message> call, io.grpc.Status.Code expected) throws Exception {
        if (!Set.of(io.grpc.Status.Code.OK, io.grpc.Status.Code.PERMISSION_DENIED, io.grpc.Status.Code.UNAUTHENTICATED).contains(expected)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var trace = top.egon.cola.component.common.trace.TraceContext.root(); Instant start = Instant.now(); Message response = null;
        try (var ignored = trace.open()) {
            try {
                response = call.call();
                if (expected != io.grpc.Status.Code.OK || response == null) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            } catch (java.lang.reflect.InvocationTargetException rejected) {
                if (expected == io.grpc.Status.Code.OK || io.grpc.Status.fromThrowable(rejected.getCause()).getCode() != expected) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
            }
        }
        return new RpcObservedResponseVO(response, new RpcAuditObservationVO(trace.traceId(), expected, start, Instant.now()));
    }

    @Test
    void rpcAuditObservationsUseActualTransportStatusAndRestoreThePreviousTraceEvenOnFailure() throws Exception {
        var parent = top.egon.cola.component.common.trace.TraceContext.root(); var seen = new java.util.ArrayList<String>();
        try (var ignored = parent.open()) {
            var success = observeRpcCall(() -> { seen.add(top.egon.cola.component.common.trace.TraceContext.getTraceId()); return com.google.protobuf.Empty.getDefaultInstance(); }, io.grpc.Status.Code.OK);
            var denied = observeRpcCall(() -> { seen.add(top.egon.cola.component.common.trace.TraceContext.getTraceId()); throw new java.lang.reflect.InvocationTargetException(io.grpc.Status.PERMISSION_DENIED.asRuntimeException()); }, io.grpc.Status.Code.PERMISSION_DENIED);
            assertThat(success.observation().traceId()).isEqualTo(seen.getFirst()).isNotEqualTo(parent.traceId());
            assertThat(denied.observation().traceId()).isEqualTo(seen.getLast()).isNotEqualTo(success.observation().traceId());
            assertThat(denied.response()).isNull(); assertThat(denied.observation().status()).isEqualTo(io.grpc.Status.Code.PERMISSION_DENIED);
            assertThat(top.egon.cola.component.common.trace.TraceContext.getTraceId()).isEqualTo(parent.traceId());
            assertThatThrownBy(() -> observeRpcCall(() -> { throw new java.lang.reflect.InvocationTargetException(io.grpc.Status.UNAVAILABLE.asRuntimeException()); }, io.grpc.Status.Code.UNAUTHENTICATED))
                    .hasMessage("XINGYUAN_VERIFICATION_FAILED");
            assertThatThrownBy(() -> observeRpcCall(() -> com.google.protobuf.Empty.getDefaultInstance(), io.grpc.Status.Code.PERMISSION_DENIED)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
            assertThatThrownBy(() -> observeRpcCall(() -> { throw new java.io.IOException("controlled transport failure"); }, io.grpc.Status.Code.OK)).isInstanceOf(java.io.IOException.class);
            assertThat(top.egon.cola.component.common.trace.TraceContext.getTraceId()).isEqualTo(parent.traceId());
        }
    }

    static void validateServiceRevocationConfirmation(JsonNode request, JsonNode confirmed, Instant now) {
        var expected = new HashSet<String>(); request.fieldNames().forEachRemaining(expected::add); expected.add("denyApplied");
        var actual = new HashSet<String>(); confirmed.fieldNames().forEachRemaining(actual::add);
        if (!confirmed.isObject() || !actual.equals(expected) || !"CONFIRMED".equals(confirmed.path("status").asText()) || !confirmed.path("denyApplied").isBoolean()
                || !confirmed.path("denyApplied").booleanValue()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        request.fields().forEachRemaining(entry -> {
            if (!Set.of("status", "recordedAt").contains(entry.getKey()) && !entry.getValue().equals(confirmed.path(entry.getKey()))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        });
        Instant recorded = XingyuanResourceAuthorizationIT.transactionProofInstant(confirmed, "recordedAt");
        if (recorded.isBefore(XingyuanResourceAuthorizationIT.transactionProofInstant(request, "recordedAt")) || recorded.isAfter(now)
                || !XingyuanResourceAuthorizationIT.transactionProofInstant(request, "expiresAt").isAfter(now)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    @Test
    void serviceRevocationConfirmationCannotSwapIdentityOrPretendAQueuedDenyIsApplied() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var request = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "run").put("manifestSha256", "a".repeat(64)).put("status", "READY")
                .put("consumerRole", "native-web").put("providerRole", "native-service").put("tokenSha256", "b".repeat(64)).put("clientPk", "51").put("authorizationId", "71")
                .put("recordedAt", now.toString()).put("expiresAt", now.plusSeconds(60).toString());
        var confirmed = request.deepCopy().put("status", "CONFIRMED").put("denyApplied", true).put("recordedAt", now.plusSeconds(1).toString());
        assertThatCode(() -> validateServiceRevocationConfirmation(request, confirmed, now.plusSeconds(2))).doesNotThrowAnyException();
        for (String fault : List.of("pending", "queued", "token", "client", "family", "role", "past", "secret")) {
            var invalid = confirmed.deepCopy();
            if (fault.equals("pending")) { invalid.put("status", "RUNNING"); }
            else if (fault.equals("queued")) { invalid.put("denyApplied", false); }
            else if (fault.equals("token")) { invalid.put("tokenSha256", "c".repeat(64)); }
            else if (fault.equals("client")) { invalid.put("clientPk", "52"); }
            else if (fault.equals("family")) { invalid.put("authorizationId", "72"); }
            else if (fault.equals("role")) { invalid.put("providerRole", "native-web"); }
            else if (fault.equals("past")) { invalid.put("recordedAt", now.minusSeconds(1).toString()); }
            else { invalid.put("accessToken", "forbidden"); }
            assertThatThrownBy(() -> validateServiceRevocationConfirmation(request, invalid, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    static DdcServiceInstance requirePeerLease(DdcServiceSnapshot snapshot, DdcServiceKey expected, int port, String resourceId, Instant now) {
        if (snapshot == null || !expected.equals(snapshot.serviceKey()) || snapshot.instances().size() != 1 || snapshot.revision() < 1) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var instance = snapshot.instances().getFirst();
        if (!expected.equals(instance.serviceKey()) || !"127.0.0.1".equals(instance.host()) || instance.port() != port || !instance.secure()
                || !DdcInstanceStatus.fromWire(instance.status()).isAvailable(now, instance.leaseExpireAt())
                || !resourceId.equals(instance.resourceServerId()) || instance.resourceVersion() == null || instance.resourceVersion() < 0
                || instance.credentialId() == null || instance.credentialId().isBlank()) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return instance;
    }

    static void requireTaskRpcTls(Path root, JsonNode manifest, RpcTransportSecurity tls) throws Exception {
        var secrets = manifest.path("secretFiles");
        if (!tls.enabled() || tls.developmentPlaintext() || !secrets.path("tls-server-cert").asText().equals(tls.certificateChainPath())
                || !secrets.path("tls-server-key").asText().equals(tls.privateKeyPath())
                || !secrets.path("tls-ca-cert").asText().equals(tls.trustCertificateCollectionPath())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        for (String file : List.of(tls.certificateChainPath(), tls.privateKeyPath(), tls.trustCertificateCollectionPath())) {
            UnifiedIdentityLiveClient.protectedBytes(Path.of(file), root.resolve("secrets"), 64 * 1024);
        }
    }

    static Message rpcIdRequest(RpcMethodDescriptor method, String fieldName, long id) throws Exception {
        if (id <= 0) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var prototype = (Message) method.requestType().getMethod("getDefaultInstance").invoke(null);
        var field = prototype.getDescriptorForType().findFieldByName(fieldName);
        if (field == null || field.getJavaType() != com.google.protobuf.Descriptors.FieldDescriptor.JavaType.LONG) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return prototype.newBuilderForType().setField(field, id).build();
    }

    static void requireUserResponse(Message response, JsonNode expected) {
        var type = response.getDescriptorForType();
        var success = type.findFieldByName("success"); var data = type.findFieldByName("data");
        if (success == null || data == null || !Boolean.TRUE.equals(response.getField(success)) || !response.hasField(data)) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var user = (Message) response.getField(data);
        for (String name : List.of("id", "name", "email", "status")) {
            var field = user.getDescriptorForType().findFieldByName(name);
            if (field == null || !expected.path(name).asText().equals(String.valueOf(user.getField(field)))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
    }

    @Test
    void rpcUserDataMustMatchTheActualHttpRowRatherThanAnotherTenantRow() throws Exception {
        var userType = com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder().setName("User");
        int number = 1;
        for (String name : List.of("id", "name", "email", "status")) {
            userType.addField(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.newBuilder().setName(name).setNumber(number++)
                    .setType(name.equals("id") ? com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64
                            : com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING));
        }
        var responseType = com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder().setName("Response")
                .addField(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.newBuilder().setName("success").setNumber(1)
                        .setType(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL))
                .addField(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.newBuilder().setName("data").setNumber(2)
                        .setType(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName(".controlled.User"));
        var file = com.google.protobuf.Descriptors.FileDescriptor.buildFrom(com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
                .setName("controlled-user-data.proto").setPackage("controlled").setSyntax("proto3").addMessageType(userType).addMessageType(responseType).build(),
                new com.google.protobuf.Descriptors.FileDescriptor[0]);
        var user = file.findMessageTypeByName("User"); var response = file.findMessageTypeByName("Response");
        var actualUser = com.google.protobuf.DynamicMessage.newBuilder(user).setField(user.findFieldByName("id"), 11L)
                .setField(user.findFieldByName("name"), "owned").setField(user.findFieldByName("email"), "owned@xingyuan.invalid")
                .setField(user.findFieldByName("status"), "ACTIVE").build();
        var actual = com.google.protobuf.DynamicMessage.newBuilder(response).setField(response.findFieldByName("success"), true)
                .setField(response.findFieldByName("data"), actualUser).build();
        var expected = UnifiedIdentityLiveClient.strictJson().createObjectNode().put("id", "11").put("name", "owned")
                .put("email", "owned@xingyuan.invalid").put("status", "ACTIVE");
        assertThatCode(() -> requireUserResponse(actual, expected)).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireUserResponse(actual, expected.deepCopy().put("id", "12"))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> requireUserResponse(actual, expected.deepCopy().put("email", "other@xingyuan.invalid"))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    static Message invokeGeneratedRpc(RpcMethodDescriptor method, Object consumer, Message request) throws Exception {
        try { return (Message) method.javaMethod().invoke(consumer, request); }
        catch (java.lang.reflect.InvocationTargetException failed) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED:RPC:" + io.grpc.Status.fromThrowable(failed.getCause()).getCode());
        }
    }

    static void requireRpcDenied(RpcMethodDescriptor method, Object consumer, Message request, io.grpc.Status.Code expected) throws Exception {
        try { method.javaMethod().invoke(consumer, request); }
        catch (java.lang.reflect.InvocationTargetException failed) {
            assertThat(io.grpc.Status.fromThrowable(failed.getCause()).getCode()).isEqualTo(expected);
            return;
        }
        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
    }

    static void requireNotFoundResponse(Message response, String expected) {
        var descriptor = response.getDescriptorForType();
        var success = descriptor.findFieldByName("success"); var code = descriptor.findFieldByName("code");
        if (success == null || code == null || !Boolean.FALSE.equals(response.getField(success)) || !expected.equals(response.getField(code))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var data = descriptor.findFieldByName("data");
        if (data != null && response.hasField(data)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    static GeneratedContractHandle generatedContract(Path root, JsonNode manifest, String role, String namespace, String suffix, jakarta.validation.Validator validator) throws Exception {
        Path artifact = Path.of(manifest.path("artifacts").path(role).path("path").asText());
        if (!Set.of("native-web", "native-service").contains(role) || artifact.startsWith(Path.of(manifest.path("repoRoot").asText()).resolve("egon-cola-archetypes/source-projects"))
                || !fileReference(artifact).equals(manifest.path("artifacts").path(role).path("sha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        byte[] facade;
        try (var archive = new java.util.zip.ZipFile(artifact.toFile())) {
            String entry = nativeFacadeEntry(archive.stream().map(java.util.zip.ZipEntry::getName).toList(), namespace);
            try (var input = archive.getInputStream(archive.getEntry(entry))) {
                facade = input.readNBytes(16 * 1024 * 1024 + 1);
                if (facade.length > 16 * 1024 * 1024) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
            }
        }
        String nestedHash = UnifiedIdentityLiveClient.sha256(facade);
        Path directory = root.resolve("artifacts/rpc-facades");
        Files.createDirectories(directory, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
        for (Path current = directory; current != null && current.startsWith(root); current = current.getParent()) {
            if (Files.isSymbolicLink(current) || !Files.isDirectory(current) || !Files.getOwner(root).equals(Files.getOwner(current))
                    || !Files.getPosixFilePermissions(current).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        Path file = directory.resolve(role + "-" + nestedHash + ".jar");
        if (Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            if (!nestedHash.equals(UnifiedIdentityLiveClient.sha256(UnifiedIdentityLiveClient.protectedBytes(file, root.resolve("artifacts"), 16 * 1024 * 1024)))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        } else {
            try (var output = java.nio.channels.FileChannel.open(file, Set.of(java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.CREATE_NEW),
                    java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")))) {
                var content = java.nio.ByteBuffer.wrap(facade); while (content.hasRemaining()) { output.write(content); } output.force(true);
            }
        }
        String className;
        try (var nested = new java.util.zip.ZipFile(file.toFile())) {
            var matches = nested.stream().map(java.util.zip.ZipEntry::getName).filter(value -> value.endsWith(suffix)).toList();
            if (matches.size() != 1 || matches.getFirst().startsWith("top/egon/cola/archetype/source/")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            className = matches.getFirst().substring(0, matches.getFirst().length() - 6).replace('/', '.');
        }
        var loader = new java.net.URLClassLoader(new java.net.URL[]{file.toUri().toURL()}, XingyuanNativeAuthenticationIT.class.getClassLoader());
        try {
            Class<?> type = Class.forName(className, false, loader);
            if (type.getClassLoader() != loader) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var descriptor = new RpcContractValidator(new top.egon.cola.component.common.core.validation.ValidationUtils(validator)).validate(type);
            if (!fileReference(artifact).equals(manifest.path("artifacts").path(role).path("sha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            return new GeneratedContractHandle(loader, descriptor);
        } catch (Exception failure) {
            try { loader.close(); } catch (java.io.IOException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    static String fileReference(Path path) throws Exception {
        if (!path.isAbsolute() || !path.equals(path.normalize()) || Files.isSymbolicLink(path) || !Files.isRegularFile(path)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        try (var input = new java.security.DigestInputStream(Files.newInputStream(path, java.nio.file.LinkOption.NOFOLLOW_LINKS), digest)) { input.transferTo(java.io.OutputStream.nullOutputStream()); }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    @RequiredArgsConstructor
    private static final class GeneratedContractHandle implements AutoCloseable {
        private final java.net.URLClassLoader loader;
        private final RpcContractDescriptor contract;
        @Override public void close() throws java.io.IOException { loader.close(); }
    }

    static void requireServiceRegistration(ClientRegistration registration, JsonNode fact, IdpStarterProperties properties) {
        if (registration == null || fact == null || properties == null) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(registration.getAuthorizationGrantType())
                || !ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(registration.getClientAuthenticationMethod())
                || registration.getClientSecret() == null || registration.getClientSecret().isBlank()
                || !"SERVICE".equals(fact.path("purpose").asText()) || !"CONFIDENTIAL".equals(fact.path("clientType").asText())
                || !"ACTIVE".equals(fact.path("status").asText())
                || !registration.getRegistrationId().equals(fact.path("registrationId").asText())
                || !registration.getClientId().equals(fact.path("clientId").asText())
                || !java.util.Objects.equals(properties.getServiceClient().getAppId(), fact.path("appId").asText())
                || !fact.path("issuer").asText().equals(registration.getProviderDetails().getIssuerUri())
                || !(fact.path("issuer").asText() + "/oauth2/token").equals(registration.getProviderDetails().getTokenUri())
                || !fact.path("issuer").asText().matches("https://127\\.0\\.0\\.1:28443/(platform|tenants/[1-9][0-9]{0,18})")
                || !fact.path("refreshTokenTtlSeconds").isIntegralNumber() || !fact.path("refreshTokenTtlSeconds").canConvertToInt()
                || fact.path("refreshTokenTtlSeconds").intValue() != 0
                || !fact.path("accessTokenTtlSeconds").isIntegralNumber() || !fact.path("accessTokenTtlSeconds").canConvertToInt()
                || fact.path("accessTokenTtlSeconds").intValue() < 300 || fact.path("accessTokenTtlSeconds").intValue() > 600
                || properties.getServiceClient().getRenewalSkew() == null || properties.getServiceClient().getRenewalSkew().isNegative()
                || properties.getServiceClient().getRenewalSkew().compareTo(Duration.ofSeconds(fact.path("accessTokenTtlSeconds").intValue())) >= 0
                || !fact.path("credentialVersion").isIntegralNumber() || !fact.path("credentialVersion").canConvertToLong()
                || fact.path("credentialVersion").longValue() != properties.getServiceClient().credentialVersion(registration.getRegistrationId())
                || !fact.path("clientPk").asText().matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var allowed = scopeSet(fact.path("allowedScopes"));
        if (registration.getScopes().isEmpty() || !allowed.containsAll(registration.getScopes()) || allowed.contains("offline_access")
                || !fact.path("resourceUris").isArray() || fact.path("resourceUris").isEmpty() || fact.path("resourceUris").size() > 64) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        try { if (Long.parseLong(fact.path("clientPk").asText()) <= 0) { throw new NumberFormatException(); } }
        catch (NumberFormatException rejected) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
    }

    static Set<String> scopeSet(JsonNode values) {
        if (!values.isArray() || values.isEmpty() || values.size() > 64) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var result = new HashSet<String>();
        for (var value : values) {
            if (!value.isTextual() || !value.textValue().matches("[A-Za-z0-9._:-]{1,128}") || !result.add(value.textValue())) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        return Set.copyOf(result);
    }

    record ServiceCacheProofVO(@NotBlank String role, @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String authorizationKeyReference,
                               @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String initialTokenReference,
                               @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String renewedTokenReference,
                               @Min(1) @Max(1) int initialTokenResponses, @Min(2) @Max(2) int totalTokenResponses,
                               @Min(8) @Max(8) int concurrentCallers, @Min(8) @Max(8) int renewalCacheMisses, @AssertTrue boolean refreshTokenAbsent,
                               @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant initialExpiresAt,
                               @NotNull @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant renewedAt) { }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void configuredNativeServiceClientsRenewThroughTheRealAsWithSingleFlight() throws Exception {
        long deadline = System.nanoTime();
        var client = UnifiedIdentityLiveClient.isolated();
        var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024);
        var manifest = mapper.readTree(bytes);
        String digest = UnifiedIdentityLiveClient.sha256(bytes);
        deadline += Duration.ofSeconds(manifest.path("deadlines").path("operationSeconds").intValue()).toNanos();
        Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        XingyuanResourceAuthorizationIT.requireEvidenceDirectory(root, evidence);
        if (Files.exists(evidence.resolve("native-service-cache.json"))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
        var registry = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024));
        JsonNode recipe = reviewedIdentityRecipe(root, manifest);
        var contexts = new ArrayList<NativeServiceContext>();
        var probes = new ArrayList<ServiceProbeBO>();
        var futures = new ArrayList<Future<?>>();
        ExecutorService warmers = Executors.newFixedThreadPool(8), calls = Executors.newVirtualThreadPerTaskExecutor();
        try {
            var owners = new LinkedHashMap<String, String>();
            for (String role : activeNativeRuntimeRoles(root, manifest, digest)) {
                var context = serviceContext(root, manifest, digest, registry, role);
                contexts.add(context);
                probes.addAll(serviceProbes(context, registry, recipe, owners));
            }
            if (probes.isEmpty() || probes.size() > 192) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
            var warmed = new ArrayList<Future<OAuth2AccessToken>>();
            for (var probe : probes) { var future = warmers.submit(() -> probe.getContext().client.authorize(probe.getRequest())); warmed.add(future); futures.add(future); }
            var initial = new ArrayList<OAuth2AccessToken>();
            for (var future : warmed) { initial.add(await(future, deadline)); }
            for (int index = 0; index < probes.size(); index++) { verifyServiceClaims(probes.get(index).getContext(), probes.get(index), initial.get(index)); }
            client.publishNativeServiceCacheReady(evidence, mapper.createObjectNode().put("schemaVersion", 1)
                    .put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                    .put("status", "READY").put("phase", "INITIAL_VERIFIED").put("recordedAt", Instant.now().toString())
                    .put("requestCount", probes.size()).put("profileCount", contexts.size()));
            var outcomes = new ArrayList<Future<ServiceCacheProofVO>>();
            for (int index = 0; index < probes.size(); index++) {
                var probe = probes.get(index); var token = initial.get(index); long bound = deadline;
                var future = calls.submit(() -> proveCacheRenewal(probe, token, calls, bound));
                outcomes.add(future); futures.add(future);
            }
            var proofs = new ArrayList<ServiceCacheProofVO>();
            for (var outcome : outcomes) { proofs.add(await(outcome, deadline)); }
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                var validation = new top.egon.cola.component.common.core.validation.ValidationUtils(factory.getValidator());
                proofs.forEach(validation::validate);
            }
            if (!digest.equals(UnifiedIdentityLiveClient.sha256(UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024)))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText())
                    .put("manifestSha256", digest).put("status", "PASSED").put("clock", "SYSTEM_UTC").put("completedAt", Instant.now().toString());
            proof.set("proofs", mapper.valueToTree(proofs));
            if (!activeNativeRuntimeRoles(root, manifest, digest).contains("native-agent")) {
                proof.putArray("excludedRuntimeRoles").add("native-agent");
            }
            client.publishNativeServiceCacheProof(evidence, proof);
        } finally {
            futures.forEach(future -> future.cancel(true));
            contexts.forEach(NativeServiceContext::close);
            warmers.shutdownNow(); calls.shutdownNow();
            if (!warmers.awaitTermination(10, TimeUnit.SECONDS) || !calls.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("XINGYUAN_PARTIAL");
            }
        }
    }

    @lombok.Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor @lombok.experimental.Accessors(chain = true) @lombok.Builder
    static class ServiceProbeBO {
        @NotNull @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude private NativeServiceContext context;
        @NotNull private IdpServiceTokenRequest request;
        @NotNull @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude private JsonNode fact;
        @NotBlank @Pattern(regexp = "[0-9a-f]{64}") private String key;
    }

    static ServiceCacheProofVO proveCacheRenewal(ServiceProbeBO probe, OAuth2AccessToken initial, ExecutorService calls, long deadline) throws Exception {
        var context = probe.getContext();
        verifyServiceClaims(context, probe, initial);
        String initialHash = tokenReference(initial);
        assertThat(tokenReference(context.client.authorize(probe.getRequest()))).isEqualTo(initialHash);
        assertThat(context.responses.get(probe.getKey()).get()).isEqualTo(1);
        Instant renewal = initial.getExpiresAt().minus(context.properties.getServiceClient().getRenewalSkew());
        var keepAlive = calls.submit(() -> {
            while (Instant.now().isBefore(renewal.minusSeconds(1))) {
                if (System.nanoTime() >= deadline) { throw new IllegalStateException("XINGYUAN_PARTIAL"); }
                context.client.authorize(probe.getRequest());
                Thread.sleep(1000);
            }
            return null;
        });
        var ready = new CountDownLatch(8); var start = new CountDownLatch(1);
        var burst = new ArrayList<Future<OAuth2AccessToken>>();
        try {
            while (Instant.now().isBefore(renewal)) {
                if (System.nanoTime() >= deadline) { throw new IllegalStateException("XINGYUAN_PARTIAL"); }
                Thread.sleep(Math.min(200, Math.max(1, Duration.between(Instant.now(), renewal).toMillis())));
            }
            for (int index = 0; index < 8; index++) {
                burst.add(calls.submit(() -> { ready.countDown(); if (!start.await(remaining(deadline), TimeUnit.NANOSECONDS)) { throw new IllegalStateException("XINGYUAN_PARTIAL"); }
                    String previous = context.burstKeys.get();
                    try { context.burstKeys.set(probe.getKey()); return context.client.authorize(probe.getRequest()); }
                    finally { if (previous == null) { context.burstKeys.remove(); } else { context.burstKeys.set(previous); } }
                }));
            }
            if (!ready.await(remaining(deadline), TimeUnit.NANOSECONDS)) { throw new IllegalStateException("XINGYUAN_PARTIAL"); }
            start.countDown();
            String renewedHash = null;
            for (var future : burst) {
                var token = await(future, deadline);
                verifyServiceClaims(context, probe, token);
                String hash = tokenReference(token);
                if (renewedHash == null) { renewedHash = hash; }
                assertThat(hash).isEqualTo(renewedHash);
            }
            assertThat(renewedHash).isNotEqualTo(initialHash);
            await(keepAlive, deadline);
            assertThat(context.responses.get(probe.getKey()).get()).isEqualTo(2);
            assertThat(context.misses.get(probe.getKey()).getCount()).isZero();
            return new ServiceCacheProofVO(context.role, probe.getKey(), initialHash, renewedHash, 1, 2, 8, 8, true, initial.getExpiresAt(), Instant.now());
        } finally { start.countDown(); keepAlive.cancel(true); burst.forEach(future -> future.cancel(true)); }
    }

    static long remaining(long deadline) {
        long result = deadline - System.nanoTime();
        if (result <= 0) { throw new IllegalStateException("XINGYUAN_PARTIAL"); }
        return result;
    }

    static <T> T await(Future<T> future, long deadline) throws Exception {
        try { return future.get(remaining(deadline), TimeUnit.NANOSECONDS); }
        catch (java.util.concurrent.TimeoutException timeout) { throw new IllegalStateException("XINGYUAN_PARTIAL:service-cache-timeout"); }
        catch (java.util.concurrent.ExecutionException failure) {
            // Protocol-decoder causes can contain response material; retain only the safe failure category.
            String category = failure.getCause() == null ? "ExecutionException" : failure.getCause().getClass().getSimpleName();
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED:service-cache:" + category);
        }
    }
    static String tokenReference(OAuth2AccessToken token) {
        byte[] bytes = token.getTokenValue().getBytes(StandardCharsets.UTF_8);
        try { return UnifiedIdentityLiveClient.sha256(bytes); } finally { java.util.Arrays.fill(bytes, (byte) 0); }
    }

    static JsonNode reviewedIdentityRecipe(Path root, JsonNode manifest) throws Exception {
        var files = java.util.stream.StreamSupport.stream(manifest.path("policyFiles").spliterator(), false)
                .filter(value -> Path.of(value.path("path").asText()).getFileName().toString().equals("identity-provisioning.json")).toList();
        if (files.size() != 1) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(Path.of(files.getFirst().path("path").asText()), root, 2 * 1024 * 1024);
        if (!UnifiedIdentityLiveClient.sha256(bytes).equals(files.getFirst().path("sha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var recipe = UnifiedIdentityLiveClient.strictJson().readTree(bytes);
        if (!manifest.path("runId").asText().equals(recipe.path("runId").asText()) || !recipe.path("grants").isArray() || recipe.path("grants").size() > 192) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return recipe;
    }

    static List<ServiceProbeBO> serviceProbes(NativeServiceContext context, JsonNode registry, JsonNode recipe, Map<String, String> owners) {
        var probes = new ArrayList<ServiceProbeBO>(); var keys = new HashSet<String>();
        for (var registration : context.registrations.values()) {
            if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(registration.getAuthorizationGrantType())) { continue; }
            var facts = java.util.stream.StreamSupport.stream(registry.path("clients").spliterator(), false)
                    .filter(value -> registration.getRegistrationId().equals(value.path("registrationId").asText())).toList();
            if (facts.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var fact = facts.getFirst(); requireServiceRegistration(registration, fact, context.properties);
            if (!fact.path("issuer").asText().equals(registry.path("realms").path(fact.path("realmCode").asText()).path("issuer").asText())) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            String ownerKey = fact.path("realmCode").asText() + ":" + registration.getClientId();
            String previous = owners.putIfAbsent(ownerKey, context.role);
            if (previous != null && !previous.equals(context.role)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var grants = java.util.stream.StreamSupport.stream(recipe.path("grants").spliterator(), false)
                    .filter(value -> fact.path("realmCode").asText().equals(value.path("realmCode").asText())
                            && registration.getClientId().equals(value.path("clientId").asText()) && "CLIENT_CREDENTIALS".equals(value.path("input").path("grantType").asText())).toList();
            if (grants.isEmpty()) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
            for (var grant : grants) {
                var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                        .filter(value -> grant.path("resourceServerId").asText().equals(value.path("resourceServerId").asText()) && "ACTIVE".equals(value.path("status").asText())).toList();
                if (resources.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                String uri = resources.getFirst().path("resourceUri").asText();
                if (java.util.stream.StreamSupport.stream(fact.path("resourceUris").spliterator(), false).noneMatch(value -> uri.equals(value.asText()))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var scopes = new HashSet<>(scopeSet(grant.path("input").path("allowedScopes"))); scopes.retainAll(registration.getScopes());
                if (scopes.isEmpty()) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
                String realm = fact.path("realmCode").asText();
                ServiceTokenContext kind = realm.equals("platform") ? ServiceTokenContext.PLATFORM : ServiceTokenContext.TENANT;
                String tenant = kind == ServiceTokenContext.PLATFORM ? null : registry.path("tenantIds").path(realm).asText();
                if (kind == ServiceTokenContext.TENANT && !Set.of("default", "tenant-b").contains(realm)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var request = new IdpServiceTokenRequest(registration.getRegistrationId(), context.properties.getServiceClient().getAppId(), URI.create(uri), kind, tenant, scopes);
                String key = keyReference(request, registration, context.properties);
                if (!keys.add(key)) { throw new IllegalArgumentException("XINGYUAN_STATE_CONFLICT"); }
                context.misses.put(key, new CountDownLatch(8));
                probes.add(ServiceProbeBO.builder().context(context).request(request).fact(fact).key(key).build());
            }
        }
        if (probes.isEmpty()) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validation = new top.egon.cola.component.common.core.validation.ValidationUtils(factory.getValidator());
            probes.forEach(validation::validate);
        }
        return probes;
    }

    static String keyReference(IdpServiceTokenRequest request, ClientRegistration registration, IdpStarterProperties properties) {
        var key = ServiceAuthorizationKey.from(request, registration.getProviderDetails().getIssuerUri(), registration.getClientId(),
                properties.getServiceClient().credentialVersion(registration.getRegistrationId()));
        return UnifiedIdentityLiveClient.sha256(key.principalName().getBytes(StandardCharsets.UTF_8));
    }

    static NativeServiceContext serviceContext(Path root, JsonNode manifest, String digest, JsonNode registry, String role) throws Exception {
        var profile = XingyuanResourceAuthorizationIT.verifiedProfile(root, manifest, digest, role);
        var flat = new LinkedHashMap<String, Object>(); profile.forEach((key, value) -> flat.put(key.toString(), value)); profile.clear();
        var binder = new Binder(new MapConfigurationPropertySource(flat));
        var properties = binder.bind("egon.cola.platform.tianquan.shoubing", Bindable.of(IdpStarterProperties.class)).orElseThrow(() -> new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"));
        var oauth = binder.bind("spring.security.oauth2.client", Bindable.of(OAuth2ClientProperties.class)).orElseThrow(() -> new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"));
        for (var provider : oauth.getProvider().values()) {
            if (provider.getIssuerUri() == null || !provider.getIssuerUri().matches("https://127\\.0\\.0\\.1:28443/(platform|tenants/[1-9][0-9]{0,18})")
                    || !(provider.getIssuerUri() + "/oauth2/token").equals(provider.getTokenUri())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        var registrations = new OAuth2ClientPropertiesMapper(oauth).asClientRegistrations();
        flat.clear();
        for (var registration : registrations.values()) {
            if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(registration.getAuthorizationGrantType())) { continue; }
            var facts = java.util.stream.StreamSupport.stream(registry.path("clients").spliterator(), false)
                    .filter(value -> registration.getRegistrationId().equals(value.path("registrationId").asText())).toList();
            if (facts.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            requireServiceRegistration(registration, facts.getFirst(), properties);
            byte[] secret = UnifiedIdentityLiveClient.protectedBytes(Path.of(facts.getFirst().path("credentialFile").asText()), root.resolve("secrets"), 4096);
            try {
                String expected = new String(secret, StandardCharsets.UTF_8);
                if (expected.endsWith("\r\n")) { expected = expected.substring(0, expected.length() - 2); }
                else if (expected.endsWith("\n")) { expected = expected.substring(0, expected.length() - 1); }
                if (!registration.getClientSecret().equals(expected)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            } finally { java.util.Arrays.fill(secret, (byte) 0); }
        }
        var repository = new InMemoryClientRegistrationRepository(new ArrayList<>(registrations.values()));
        var store = new InMemoryOAuth2AuthorizedClientService(repository);
        var converter = new IdpClientCredentialsRequestEntityConverter();
        var responses = new java.util.concurrent.ConcurrentHashMap<String, AtomicInteger>();
        var misses = new java.util.concurrent.ConcurrentHashMap<String, CountDownLatch>();
        var burstKeys = new ThreadLocal<String>();
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue())).followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http); factory.setReadTimeout(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()));
        var responseClient = new RestClientClientCredentialsTokenResponseClient();
        responseClient.setHeadersConverter(converter::convertHeaders); responseClient.setParametersConverter(converter::convertParameters);
        responseClient.setRestClient(RestClient.builder().requestFactory(factory).messageConverters(values -> {
            values.clear(); values.add(new FormHttpMessageConverter()); values.add(new OAuth2AccessTokenResponseHttpMessageConverter());
        }).defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build());
        var provider = new ClientCredentialsOAuth2AuthorizedClientProvider(); provider.setClockSkew(properties.getServiceClient().getRenewalSkew()); provider.setClock(Clock.systemUTC());
        provider.setAccessTokenResponseClient(grant -> {
            var parameters = converter.convertParameters(grant);
            String tenant = parameters.getFirst("tenant_id");
            var request = new IdpServiceTokenRequest(grant.getClientRegistration().getRegistrationId(), properties.getServiceClient().getAppId(),
                    URI.create(parameters.getFirst("resource")), tenant == null ? ServiceTokenContext.PLATFORM : ServiceTokenContext.TENANT, tenant,
                    Set.of(parameters.getFirst("scope").split(" ")));
            var response = responseClient.getTokenResponse(grant);
            requireNoRefreshToken(response);
            String key = keyReference(request, grant.getClientRegistration(), properties);
            int count = responses.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
            // Hold only the second real response until all concurrent callers have observed a cache miss.
            if (count == 2 && misses.containsKey(key)) {
                try {
                    var barrier = misses.get(key);
                    if (barrier == null || !barrier.await(manifest.path("deadlines").path("httpSeconds").intValue(), TimeUnit.SECONDS)) {
                        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                    }
                } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("XINGYUAN_PARTIAL", interrupted); }
            }
            return response;
        });
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repository, store); manager.setAuthorizedClientProvider(provider);
        var trusted = new TrustedIssuerRegistry(properties);
        try {
            if (trusted.isEmpty()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var verifier = new TenantIssuerAuthenticationManagerComponent(trusted, properties, Clock.systemUTC()); verifier.initialize();
            var facade = IdpServiceOAuth2Client.createConfigured(manager, converter, Clock.systemUTC(), properties.getServiceClient().getRenewalSkew(), repository, store, trusted, properties, observation -> {
                String key = burstKeys.get();
                if (key != null && "token.cache.access".equals(observation.getSignal()) && !"HIT".equals(observation.getResult())) {
                    var barrier = misses.get(key); if (barrier != null) { barrier.countDown(); }
                }
            });
            return new NativeServiceContext(role, properties, registrations, responses, misses, burstKeys, facade, verifier, http);
        } catch (RuntimeException rejected) { http.shutdownNow(); throw rejected; }
    }

    static void requireNoRefreshToken(org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse response) {
        if (response == null || response.getRefreshToken() != null) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    static void verifyServiceClaims(NativeServiceContext context, ServiceProbeBO probe, OAuth2AccessToken token) {
        var jwt = context.verifier.authenticate(token.getTokenValue()).getJwt();
        var request = probe.getRequest(); var fact = probe.getFact();
        if (!"at+jwt".equals(String.valueOf(jwt.getHeaders().get("typ"))) || !"SERVICE".equals(jwt.getClaimAsString("principal_type"))
                || !request.context().name().equals(jwt.getClaimAsString("scope_context"))
                || !fact.path("issuer").asText().equals(jwt.getIssuer().toString()) || !fact.path("clientId").asText().equals(jwt.getClaimAsString("client_id"))
                || !fact.path("clientPk").asText().equals(jwt.getClaimAsString("client_registration_id"))
                || !request.appId().equals(jwt.getClaimAsString("app_id")) || !java.util.Objects.equals(request.tenantId(), jwt.getClaimAsString("tenant_id"))
                || !List.of(request.audience().toASCIIString()).equals(jwt.getAudience()) || !request.scopes().equals(Set.of(jwt.getClaimAsString("scope").split(" ")))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    @RequiredArgsConstructor
    @Slf4j
    private static final class NativeServiceContext implements AutoCloseable {
        private final String role;
        private final IdpStarterProperties properties;
        private final Map<String, ClientRegistration> registrations;
        private final java.util.concurrent.ConcurrentHashMap<String, AtomicInteger> responses;
        private final java.util.concurrent.ConcurrentHashMap<String, CountDownLatch> misses;
        private final ThreadLocal<String> burstKeys;
        private final IdpServiceOAuth2Client client;
        private final TenantIssuerAuthenticationManagerComponent verifier;
        private final HttpClient http;

        @Override public void close() { client.close(); http.shutdownNow(); }
    }

    @Test
    void configuredServiceRegistrationRejectsUserPurposeAndForeignIdentityBeforeAnyRequest() {
        String issuer = "https://127.0.0.1:28443/tenants/101";
        var registration = ClientRegistration.withRegistrationId("controlled-native-a").clientId("controlled-client")
                .clientSecret("controlled-secret-not-live").clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).scope(Set.of("native:user:read"))
                .tokenUri(issuer + "/oauth2/token").issuerUri(issuer).build();
        var properties = new IdpStarterProperties();
        properties.getServiceClient().setAppId("controlled-native");
        properties.getServiceClient().setCredentialVersions(java.util.Map.of("controlled-native-a", 1L));
        var fact = UnifiedIdentityLiveClient.strictJson().createObjectNode().put("registrationId", "controlled-native-a")
                .put("clientId", "controlled-client").put("clientPk", 51).put("appId", "controlled-native").put("credentialVersion", 1)
                .put("purpose", "SERVICE").put("clientType", "CONFIDENTIAL").put("status", "ACTIVE").put("issuer", issuer)
                .put("refreshTokenTtlSeconds", 0).put("accessTokenTtlSeconds", 300);
        fact.putArray("allowedScopes").add("native:user:read");
        fact.putArray("resourceUris").add("https://127.0.0.1:28443/resources/native");
        assertThatCode(() -> requireServiceRegistration(registration, fact, properties)).doesNotThrowAnyException();
        for (String field : java.util.List.of("purpose", "clientId", "appId", "issuer")) {
            var foreign = fact.deepCopy().put(field, "foreign");
            assertThatThrownBy(() -> requireServiceRegistration(registration, foreign, properties)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
    }

    @Test
    void serviceScopeAndResponseGuardsRejectDuplicatesAndRefreshTokens() {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        assertThatThrownBy(() -> scopeSet(mapper.createArrayNode().add("native:read").add("native:read")))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> scopeSet(mapper.createArrayNode().add(true))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var normal = org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse.withToken("controlled-not-live")
                .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(300).scopes(Set.of("native:read")).build();
        assertThatCode(() -> requireNoRefreshToken(normal)).doesNotThrowAnyException();
        var unsafe = org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse.withToken("controlled-not-live")
                .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(300).refreshToken("controlled-not-live-refresh").build();
        assertThatThrownBy(() -> requireNoRefreshToken(unsafe)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    void serviceProofSerializesOnlyReferencesAndUtcInstants() {
        var instant = Instant.parse("2026-10-15T00:00:00Z");
        var proof = new ServiceCacheProofVO("native-light", "a".repeat(64), "b".repeat(64), "c".repeat(64), 1, 2, 8, 8, true, instant, instant);
        var json = UnifiedIdentityLiveClient.strictJson().valueToTree(proof);
        assertThat(json.path("initialExpiresAt").isTextual()).isTrue();
        assertThat(json.path("initialExpiresAt").textValue()).isEqualTo(instant.toString());
        assertThat(json.path("renewedAt").textValue()).isEqualTo(instant.toString());
        assertThat(json.has("access_token") || json.has("clientSecret") || json.has("refresh_token")).isFalse();
    }
}
