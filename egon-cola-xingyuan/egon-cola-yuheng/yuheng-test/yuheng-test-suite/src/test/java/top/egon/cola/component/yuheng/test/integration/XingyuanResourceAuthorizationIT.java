package top.egon.cola.component.yuheng.test.integration;

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.fasterxml.jackson.databind.JsonNode;
import top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient;
import top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient.HttpAuditObservationVO;
import top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient.FormalReferenceDTO;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient.boundedBody;
import static top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient.observedHttpCall;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class XingyuanResourceAuthorizationIT {

    record KnowledgeModelSelectionVO(String chatModel, String embeddingModel, String embeddingSpaceId, int dimensions) { }

    static KnowledgeModelSelectionVO knowledgePublishedModels(JsonNode models) {
        if (!models.isArray() || models.size() > 512) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var chats = new ArrayList<JsonNode>(); var embeddings = new ArrayList<JsonNode>(); var keys = new java.util.HashSet<String>();
        for (var model : models) {
            if (!model.path("key").isTextual() || !model.path("key").asText().matches("[a-z0-9][a-z0-9-]{0,63}") || !keys.add(model.path("key").asText())
                    || !model.path("enabled").isBoolean()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            if (!model.path("enabled").booleanValue()) { continue; }
            if ("CHAT".equals(model.path("kind").asText())) { chats.add(model); }
            else if ("EMBEDDING".equals(model.path("kind").asText())) {
                if (!model.path("embeddingSpaceId").isTextual() || model.path("embeddingSpaceId").asText().isBlank() || model.path("embeddingSpaceId").asText().length() > 128
                        || !model.path("dimensions").isIntegralNumber() || !model.path("dimensions").canConvertToInt() || model.path("dimensions").intValue() < 1
                        || model.path("dimensions").intValue() > 16000) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                embeddings.add(model);
            }
        }
        if (chats.isEmpty() || embeddings.isEmpty()) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
        chats.sort(java.util.Comparator.comparing(value -> value.path("key").asText())); embeddings.sort(java.util.Comparator.comparing(value -> value.path("key").asText()));
        var chat = chats.getFirst(); var embedding = embeddings.getFirst();
        return new KnowledgeModelSelectionVO(chat.path("key").asText(), embedding.path("key").asText(), embedding.path("embeddingSpaceId").asText(), embedding.path("dimensions").intValue());
    }

    static void requireManagedKnowledgeBase(JsonNode value, JsonNode command, KnowledgeModelSelectionVO models) {
        responseId(value);
        if (!command.path("name").equals(value.path("name")) || !command.path("description").equals(value.path("description"))
                || !"LOCAL_ONLY".equals(value.path("egressPolicy").asText()) || !"OWNER".equals(value.path("myRole").asText())
                || !models.chatModel().equals(value.path("chatModel").asText()) || !models.embeddingModel().equals(value.path("embeddingModel").asText())
                || !models.embeddingSpaceId().equals(value.path("embeddingSpaceId").asText()) || transactionProofLong(value, "dimensions") != models.dimensions()
                || !value.path("ownerActorId").isTextual() || value.path("ownerActorId").asText().isBlank() || value.path("ownerActorId").asText().length() > 128
                || transactionProofLong(value, "revision") != 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    @Test
    void knowledgeMetadataNeedsActuallyPublishedEnabledModelsAndExactOwnerProjection() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var models = mapper.createArrayNode();
        models.addObject().put("key", "chat-existing").put("enabled", true).put("kind", "CHAT");
        models.addObject().put("key", "embedding-existing").put("enabled", true).put("kind", "EMBEDDING").put("embeddingSpaceId", "space-existing").put("dimensions", 1536);
        var selected = knowledgePublishedModels(models); assertThat(selected.chatModel()).isEqualTo("chat-existing");
        var disabled = models.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) disabled.get(1)).put("enabled", false);
        assertThatThrownBy(() -> knowledgePublishedModels(disabled)).hasMessage("XINGYUAN_INPUT_UNAVAILABLE");
        var invalid = models.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.get(1)).put("dimensions", 0);
        assertThatThrownBy(() -> knowledgePublishedModels(invalid)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var duplicate = models.deepCopy().add(models.get(0)); assertThatThrownBy(() -> knowledgePublishedModels(duplicate)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var command = mapper.createObjectNode().put("name", "Task metadata").put("description", "No inference");
        var view = command.deepCopy().put("id", "123").put("ownerActorId", "actual-actor").put("myRole", "OWNER").put("egressPolicy", "LOCAL_ONLY")
                .put("chatModel", selected.chatModel()).put("embeddingModel", selected.embeddingModel()).put("embeddingSpaceId", selected.embeddingSpaceId()).put("dimensions", selected.dimensions()).put("revision", 1);
        assertThatCode(() -> requireManagedKnowledgeBase(view, command, selected)).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireManagedKnowledgeBase(view.deepCopy().put("chatModel", "unpublished"), command, selected)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> requireManagedKnowledgeBase(view.deepCopy().put("myRole", "READER"), command, selected)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualYuhengKnowledgeMetadataKeepsScopeCapabilityAndOriginalMembershipWithoutInference() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(bytes);
        String digest = UnifiedIdentityLiveClient.sha256(bytes); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        Path output = evidence.resolve("resource-knowledge-management.json"), intent = evidence.resolve("resource-knowledge-running.json");
        if (Files.exists(output, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
        if (Files.exists(intent, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng"));
        String resource = client.resourceForAlias("edge-yuheng"), base = "/api/v1/yuheng/admin/knowledge-bases", page = base + "?page=1&size=100";
        var a = client.formal("default", resource, "admin"); var b = client.formal("tenant-b", resource, "admin"); client.verifiedClaims(a); client.verifiedClaims(b);
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "YUHENG_ACTUAL_KNOWLEDGE_METADATA_SCOPE_AND_MEMBERSHIP_ONLY"); var observations = proof.putArray("observations");
        var created = new java.util.LinkedHashMap<String, JsonNode>();
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var api = new NativeResourceHttpProbe("yuheng", root, manifestFile, digest, http, timeout);
            if (!a.getScopes().containsAll(java.util.Set.of("yuheng:knowledge:read", "yuheng:knowledge:write", "yuheng:llm:read"))
                    || !b.getScopes().containsAll(java.util.Set.of("yuheng:knowledge:read", "yuheng:knowledge:write"))) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            var models = knowledgePublishedModels(api.request("GET", "/api/v1/yuheng/admin/llm/models", a, null, null, 200));
            var commands = new java.util.LinkedHashMap<String, JsonNode>();
            for (String realm : List.of("default", "tenant-b")) {
                var reference = realm.equals("default") ? a : b; var viewer = client.formal(realm, resource, "viewer"); client.verifiedClaims(viewer);
                var scopeOnly = acquireIdentityOnlyProbe(client, root, evidence, manifestFile, manifest, digest, realm, resource);
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "KNOWLEDGE_COUNTS", null);
                var baseline = api.request("GET", page, reference, null, null, 200);
                if (!baseline.path("items").isArray() || transactionProofLong(baseline, "page") != 1 || transactionProofLong(baseline, "size") != 100) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var success = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "KNOWLEDGE_COUNTS", null);
                UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(before, success, "listKnowledgeBases", 1);
                var command = mapper.createObjectNode().put("name", "it-" + manifest.path("runId").asText() + '-' + realm + '-' + UUID.randomUUID().toString().substring(0, 8))
                        .put("description", "Task metadata only").put("egressPolicy", "LOCAL_ONLY").put("chatModel", models.chatModel()).put("embeddingModel", models.embeddingModel());
                commands.put(realm, command);
                api.request("GET", page, null, null, null, 401); api.request("GET", page, scopeOnly, null, null, 403); api.request("GET", page, viewer, null, null, 403);
                api.request("POST", base, scopeOnly, command, null, 403); api.request("POST", base, viewer, command, null, 403); api.request("POST", base, null, command, null, 401);
                var denied = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "KNOWLEDGE_COUNTS", null);
                if (!denied.equals(success)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var recovery = api.request("GET", page, reference, null, null, 200); if (!recovery.equals(baseline)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var recovered = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "KNOWLEDGE_COUNTS", null);
                UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(denied, recovered, "listKnowledgeBases", 1);
                var item = observations.addObject().put("realmCode", realm).put("tokenReference", reference.getTokenSha256()).put("viewerReference", viewer.getTokenSha256())
                        .put("identityOnlyTokenReference", scopeOnly.getTokenSha256()).put("zeroMethodsDuringAuthorizationDenial", true)
                        .put("baselineMetadataSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(baseline)));
                item.set("before", before); item.set("successful", success); item.set("denied", denied); item.set("recovered", recovered);
            }
            XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, intent, mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                    .put("status", "RUNNING").put("commandSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(commands))).put("recordedAt", Instant.now().toString()));
            for (String realm : List.of("default", "tenant-b")) {
                var reference = realm.equals("default") ? a : b; var command = commands.get(realm);
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "KNOWLEDGE_COUNTS", null);
                var value = api.request("POST", base, reference, command, null, 201); requireManagedKnowledgeBase(value, command, models); created.put(realm, value);
                var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "KNOWLEDGE_COUNTS", null);
                UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(before, after, "createKnowledgeBase", 1);
                String path = base + '/' + responseId(value);
                if (!value.equals(api.request("GET", path, reference, null, null, 200))
                        || !value.equals(api.request("GET", path, reference, null, (realm.equals("default") ? b : a).getTenantId(), 200))) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
                var item = observations.addObject().put("realmCode", realm).put("knowledgeBaseId", Long.toString(responseId(value)))
                        .put("metadataSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(value)))
                        .put("ownerReference", UnifiedIdentityLiveClient.sha256(value.path("ownerActorId").asText().getBytes(StandardCharsets.UTF_8))).put("createStatus", 201);
                item.set("before", before); item.set("after", after);
            }
            boolean sameActor = created.get("default").path("ownerActorId").equals(created.get("tenant-b").path("ownerActorId"));
            proof.put("sameInstallationActorAcrossIssuers", sameActor);
            for (String realm : List.of("default", "tenant-b")) {
                var reference = realm.equals("default") ? a : b; var foreign = created.get(realm.equals("default") ? "tenant-b" : "default");
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "KNOWLEDGE_COUNTS", null);
                var response = api.request("GET", base + '/' + responseId(foreign), reference, null, null, sameActor ? 200 : 403);
                if (sameActor ? !response.equals(foreign) : !"KNOWLEDGE_FORBIDDEN".equals(response.path("code").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "KNOWLEDGE_COUNTS", null);
                UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(before, after, "getKnowledgeBase", 1);
                var own = created.get(realm); if (!own.equals(api.request("GET", base + '/' + responseId(own), reference, null, null, 200))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            }
        }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng"));
        proof.put("status", "PASSED").put("externalInferenceExecuted", false).put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, output, proof);
    }

    record GatewayTaskScopeVO(@NotBlank String groupCode, @NotBlank String env, @NotBlank String namespace) { }

    private static JsonNode requireFullPhase(Path root, Path evidence, JsonNode manifest, String digest, String filename, String status) throws Exception {
        requireEvidenceDirectory(root, evidence); Path relative = Path.of(filename);
        if (relative.isAbsolute() || !relative.normalize().equals(relative) || relative.startsWith("..")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var mapper = UnifiedIdentityLiveClient.strictJson();
        var value = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve(relative), evidence, 128 * 1024));
        var intent = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("intent.json"), evidence, 128 * 1024));
        if (!value.path("schemaVersion").isInt() || value.path("schemaVersion").intValue() != 1 || !manifest.path("runId").asText().equals(value.path("runId").asText())
                || !digest.equals(value.path("manifestSha256").asText()) || !status.equals(value.path("status").asText())
                || !manifest.path("runId").equals(intent.path("runId")) || !digest.equals(intent.path("manifestSha256").asText()) || !"full".equals(intent.path("caseSet").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        String time = value.has("completedAt") ? "completedAt" : value.has("recordedAt") ? "recordedAt" : value.has("observedAt") ? "observedAt" : "executedAt";
        Instant observed = transactionProofInstant(value, time);
        if (observed.isBefore(transactionProofInstant(intent, "startedAt")) || observed.isAfter(Instant.now())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return value;
    }

    @Test
    void fullResourceProofsCannotReuseAnotherAttemptStatusOrMutablePrivateFile(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Path base = Files.createDirectory(root.resolve("evidence"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path evidence = Files.createDirectory(base.resolve("verify-" + UUID.randomUUID()), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        var mapper = UnifiedIdentityLiveClient.strictJson(); String runId = UUID.randomUUID().toString(), digest = "a".repeat(64); Instant now = Instant.now();
        var manifest = mapper.createObjectNode().put("runId", runId); var intent = mapper.createObjectNode().put("runId", runId).put("manifestSha256", digest).put("caseSet", "full").put("startedAt", now.minusSeconds(2).toString());
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", runId).put("manifestSha256", digest).put("status", "PASSED").put("recordedAt", now.toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("intent.json"), intent);
        Path path = evidence.resolve("controlled.json"); XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, path, proof);
        assertThatCode(() -> requireFullPhase(root, evidence, manifest, digest, "controlled.json", "PASSED")).doesNotThrowAnyException();
        for (String fault : List.of("foreignRun", "foreignDigest", "oldAttempt", "future", "skipped", "schemaBoolean")) {
            var wrong = proof.deepCopy();
            if (fault.equals("foreignRun")) { wrong.put("runId", UUID.randomUUID().toString()); }
            else if (fault.equals("foreignDigest")) { wrong.put("manifestSha256", "b".repeat(64)); }
            else if (fault.equals("oldAttempt")) { wrong.put("recordedAt", now.minusSeconds(3).toString()); }
            else if (fault.equals("future")) { wrong.put("recordedAt", now.plusSeconds(3600).toString()); }
            else if (fault.equals("skipped")) { wrong.put("status", "SKIPPED"); }
            else { wrong.put("schemaVersion", true); }
            Files.write(path, mapper.writeValueAsBytes(wrong));
            assertThatThrownBy(() -> requireFullPhase(root, evidence, manifest, digest, "controlled.json", "PASSED")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        Files.write(path, mapper.writeValueAsBytes(proof)); Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r--r--"));
        assertThatThrownBy(() -> requireFullPhase(root, evidence, manifest, digest, "controlled.json", "PASSED")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> requireFullPhase(root, evidence, manifest, digest, "../controlled.json", "PASSED")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    private static List<String> requireBrowserAndOraclePhases(Path root, Path evidence, JsonNode manifest, String digest) throws Exception {
        var files = new ArrayList<String>();
        var execution = requireFullPhase(root, evidence, manifest, digest, "browser-BROWSER_EXECUTION.json", "PASSED");
        if (!"passed".equals(execution.path("expectedStatus").asText()) || !"passed".equals(execution.path("actualStatus").asText())
                || transactionProofLong(execution, "retry") != 0 || transactionProofLong(execution, "executedTestCount") != 1
                || !execution.path("titleReference").asText().matches("[0-9a-f]{64}")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        requireFullPhase(root, evidence, manifest, digest, "browser-AB_BROWSER.json", "PASSED");
        files.add("browser-BROWSER_EXECUTION.json"); files.add("browser-AB_BROWSER.json");
        var mapper = UnifiedIdentityLiveClient.strictJson(); var registry = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024));
        JsonNode first = null;
        for (String realm : List.of("TENANT_A", "TENANT_B")) {
            var canonical = requireFullPhase(root, evidence, manifest, digest, "browser-" + realm + "_CANONICAL.json", "PASSED");
            var value = canonical.path("canonical");
            if (!value.path("bindingReference").asText().matches("[0-9a-f]{64}") || !canonical.path("proofs").isArray() || canonical.path("proofs").size() != 7
                    || !transactionProofInstant(value, "absoluteExpiresAt").isAfter(transactionProofInstant(value, "originalCreatedAt"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            String realmCode = realm.equals("TENANT_A") ? "default" : "tenant-b";
            if (!registry.path("realms").path(realmCode).path("issuer").equals(value.path("issuer")) || !registry.path("tenantIds").path(realmCode).equals(value.path("tenantId"))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            var phases = new java.util.HashSet<String>();
            for (var reference : canonical.path("proofs")) {
                String nonce = reference.path("nonce").asText(), phase = reference.path("phase").asText();
                if (!nonce.matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}") || !phases.add(phase)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                Path file = root.resolve("evidence/runtime-probe/edge/results/" + nonce + ".json"); byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(file, root.resolve("evidence"), 16 * 1024);
                var original = mapper.readTree(bytes);
                if (!UnifiedIdentityLiveClient.sha256(bytes).equals(reference.path("ownerProofSha256").asText()) || !manifest.path("runId").equals(original.path("runId"))
                        || !digest.equals(original.path("manifestSha256").asText()) || !"EDGE_BINDING".equals(original.path("operation").asText()) || !"edge".equals(original.path("role").asText())
                        || !nonce.equals(original.path("nonce").asText()) || !"COMPLETED".equals(original.path("status").asText()) || !value.equals(original.path("result"))) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
            }
            if (!phases.equals(java.util.Set.of("PORTAL", "edge-shoubing", "edge-jianshen", "edge-yuheng", "edge-tianshu", "REFRESH", "STEP_UP"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            if (first == null) { first = value; }
            else if (first.path("issuer").equals(value.path("issuer")) || first.path("tenantId").equals(value.path("tenantId")) || first.path("bindingReference").equals(value.path("bindingReference"))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            files.add("browser-" + realm + "_CANONICAL.json");
            for (String phase : List.of("REFRESH", "STEP_UP", "HOLD", "REPLAY")) {
                String filename = "oracle-" + realm + '_' + phase + ".json";
                var proof = requireFullPhase(root, evidence, manifest, digest, filename, phase.equals("HOLD") ? "READY" : "PASSED");
                int count = phase.equals("REFRESH") ? 4 : phase.equals("STEP_UP") ? 1 : 5;
                if (!(realm + '_' + phase).equals(proof.path("phase").asText()) || !proof.path("proofs").isArray() || proof.path("proofs").size() != count) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                files.add(filename);
            }
        }
        for (String phase : List.of("HOLD", "REPLAY")) {
            String filename = "oracle-TENANT_A_FAULT_" + phase + ".json";
            var proof = requireFullPhase(root, evidence, manifest, digest, filename, phase.equals("HOLD") ? "READY" : "PASSED");
            if (!proof.path("proofs").isArray() || proof.path("proofs").size() != 5) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            files.add(filename);
        }
        requireFullPhase(root, evidence, manifest, digest, "oracle-COMPLETE.json", "PASSED"); files.add("oracle-COMPLETE.json");
        var pending = requireFullPhase(root, evidence, manifest, digest, "browser-AS_REVOCATION_PENDING.json", "PASSED");
        var recovered = requireFullPhase(root, evidence, manifest, digest, "browser-AS_REVOCATION_RECOVERED.json", "PASSED");
        if (transactionProofLong(pending, "responseStatus") != 503 || !"PENDING".equals(pending.path("remoteState").asText()) || !pending.path("localLoggedOut").isBoolean() || !pending.path("localLoggedOut").booleanValue()
                || transactionProofLong(pending, "durableIntentCount") != 5 || !pending.path("lateCallbackObserved").isBoolean() || !pending.path("lateCallbackObserved").booleanValue() || transactionProofLong(pending, "lateCallbackSessionStatus") != 401
                || pending.path("proofs").size() != 3 || transactionProofLong(recovered, "queuedCount") != 0 || transactionProofLong(recovered, "pendingCount") != 0
                || transactionProofLong(recovered, "sessionStatus") != 401 || recovered.path("proofs").size() != 5) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        files.add("browser-AS_REVOCATION_PENDING.json"); files.add("browser-AS_REVOCATION_RECOVERED.json");
        return List.copyOf(files);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void verifyResourceAuthorization() throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] raw = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(raw);
        String digest = UnifiedIdentityLiveClient.sha256(raw); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory")); requireEvidenceDirectory(root, evidence);
        if (evidence.equals(root.resolve("evidence"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var intent = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("intent.json"), evidence, 128 * 1024));
        if (!manifest.path("runId").asText().equals(intent.path("runId").asText()) || !digest.equals(intent.path("manifestSha256").asText())
                || !"full".equals(intent.path("caseSet").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        nativeUserHttpTargetsCannotChooseAnUnreviewedHostOrMutation(); nativeSqlTargetsRejectForeignReceiptsAndIdentifiersBeforeConnecting();
        gatewayOwnerCallsStayOnTheOriginalTaskManagementMethodsAndPaths(); mcpOwnerBoundaryOnlyAllowsTheOriginalServerAndResourceMethods();
        Path guards = evidence.resolve("resource-live-guards.json");
        var guard = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "observe-live-guards", manifestFile.toString(), guards.toString()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!guard.waitFor(manifest.path("deadlines").path("operationSeconds").intValue(), TimeUnit.SECONDS) || guard.exitValue() != 0) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        } finally { if (guard.isAlive()) { guard.destroy(); guard.waitFor(10, TimeUnit.SECONDS); } }
        var owned = requireFullPhase(root, evidence, manifest, digest, guards.getFileName().toString(), "PASSED");
        if (!"OWNED_RUNNING_COHORT_AND_ORIGINAL_FACILITY_GUARDS_ONLY".equals(owned.path("scope").asText()) || transactionProofLong(owned, "ownedDatabaseCount") != 9 || owned.path("proofs").size() != 3) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-001", "resource", "verifyResourceAuthorization", List.of("resource-live-guards.json"), false);
        var transactions = readJpaMpTransactionProofs(root, evidence, manifest, digest);
        var transactionFiles = new ArrayList<>(transactions.stream().map(path -> evidence.relativize(path).toString()).toList()); transactionFiles.add("junit-outbox.json");
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-002", "resource", "verifyResourceAuthorization", transactionFiles, false);
        threeIssuerMetadataUsesDistinctPublicKeysAndRetiresTheRoot();
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-003", "resource", "verifyResourceAuthorization", List.of("resource-issuer-discovery.json"), false);
        var browser = requireBrowserAndOraclePhases(root, evidence, manifest, digest);
        for (String id : List.of("LIVE-004", "LIVE-005", "LIVE-008", "LIVE-018", "LIVE-019", "LIVE-020")) {
            XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, id, "resource", "verifyResourceAuthorization", browser, false);
        }
        var formal = requireFullPhase(root, evidence, manifest, digest, "formal-inputs.json", "COMMITTED");
        if (!"CLI-006".equals(formal.path("operationId").asText()) || transactionProofLong(formal, "inputCount") < 1
                || formal.path("inputs").size() != transactionProofLong(formal, "inputCount")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        UnifiedIdentityLiveClient.isolated();
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-010", "resource", "verifyResourceAuthorization", List.of("formal-inputs.json"), false);
        for (String name : List.of("native-resource-grant.json", "native-field-write.json", "native-tenant-isolation.json")) { requireFullPhase(root, evidence, manifest, digest, name, "PASSED"); }
        var grant = requireFullPhase(root, evidence, manifest, digest, "native-resource-grant.json", "PASSED");
        if (!"ACTUAL_RESOURCE_GRANT_REMOVAL_AND_RESTORE_ONLY".equals(grant.path("scope").asText()) || grant.path("proofs").size() != 3
                || transactionProofLong(grant.path("proofs").get(1), "status") != 403 || transactionProofLong(grant.path("proofs").get(2), "status") != 200) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        platformHostsRejectTamperingAndForeignAudienceBeforeTheirOriginalBusiness();
        actualBizGatewayRejectsInvalidPublishedScopesAndRecoversThroughTheOriginalOwners();
        actualMcpResourceReadEnforcesPublishedScopeAndExactFinePermission();
        var platforms = requireFullPhase(root, evidence, manifest, digest, "resource-platform-http-boundaries.json", "PASSED");
        if (platforms.path("observations").size() != 8) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-006", "resource", "verifyResourceAuthorization",
                List.of("native-resource-grant.json", "native-resource-grant-result-01.json", "native-resource-grant-result-02.json", "resource-platform-http-boundaries.json", "resource-mcp-boundaries.json", "mcp-authorization.json", "mcp-authorization-execution.json"), false);
        var tenancy = new ArrayList<>(browser); tenancy.add("native-tenant-isolation.json"); tenancy.add("resource-platform-http-boundaries.json");
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-007", "resource", "verifyResourceAuthorization", tenancy, false);
        actualYuhengSharedStateIoUnavailabilityAndFixedMetricsRecover();
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-009", "resource", "verifyResourceAuthorization",
                List.of("resource-platform-http-boundaries.json", "resource-shared-state-metrics.json", "native-light-graphql.json", "native-web-graphql.json"), false);
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-016", "resource", "verifyResourceAuthorization", List.of("native-field-write.json", "junit-field-owner.json", "junit-field-consumer.json"), false);
        actualLlmProtocolsRejectMissingAudienceAndScopeBeforeInference(); actualYuhengKnowledgeMetadataKeepsScopeCapabilityAndOriginalMembershipWithoutInference();
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-017", "resource", "verifyResourceAuthorization",
                List.of("resource-llm-protocol-guards.json", "resource-knowledge-management.json"), false);
        nativeUserInfoRequestsUseRealRealmTokensAndReturnAuditCorrelation(); formalPublicFamilyRevocationRejectsTheSameUnexpiredTokenBeforeBusiness();
        for (String protocol : List.of("platform-http", "native-userinfo", "native-rpc", "mcp")) {
            var audit = requireFullPhase(root, evidence, manifest, digest, "protocol-" + protocol + "-audits.json", "PASSED");
            if (transactionProofLong(audit, "storedEventCount") != transactionProofLong(audit, "spoolAbsentAfterReceiptCount")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-021", "resource", "verifyResourceAuthorization",
                List.of("resource-native-userinfo.json", "resource-platform-http-boundaries.json", "native-rpc-contract-probe.json", "resource-mcp-boundaries.json", "protocol-platform-http-audits.json", "protocol-native-userinfo-audits.json", "protocol-native-rpc-audits.json", "protocol-mcp-audits.json", "resource-formal-family-revocation.json"), false);
        actualYuhengSpoolFaultsFailClosedAndRecoverWithoutEnteringBusiness();
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-022", "resource", "verifyResourceAuthorization", List.of("resource-spool-readiness.json", "resource-shared-state-metrics.json"), false);
        var faults = requireFullPhase(root, evidence, manifest, digest, "resource-biz-gateway-policy-rejection.json", "PASSED");
        if (faults.path("faults").size() != 6) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-024", "resource", "verifyResourceAuthorization",
                List.of("resource-platform-http-boundaries.json", "resource-biz-gateway-policy-rejection.json", "resource-mcp-boundaries.json", "mcp-publication.json", "mcp-authorization.json", "mcp-authorization-execution.json", "native-resource-grant.json", "resource-llm-protocol-guards.json", "resource-knowledge-management.json"), false);
        actualOwnedStopRetainsTheOriginalCohortAndBoundedPreservationEvidence();
        XingyuanNativeAuthenticationIT.publishLiveCase(root, evidence, manifest, digest, "LIVE-023", "resource", "verifyResourceAuthorization", List.of("resource-owned-stop-preservation.json"), false);
    }

    static GatewayTaskScopeVO gatewayTaskScope(Properties biz, Properties mcp, String runId) {
        try { if (!UUID.fromString(runId).toString().equals(runId)) { throw new IllegalArgumentException(); } }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var source = new java.util.LinkedHashMap<String, Object>(); biz.forEach((key, value) -> source.put(key.toString(), value));
        var engine = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(source))
                .bind("egon.cola.component.yuheng.engine", top.egon.cola.component.yuheng.engine.common.config.GatewayEngineRuntimeProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        var bizDdc = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(source))
                .bind("egon.cola.component.tianshu", top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        source.clear(); mcp.forEach((key, value) -> source.put(key.toString(), value));
        var protocol = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(source))
                .bind("egon.cola.component.yuheng.mcp-engine", top.egon.cola.component.yuheng.mcp.engine.config.McpGatewayEngineProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        var mcpDdc = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(source))
                .bind("egon.cola.component.tianshu", top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        source.clear(); String expected = "xingyuan-it-" + runId;
        if (!expected.equals(engine.getGatewayGroupCode()) || !expected.equals(protocol.yuhengGroupCode())
                || !"local".equals(engine.getEnv()) || !engine.getEnv().equals(protocol.env()) || !engine.getEnv().equals(bizDdc.getEnv()) || !protocol.env().equals(mcpDdc.getEnv())
                || !expected.equals(engine.getNamespace()) || !expected.equals(protocol.namespace())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return new GatewayTaskScopeVO(expected, engine.getEnv(), expected);
    }

    static java.util.Map<String, Object> gatewayProtectedReadPolicy(String requiredScope, String operationId) {
        if (requiredScope == null || !requiredScope.matches("[A-Za-z0-9._-]+(?::[A-Za-z0-9._-]+){1,3}") || requiredScope.length() > 128
                || operationId == null || !operationId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return java.util.Map.ofEntries(
                java.util.Map.entry("authenticationMode", "REQUIRED"),
                java.util.Map.entry("credentialExtractorIds", List.of(top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserCookieCredentialExtractor.EXTRACTOR_ID)),
                java.util.Map.entry("authenticationProviderIds", List.of(top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpIdentityAuthenticationProvider.PROVIDER_ID)),
                java.util.Map.entry("authorizationProviderIds", List.of(top.egon.cola.platform.tianquan.jianshen.yuheng.security.Rbac3BizAppScopeAuthorizationProvider.PROVIDER_ID)),
                java.util.Map.entry("decisionMode", "ALL_ALLOW"), java.util.Map.entry("failureMode", "FAIL_CLOSED"),
                java.util.Map.entry("providerTimeoutMs", 1000), java.util.Map.entry("credentialForwardingMode", "ORIGINAL_BEARER"),
                java.util.Map.entry("routeSecurityType", "BUSINESS_PROTECTED"), java.util.Map.entry("operationIds", List.of(operationId)),
                java.util.Map.entry("requiredOAuthScopes", List.of(requiredScope)));
    }

    static String mcpTaskServerCode(String runId) {
        if (runId == null || !runId.matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return "xingyuan-it-" + runId;
    }

    static java.util.Map<String, Object> mcpTaskReadPolicy(String runId) {
        String code = mcpTaskServerCode(runId);
        var policy = new java.util.LinkedHashMap<>(gatewayProtectedReadPolicy("yuheng:read", "123"));
        policy.put("operationIds", List.of("yuheng.mcp." + code));
        policy.put("authorizationProviderIds", List.of());
        policy.put("routeSecurityType", "IDENTITY_PROTECTED");
        return java.util.Map.copyOf(policy);
    }

    @Test
    void taskMcpPolicyBindsThePublishedServerAndKeepsTheCapabilityPdpSeparate() {
        String runId = UUID.randomUUID().toString(), code = mcpTaskServerCode(runId);
        var policy = mcpTaskReadPolicy(runId);
        assertThat(policy.get("operationIds")).isEqualTo(List.of("yuheng.mcp." + code));
        assertThat(policy.get("requiredOAuthScopes")).isEqualTo(List.of("yuheng:read"));
        assertThat(policy.get("routeSecurityType")).isEqualTo("IDENTITY_PROTECTED");
        assertThat(policy.get("authorizationProviderIds")).isEqualTo(List.of());
        assertThat(policy).doesNotContainKey("identityMapperId");
        assertThatThrownBy(() -> mcpTaskReadPolicy("other-run")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    static JsonNode mcpTaskServerRequest(String runId, String groupId, URI resourceUri, long revision) {
        String code = mcpTaskServerCode(runId);
        if (groupId == null || !groupId.matches("[A-Za-z0-9_-]{1,128}") || resourceUri == null || !"https".equals(resourceUri.getScheme())
                || resourceUri.getUserInfo() != null || resourceUri.getFragment() != null || resourceUri.getQuery() != null || revision < 0) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var request = new top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpServerRequestDTO(groupId, code, code,
                "XINGYUAN_TASK:" + runId, "Read the approved scope catalog only", java.util.Set.of("STABLE_2025_11_25"),
                resourceUri.toASCIIString(), 0, true, 0, revision, "Task protected MCP scope read");
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(request);
        }
        return UnifiedIdentityLiveClient.strictJson().valueToTree(request);
    }

    static java.util.Map<String, Object> mcpTaskResourceContent(String runId, String operationId) {
        mcpTaskServerCode(runId);
        if (operationId == null || !operationId.matches("[A-Za-z0-9_-]{1,128}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return java.util.Map.of("uri", "egon://" + mcpTaskServerCode(runId) + "/scopes", "mimeType", "application/json",
                "driverType", "LOCAL_OPERATION", "operationId", operationId, "configuration", java.util.Map.of(), "requiredPermissions", List.of(), "maxBytes", 65536);
    }

    static JsonNode mcpTaskCapabilityRequest(String runId, String groupId, String serverId, String operationId, long revision) {
        mcpTaskServerCode(runId);
        if (groupId == null || !groupId.matches("[A-Za-z0-9_-]{1,128}") || serverId == null || !serverId.matches("[A-Za-z0-9_-]{1,128}") || revision < 0) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var request = new top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpCapabilityRequestDTO(groupId, serverId, "scopes",
                mcpTaskResourceContent(runId, operationId), true, 0, revision, "Task original scope catalog read");
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(request);
        }
        return UnifiedIdentityLiveClient.strictJson().valueToTree(request);
    }

    @Test
    void taskMcpCommandsKeepTheOriginalDtosDriverAndExactServerOperationBinding() throws Exception {
        String runId = UUID.randomUUID().toString(); var mapper = UnifiedIdentityLiveClient.strictJson();
        var body = mcpTaskServerRequest(runId, "group-1", URI.create("https://resource.example/yuheng"), 7);
        var server = mapper.treeToValue(body, top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpServerRequestDTO.class);
        assertThat(server.serverCode()).isEqualTo(mcpTaskServerCode(runId)); assertThat(server.expectedDraftRevision()).isEqualTo(7);
        var capability = mapper.treeToValue(mcpTaskCapabilityRequest(runId, "group-1", "server-1", "123", 8), top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpCapabilityRequestDTO.class);
        assertThat(capability.serverId()).isEqualTo("server-1"); assertThat(capability.expectedDraftRevision()).isEqualTo(8);
        var value = capability.content();
        assertThat(new top.egon.cola.component.yuheng.mcp.resource.domain.McpResourceUriValidator().validate(value.get("uri").toString()).getScheme()).isEqualTo("egon");
        var resource = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeResource("resource-1", server.serverCode(), "scopes",
                value.get("uri").toString(), null, "application/json", value.get("driverType").toString(), "123", null, java.util.Map.of(), java.util.Set.of(), 65536, true);
        var runtime = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeServer("server-1", server.serverCode(), server.displayName(), server.description(), server.instructions(),
                java.util.Set.of(top.egon.cola.component.yuheng.contract.mcp.protocol.McpProtocolDialect.STABLE_2025_11_25), server.resourceUri(), 0, true);
        var content = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuleContent(List.of(runtime), List.of(), List.of(resource), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        var compiled = new top.egon.cola.component.yuheng.mcp.rule.service.McpRuleCompiler().compile(content, java.util.Set.of("123"));
        assertThat(compiled.resourcesByQualifiedName()).hasSize(1);
        assertThatThrownBy(() -> mcpTaskCapabilityRequest(runId, "group-1", "server-1", "foreign/path", 8)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> mcpTaskServerRequest(runId, "group-1", URI.create("https://user:secret@resource.example"), 7)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> mcpTaskServerRequest("foreign-run", "group-1", URI.create("https://resource.example"), 7)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    record GatewayReadOperationVO(String applicationId, String operationId, String definitionId, long definitionVersion,
                                  String definitionSha256, top.egon.cola.component.yuheng.contract.rule.GatewayProviderServiceRef provider) { }

    static void requireTaskMcpPreview(JsonNode preview, String runId, String serverId, String resourceId, String operationId, URI resourceUri) throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); String code = mcpTaskServerCode(runId);
        var content = preview.path("content");
        if (!preview.path("validation").path("valid").isBoolean() || !preview.path("validation").path("valid").booleanValue()) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var fields = new java.util.HashSet<String>(); content.fieldNames().forEachRemaining(fields::add);
        if (!fields.equals(java.util.Set.of("servers", "tools", "resources", "resourceTemplates", "prompts", "taskPolicies", "apps", "remoteProviders", "remoteMounts"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        for (String field : fields) {
            if (!content.path(field).isArray() || content.path(field).size() != (java.util.Set.of("servers", "resources").contains(field) ? 1 : 0)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        var original = mcpTaskServerRequest(runId, "group", resourceUri, 0);
        var expectedServer = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeServer(serverId, code, code, original.path("description").asText(),
                original.path("instructions").asText(), java.util.Set.of(top.egon.cola.component.yuheng.contract.mcp.protocol.McpProtocolDialect.STABLE_2025_11_25), resourceUri.toASCIIString(), 0, true);
        var expectedResource = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeResource(resourceId, code, "scopes", "egon://" + code + "/scopes",
                null, "application/json", "LOCAL_OPERATION", operationId, null, java.util.Map.of(), java.util.Set.of(), 65536, true);
        if (!mapper.valueToTree(expectedServer).equals(content.path("servers").get(0)) || !mapper.valueToTree(expectedResource).equals(content.path("resources").get(0))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        new top.egon.cola.component.yuheng.mcp.resource.domain.McpResourceUriValidator().validate(expectedResource.uri());
        new top.egon.cola.component.yuheng.mcp.rule.service.McpRuleCompiler().compile(mapper.treeToValue(content,
                top.egon.cola.component.yuheng.contract.mcp.rule.McpRuleContent.class), java.util.Set.of(operationId));
    }

    @Test
    void taskMcpPreviewRequiresOneOriginalServerResourceAndNoOtherCapabilities() throws Exception {
        String runId = UUID.randomUUID().toString(); var mapper = UnifiedIdentityLiveClient.strictJson();
        var request = mapper.treeToValue(mcpTaskServerRequest(runId, "group", URI.create("https://resource.example/yuheng"), 0),
                top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpServerRequestDTO.class);
        var server = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeServer("server-1", request.serverCode(), request.displayName(), request.description(), request.instructions(),
                java.util.Set.of(top.egon.cola.component.yuheng.contract.mcp.protocol.McpProtocolDialect.STABLE_2025_11_25), request.resourceUri(), 0, true);
        var resource = new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeResource("resource-1", request.serverCode(), "scopes", "egon://" + request.serverCode() + "/scopes", null,
                "application/json", "LOCAL_OPERATION", "123", null, java.util.Map.of(), java.util.Set.of(), 65536, true);
        var preview = mapper.createObjectNode(); preview.putObject("validation").put("valid", true);
        preview.set("content", mapper.valueToTree(new top.egon.cola.component.yuheng.contract.mcp.rule.McpRuleContent(List.of(server), List.of(), List.of(resource), List.of(), List.of(), List.of(), List.of(), List.of(), List.of())));
        assertThatCode(() -> requireTaskMcpPreview(preview, runId, "server-1", "resource-1", "123", URI.create(request.resourceUri()))).doesNotThrowAnyException();
        for (String field : List.of("servers", "resources", "tools", "remoteProviders")) {
            var changed = preview.deepCopy(); ((com.fasterxml.jackson.databind.node.ArrayNode) changed.path("content").path(field)).add(mapper.createObjectNode());
            assertThatThrownBy(() -> requireTaskMcpPreview(changed, runId, "server-1", "resource-1", "123", URI.create(request.resourceUri()))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        assertThatThrownBy(() -> requireTaskMcpPreview(preview, UUID.randomUUID().toString(), "server-1", "resource-1", "123", URI.create(request.resourceUri()))).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void publishTaskMcpServerAndResourceThroughOriginalOwners() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] raw = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(raw);
        String digest = UnifiedIdentityLiveClient.sha256(raw), runId = manifest.path("runId").asText(); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        var initialBytes = UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/gateway-publication.json"), root.resolve("evidence"), 128 * 1024);
        var initial = mapper.readTree(initialBytes); requireGatewayPublicationReceipt(initial, manifest, digest, initial.path("inputSha256").asText());
        var faults = XingyuanNativeAuthenticationIT.requireNativeEvidence(root, evidence, manifest, digest, "resource-biz-gateway-policy-rejection.json");
        if (!"BIZ_GATEWAY_ORIGINAL_OWNER_SCOPE_AND_SIGNEDHASH_NACK_LKG_RECOVERY_ONLY".equals(faults.path("scope").asText()) || faults.path("faults").size() != 6
                || !UnifiedIdentityLiveClient.sha256(initialBytes).equals(faults.path("initialOwnerReceiptSha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var recovered = faults.path("faults").get(5); String groupId = initial.path("gatewayGroupId").asText(), operationId = initial.path("operationId").asText();
        var expected = ((com.fasterxml.jackson.databind.node.ObjectNode) initial).deepCopy().put("releaseId", recovered.path("recoveredReleaseId").asText());
        var reference = client.formal("default", client.resourceForAlias("edge-yuheng"), "admin"); client.verifiedClaims(reference);
        if (!reference.getScopes().containsAll(java.util.Set.of("yuheng:read", "yuheng:mcp:read", "yuheng:mcp:write", "yuheng:drafts:write", "yuheng:releases:write"))) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        var scope = gatewayTaskScope(verifiedProfile(root, manifest, digest, "biz"), verifiedProfile(root, manifest, digest, "mcp"), runId);
        var registry = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024));
        var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                .filter(value -> reference.getResourceUri().toASCIIString().equals(value.path("resourceUri").asText()) && "ACTIVE".equals(value.path("status").asText())).toList();
        if (resources.size() != 1 || !"yuheng-admin".equals(resources.getFirst().path("rbacApplicationCode").asText())
                || !runId.equals(registry.path("runId").asText()) || !digest.equals(registry.path("manifestSha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var mcpProfile = verifiedProfile(root, manifest, digest, "mcp");
        if (!resources.getFirst().path("rbacApplicationCode").asText().equals(mcpProfile.getProperty("egon.cola.platform.tianquan.jianshen.system-code"))) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        String groupPath = "/api/v1/yuheng/admin/yuheng-groups/" + groupId, serverPath = "/api/v1/yuheng/admin/mcp/servers";
        Path intent = evidence.resolve("mcp-publication-running.json"), output = evidence.resolve("mcp-publication.json");
        if (Files.exists(intent, java.nio.file.LinkOption.NOFOLLOW_LINKS) || Files.exists(output, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var owner = new NativeResourceHttpProbe("yuheng", root, manifestFile, digest, http, timeout);
            var good = owner.request("GET", "/api/v1/yuheng/admin/releases/" + expected.path("releaseId").asText(), reference, null, null, 200);
            if (!UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(good)).equals(recovered.path("recoveredReleaseSha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            expected.put("ruleContentSha256", good.path("structuredDiff").path("ruleContentSha256").asText());
            verifyGatewayOwnerPublication(owner, reference, expected, scope, gatewayProtectedReadPolicy("yuheng:read", operationId), timeout);
            var servers = owner.request("GET", serverPath + "?gatewayGroupId=" + groupId, reference, null, null, 200);
            if (!servers.isArray() || !servers.isEmpty()) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            var draft = owner.request("GET", groupPath + "/draft", reference, null, null, 200); long revision = transactionProofLong(draft, "revision");
            requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "biz", "mcp", "tianshu"));
            XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, intent, mapper.createObjectNode().put("schemaVersion", 1).put("runId", runId).put("manifestSha256", digest)
                    .put("status", "RUNNING").put("previousReleaseId", good.path("releaseId").asText()).put("expectedDraftRevision", revision).put("recordedAt", Instant.now().toString()));
            var server = owner.mcpOwnerRequest("POST", serverPath, reference, mcpTaskServerRequest(runId, groupId, reference.getResourceUri(), revision), "it-mcp-server-" + runId, 201);
            String serverId = server.path("resourceId").asText(); requireFreshMcpMutation(server, revision); revision = transactionProofLong(server, "draftRevision");
            var resource = owner.mcpOwnerRequest("POST", serverPath + '/' + serverId + "/resources", reference, mcpTaskCapabilityRequest(runId, groupId, serverId, operationId, revision), "it-mcp-resource-" + runId, 201);
            String resourceId = resource.path("resourceId").asText(); requireFreshMcpMutation(resource, revision); revision = transactionProofLong(resource, "draftRevision");
            var policy = mapper.createObjectNode().put("policyType", "SECURITY").put("policyScope", "OPERATION").put("enabled", true).put("expectedRevision", revision)
                    .put("idempotencyKey", "it-mcp-security-" + runId).put("changeReason", "Task MCP exact published OAuth policy"); policy.set("content", mapper.valueToTree(mcpTaskReadPolicy(runId)));
            var mutation = owner.request("PUT", groupPath + "/draft/policies/it-mcp-security", reference, policy, null, 200);
            if (transactionProofLong(mutation, "revision") != Math.incrementExact(revision) || !"it-mcp-security".equals(mutation.path("resourceId").asText())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            var preview = owner.request("GET", serverPath + '/' + serverId + "/capability-preview", reference, null, null, 200);
            requireTaskMcpPreview(preview, runId, serverId, resourceId, operationId, reference.getResourceUri());
            var release = owner.request("POST", groupPath + "/releases", reference, mapper.createObjectNode().put("expectedDraftRevision", transactionProofLong(mutation, "revision")).put("changeReason", "Task MCP protected data read"), null, 200);
            if (!"SUCCESS".equals(release.path("status").asText()) || !groupId.equals(release.path("gatewayGroupId").asText()) || release.path("releaseId").equals(good.path("releaseId"))
                    || !release.path("partialApplied").isBoolean() || release.path("partialApplied").booleanValue()) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            var ready = mapper.createObjectNode().put("schemaVersion", 1).put("runId", runId).put("manifestSha256", digest).put("scope", "ORIGINAL_TASK_MCP_OWNER_PUBLICATION_ONLY").put("status", "COMMITTED")
                    .put("gatewayGroupId", groupId).put("serverId", serverId).put("serverCode", mcpTaskServerCode(runId)).put("resourceId", resourceId).put("resourceUri", reference.getResourceUri().toASCIIString())
                    .put("rbacApplicationCode", resources.getFirst().path("rbacApplicationCode").asText()).put("operationId", operationId).put("releaseId", release.path("releaseId").asText())
                    .put("ruleContentSha256", release.path("structuredDiff").path("ruleContentSha256").asText()).put("initialOwnerReceiptSha256", UnifiedIdentityLiveClient.sha256(initialBytes))
                    .put("previewSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(preview))).put("recordedAt", Instant.now().toString());
            verifyTaskMcpPublication(owner, reference, ready, scope, timeout);
            requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "biz", "mcp", "tianshu"));
            XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, output, ready);
        }
    }

    static void requireFreshMcpMutation(JsonNode result, long revision) {
        if (transactionProofLong(result, "draftRevision") != Math.incrementExact(revision) || transactionProofLong(result, "resourceRevision") != 0
                || !result.path("replayed").isBoolean() || result.path("replayed").booleanValue() || !result.path("resourceId").asText().matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreType
    record McpHttpExchangeVO(HttpAuditObservationVO observation, String sessionId, String body) {
        @Override public String toString() { return "McpHttpExchangeVO[status=" + observation.status() + ",redacted]"; }
    }

    private static McpHttpExchangeVO mcpDataRequest(HttpClient http, Duration timeout, Path root, String runId, FormalReferenceDTO reference,
            String sessionId, String method, String tenantHeader, int expected) throws Exception {
        if (!java.util.Set.of("initialize", "resources/read").contains(method) || method.equals("resources/read") && (sessionId == null || sessionId.isBlank())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        String code = mcpTaskServerCode(runId), trace = UUID.randomUUID().toString().replace("-", "");
        var mapper = UnifiedIdentityLiveClient.strictJson(); var body = mapper.createObjectNode().put("jsonrpc", "2.0").put("id", 1).put("method", method);
        var params = body.putObject("params");
        if (method.equals("initialize")) { params.put("protocolVersion", "2025-11-25"); }
        else { params.put("uri", "egon://" + code + "/scopes"); }
        var request = HttpRequest.newBuilder(URI.create("https://127.0.0.1:28185/mcp/" + code)).timeout(timeout).header("content-type", "application/json")
                .header("Mcp-Protocol-Version", "2025-11-25").header("traceparent", "00-" + trace + '-' + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01");
        if (sessionId != null) { request.header("Mcp-Session-Id", sessionId); }
        if (tenantHeader != null) { request.header("X-Tenant-Id", tenantHeader); }
        if (reference != null) {
            byte[] token = UnifiedIdentityLiveClient.protectedBytes(reference.getTokenFile(), root.resolve("secrets"), 128 * 1024);
            try {
                if (!reference.getExpiresAt().isAfter(Instant.now()) || !reference.getTokenSha256().equals(UnifiedIdentityLiveClient.sha256(token))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                request.header("Authorization", "Bearer " + new String(token, StandardCharsets.UTF_8));
            } finally { java.util.Arrays.fill(token, (byte) 0); }
        }
        Instant started = Instant.now(); var captured = new ByteArrayOutputStream();
        var response = http.send(request.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), boundedBody(captured));
        Instant completed = Instant.now();
        if (response.statusCode() != expected || expected != 200 && !response.headers().firstValue("cache-control").orElse("").equals("no-store")) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var content = mapper.readTree(captured.toByteArray());
        if (expected == 200 && (content.hasNonNull("error") || !content.path("result").isObject())
                || expected != 200 && !content.hasNonNull("error")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        // MCP uses the supplied traceparent directly; the signed owner observation proves this correlation.
        return new McpHttpExchangeVO(new HttpAuditObservationVO(trace, expected, started, completed), response.headers().firstValue("mcp-session-id").orElse(null), captured.toString(StandardCharsets.UTF_8));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualMcpResourceReadEnforcesPublishedScopeAndExactFinePermission() throws Exception {
        publishTaskMcpServerAndResourceThroughOriginalOwners();
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] raw = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(raw);
        String digest = UnifiedIdentityLiveClient.sha256(raw), runId = manifest.path("runId").asText(); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        Path ownerResult = evidence.resolve("mcp-authorization-execution.json");
        var owner = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "prepare-mcp-authorization", manifestFile.toString(), ownerResult.toString()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!owner.waitFor(manifest.path("deadlines").path("operationSeconds").intValue(), TimeUnit.SECONDS)) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            if (owner.exitValue() != 0) {
                throw new IllegalStateException(switch (owner.exitValue()) {
                    case 3 -> "XINGYUAN_TARGET_UNSAFE"; case 4 -> "XINGYUAN_INPUT_UNAVAILABLE"; case 5 -> "XINGYUAN_STATE_CONFLICT";
                    case 6 -> "XINGYUAN_DEPENDENCY_UNAVAILABLE"; case 9 -> "XINGYUAN_COMMIT_UNKNOWN"; default -> "XINGYUAN_VERIFICATION_FAILED";
                });
            }
        } finally { if (owner.isAlive()) { owner.destroy(); owner.waitFor(10, TimeUnit.SECONDS); } }
        var execution = XingyuanNativeAuthenticationIT.requireNativeEvidence(root, evidence, manifest, digest, ownerResult.getFileName().toString());
        byte[] authorityBytes = UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("mcp-authorization.json"), evidence, 128 * 1024); var authority = mapper.readTree(authorityBytes);
        if (!"MCP_TASK_AUTHORIZATION_OWNER_EXECUTION_ONLY".equals(execution.path("scope").asText())
                || !"top.egon.cola.platform.tianquan.jianshen.admin.integration.IsolatedAuthorizationProvisioningIT".equals(execution.path("execution").path("classname").asText())
                || transactionProofLong(execution.path("execution"), "testCount") != 1 || execution.path("execution").path("methods").size() != 1
                || !"provisionTaskMcpAuthorization".equals(execution.path("execution").path("methods").get(0).asText())
                || !root.relativize(evidence.resolve("mcp-authorization.json")).toString().equals(execution.path("proof").path("path").asText())
                || !UnifiedIdentityLiveClient.sha256(authorityBytes).equals(execution.path("proof").path("sha256").asText())
                || !("mcp:" + mcpTaskServerCode(runId) + ":resource:scopes:read").equals(authority.path("permissionCode").asText())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        var output = mapper.createObjectNode().put("schemaVersion", 1).put("runId", runId).put("manifestSha256", digest)
                .put("scope", "TASK_MCP_REAL_RESOURCE_READ_SCOPE_AND_FINE_PERMISSION_ONLY"); var observations = output.putArray("observations");
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            for (String realm : List.of("default", "tenant-b")) {
                var admin = client.formal(realm, client.resourceForAlias("edge-yuheng"), "admin"); var viewer = client.formal(realm, client.resourceForAlias("edge-yuheng"), "viewer");
                var wrong = client.formal(realm, client.resourceForAlias("edge-tianshu"), "admin");
                client.verifiedClaims(admin); client.verifiedClaims(viewer); client.verifiedClaims(wrong);
                var scopeOnly = acquireIdentityOnlyProbe(client, root, evidence, manifestFile, manifest, digest, realm, client.resourceForAlias("edge-yuheng"));
                var initialized = mcpDataRequest(http, timeout, root, runId, admin, null, "initialize", null, 200);
                var viewerInitialized = mcpDataRequest(http, timeout, root, runId, viewer, null, "initialize", null, 200);
                if (initialized.sessionId() == null || viewerInitialized.sessionId() == null || initialized.sessionId().equals(viewerInitialized.sessionId())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var requests = observations.addObject().put("realmCode", realm).put("tenantId", admin.getTenantId()).put("resourceUri", admin.getResourceUri().toASCIIString())
                        .put("adminReference", admin.getTokenSha256()).put("viewerReference", viewer.getTokenSha256()).put("identityOnlyReference", scopeOnly.getTokenSha256()).putArray("requests");
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "BUSINESS_COUNTS", null);
                for (int index = 0; index < 6; index++) {
                    var reference = index == 1 ? viewer : index == 2 ? scopeOnly : index == 3 ? wrong : index == 4 ? null : admin;
                    int status = index == 1 || index == 2 ? 403 : index == 3 || index == 4 ? 401 : 200;
                    var response = mcpDataRequest(http, timeout, root, runId, reference, index == 1 ? viewerInitialized.sessionId() : initialized.sessionId(), "resources/read",
                            index == 5 ? client.formal(realm.equals("default") ? "tenant-b" : "default", client.resourceForAlias("edge-yuheng"), "admin").getTenantId() : null, status);
                    var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "BUSINESS_COUNTS", null);
                    if (status == 200) {
                        requireOneBusinessInvocation(before, after);
                        var data = mapper.readTree(response.body());
                        if (!data.path("result").path("contents").isArray() || data.path("result").path("contents").size() != 1
                                || !("egon://" + mcpTaskServerCode(runId) + "/scopes").equals(data.path("result").path("contents").get(0).path("uri").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    } else if (!before.equals(after)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    var item = requests.addObject().put("kind", List.of("SUCCESS", "FINE_DENIED", "SCOPE_DENIED", "WRONG_AUDIENCE", "MISSING", "RECOVERY").get(index))
                            .put("responseSha256", UnifiedIdentityLiveClient.sha256(response.body().getBytes(StandardCharsets.UTF_8)));
                    item.set("request", mapper.valueToTree(response.observation())); item.set("before", before); item.set("after", after); before = after;
                }
            }
        }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "mcp", "jianshen", "shoubing"));
        output.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-mcp-boundaries.json"), output);
        observeHttpAuditOwner(root, evidence, manifestFile, manifest, digest, "mcp");
    }

    static java.util.Map<String, Object> gatewayReadRouteContent(String path) {
        if (!java.util.Set.of("/api/v1/yuheng/admin/scopes", "/api/v1/yuheng/admin/scopes/__xingyuan-required-scope").contains(path)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return java.util.Map.of("host", "*", "httpMethod", "GET", "pathPattern", path, "priority", 0, "accessZones", List.of("PUBLIC"));
    }

    @Test
    void taskReadRouteMeetsTheOriginalTransportContractBeforeAnyOwnerWrite() {
        var validator = new top.egon.cola.component.yuheng.admin.routing.service.GatewayRouteTransportPolicyValidator();
        for (String path : List.of("/api/v1/yuheng/admin/scopes", "/api/v1/yuheng/admin/scopes/__xingyuan-required-scope")) {
            assertThat(validator.validate(gatewayReadRouteContent(path), top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol.HTTP,
                    top.egon.cola.component.yuheng.core.route.GatewayResponseMode.TRANSPARENT)).isEmpty();
        }
        assertThatThrownBy(() -> gatewayReadRouteContent("/foreign/path")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    static JsonNode gatewayScopeReadNode(JsonNode catalog, String applicationId) {
        if (!catalog.path("applicationId").asText().equals(applicationId)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var candidates = new ArrayList<JsonNode>();
        for (var group : catalog.findValues("operations")) {
            if (!group.isArray() || group.size() > 2048) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            for (var node : group) {
                if ("HTTP".equals(node.path("protocol").asText()) && "GET /api/v1/yuheng/admin/scopes".equals(node.path("methodIdentity").asText())) {
                    if (!"OPENAPI31".equals(node.path("sourceType").asText()) || !"DISCOVERED".equals(node.path("lifecycleStatus").asText())
                            || !node.path("externalAccessible").isBoolean() || !node.path("externalAccessible").booleanValue()
                            || !node.path("id").asText().matches("[A-Za-z0-9_-]{1,128}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    candidates.add(node);
                }
            }
        }
        if (candidates.size() != 1) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
        return candidates.getFirst();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void publishTaskGatewayGroupAndProtectedReadRouteThroughOriginalOwners() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] raw = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(raw);
        String digest = UnifiedIdentityLiveClient.sha256(raw); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = root.resolve("evidence"); requireEvidenceDirectory(root, evidence);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "biz", "mcp", "tianshu"));
        var scope = gatewayTaskScope(verifiedProfile(root, manifest, digest, "biz"), verifiedProfile(root, manifest, digest, "mcp"), manifest.path("runId").asText());
        var properties = verifiedProfile(root, manifest, digest, "yuheng"); var flat = new java.util.LinkedHashMap<String, Object>(); properties.forEach((key, value) -> flat.put(key.toString(), value));
        var owner = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(flat))
                .bind("egon.cola.component.tianshu", top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE")); flat.clear(); properties.clear();
        var reference = client.formal("default", client.resourceForAlias("edge-yuheng"), "admin"); client.verifiedClaims(reference);
        if (!reference.getScopes().containsAll(java.util.Set.of("yuheng:read", "yuheng:groups:write", "yuheng:drafts:write", "yuheng:releases:write"))) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()); String base = "/api/v1/yuheng/admin";
        try (var transport = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var api = new NativeResourceHttpProbe("yuheng", root, manifestFile, digest, transport, timeout);
            var applications = api.request("GET", base + "/applications", reference, null, null, 200);
            if (!applications.isArray() || applications.size() > 512) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            var matches = java.util.stream.StreamSupport.stream(applications.spliterator(), false)
                    .filter(value -> owner.getBizCode().equals(value.path("bizCode").asText()) && owner.getAppCode().equals(value.path("applicationCode").asText())
                            && owner.getEnv().equals(value.path("env").asText()) && owner.getNamespace().equals(value.path("namespace").asText())).toList();
            if (matches.size() != 1 || !matches.getFirst().path("id").asText().matches("[A-Za-z0-9_-]{1,128}")) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            String applicationId = matches.getFirst().path("id").asText();
            var catalog = api.request("GET", base + "/applications/" + applicationId + "/catalog", reference, null, null, 200);
            var node = gatewayScopeReadNode(catalog, applicationId);
            var detail = api.request("GET", base + "/operations/" + node.path("id").asText(), reference, null, null, 200);
            var operation = gatewayReadOperation(matches.getFirst(), detail, owner);
            if (!node.path("id").asText().equals(operation.operationId())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var configuration = gatewayProtectedReadPolicy("yuheng:read", operation.operationId());
            var input = mapper.createObjectNode().put("runId", manifest.path("runId").asText()).put("manifestSha256", digest);
            input.set("scope", mapper.valueToTree(scope)); input.set("operation", mapper.valueToTree(operation));
            var profiles = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("profiles-ready.json"), evidence, 128 * 1024));
            for (String role : List.of("yuheng", "biz", "mcp")) { input.put(role + "ProfileSha256", profiles.path("profiles").path(role).path("sha256").asText()); }
            String inputHash = UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(input)); Path readyFile = evidence.resolve("gateway-publication.json");
            if (Files.exists(readyFile, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                var ready = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(readyFile, evidence, 128 * 1024));
                requireGatewayPublicationReceipt(ready, manifest, digest, inputHash);
                verifyGatewayOwnerPublication(api, reference, ready, scope, configuration, timeout);
                return;
            }
            Path intent = evidence.resolve("gateway-publication-running.json");
            if (Files.exists(intent, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            var groups = api.request("GET", base + "/yuheng-groups", reference, null, null, 200);
            if (!groups.isArray() || java.util.stream.StreamSupport.stream(groups.spliterator(), false).anyMatch(value -> scope.groupCode().equals(value.path("gatewayGroupCode").asText()))) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, intent, mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText())
                    .put("manifestSha256", digest).put("inputSha256", inputHash).put("status", "RUNNING").put("recordedAt", Instant.now().toString()));
            var groupBody = mapper.createObjectNode().put("gatewayGroupCode", scope.groupCode()).put("displayName", scope.groupCode()).put("env", scope.env())
                    .put("namespace", scope.namespace()).put("description", "XINGYUAN_TASK:" + manifest.path("runId").asText());
            var group = api.request("POST", base + "/yuheng-groups", reference, groupBody, null, 201); String groupId = group.path("id").asText();
            if (!groupId.matches("[A-Za-z0-9_-]{1,128}") || !scope.groupCode().equals(group.path("gatewayGroupCode").asText())
                    || !scope.env().equals(group.path("env").asText()) || !scope.namespace().equals(group.path("namespace").asText())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            String draftPath = base + "/yuheng-groups/" + groupId + "/draft";
            var draft = api.request("GET", draftPath, reference, null, null, 200);
            if (!draft.path("routes").isArray() || !draft.path("routes").isEmpty() || !draft.path("policies").isArray() || !draft.path("policies").isEmpty()) {
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
            long version = transactionProofLong(draft, "revision");
            var policy = mapper.createObjectNode().put("policyType", "SECURITY").put("policyScope", "OPERATION").put("enabled", true).put("expectedRevision", version)
                    .put("idempotencyKey", "it-policy-" + manifest.path("runId").asText()).put("changeReason", "Task protected read route");
            policy.set("content", mapper.valueToTree(configuration));
            var policyResult = api.request("PUT", draftPath + "/policies/it-read-security", reference, policy, null, 200);
            if (transactionProofLong(policyResult, "revision") != Math.addExact(version, 1) || !"it-read-security".equals(policyResult.path("resourceId").asText())) {
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
            version = transactionProofLong(policyResult, "revision");
            var route = mapper.createObjectNode().put("operationId", operation.operationId()).put("enabled", true).put("expectedRevision", version)
                    .put("idempotencyKey", "it-route-" + manifest.path("runId").asText()).put("changeReason", "Task protected read route");
            route.set("content", mapper.valueToTree(gatewayReadRouteContent("/api/v1/yuheng/admin/scopes")));
            var routeResult = api.request("PUT", draftPath + "/routes/it-protected-read", reference, route, null, 200);
            if (transactionProofLong(routeResult, "revision") != Math.addExact(version, 1) || !"it-protected-read".equals(routeResult.path("resourceId").asText())) {
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
            var release = api.request("POST", base + "/yuheng-groups/" + groupId + "/releases", reference,
                    mapper.createObjectNode().put("expectedDraftRevision", transactionProofLong(routeResult, "revision")).put("changeReason", "Task protected read route"), null, 200);
            if (!release.path("releaseId").asText().matches("[A-Za-z0-9_-]{1,128}") || !"SUCCESS".equals(release.path("status").asText())
                    || !release.path("partialApplied").isBoolean() || release.path("partialApplied").booleanValue()
                    || !release.path("structuredDiff").path("ruleContentSha256").asText().matches("[0-9a-f]{64}")) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            var ready = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest).put("inputSha256", inputHash)
                    .put("scope", "ORIGINAL_TASK_GATEWAY_OWNER_PUBLICATION_ONLY").put("status", "COMMITTED").put("gatewayGroupId", groupId).put("operationId", operation.operationId())
                    .put("definitionId", operation.definitionId()).put("definitionSha256", operation.definitionSha256()).put("releaseId", release.path("releaseId").asText())
                    .put("ruleContentSha256", release.path("structuredDiff").path("ruleContentSha256").asText());
            var consistency = verifyGatewayOwnerPublication(api, reference, ready, scope, configuration, timeout);
            ready.put("runtimeProjectionSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(consistency))).put("recordedAt", Instant.now().toString());
            requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "biz", "mcp", "tianshu"));
            XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, readyFile, ready);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualBizGatewayRejectsInvalidPublishedScopesAndRecoversThroughTheOriginalOwners() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] raw = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(raw);
        String digest = UnifiedIdentityLiveClient.sha256(raw); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "biz", "mcp", "tianshu"));
        var scope = gatewayTaskScope(verifiedProfile(root, manifest, digest, "biz"), verifiedProfile(root, manifest, digest, "mcp"), manifest.path("runId").asText());
        Path receiptFile = root.resolve("evidence/gateway-publication.json"); byte[] original = UnifiedIdentityLiveClient.protectedBytes(receiptFile, root.resolve("evidence"), 128 * 1024);
        var receipt = mapper.readTree(original); requireGatewayPublicationReceipt(receipt, manifest, digest, receipt.path("inputSha256").asText());
        var configuration = gatewayProtectedReadPolicy("yuheng:read", receipt.path("operationId").asText());
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        var reference = client.formal("default", client.resourceForAlias("edge-yuheng"), "admin"); client.verifiedClaims(reference);
        String path = "/api/v1/yuheng/admin/scopes", candidatePath = path + "/__xingyuan-required-scope", groupId = receipt.path("gatewayGroupId").asText();
        String groupPath = "/api/v1/yuheng/admin/yuheng-groups/" + groupId;
        var output = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "BIZ_GATEWAY_ORIGINAL_OWNER_SCOPE_AND_SIGNEDHASH_NACK_LKG_RECOVERY_ONLY").put("initialOwnerReceiptSha256", UnifiedIdentityLiveClient.sha256(original));
        var identities = output.putArray("identities"); var faults = output.putArray("faults");
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var owner = new NativeResourceHttpProbe("yuheng", root, manifestFile, digest, http, timeout); var gateway = new NativeResourceHttpProbe("biz", root, manifestFile, digest, http, timeout);
            var active = verifyGatewayOwnerPublication(owner, reference, receipt, scope, configuration, timeout);
            for (String realm : List.of("default", "tenant-b")) {
                var admin = client.formal(realm, client.resourceForAlias("edge-yuheng"), "admin"); client.verifiedClaims(admin);
                var viewer = client.formal(realm, client.resourceForAlias("edge-yuheng"), "viewer"); client.verifiedClaims(viewer);
                var identityOnly = acquireIdentityOnlyProbe(client, root, evidence, manifestFile, manifest, digest, realm, admin.getResourceKey());
                var foreign = client.formal(realm, client.resourceForAlias("edge-shoubing"), "admin"); client.verifiedClaims(foreign);
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "BUSINESS_COUNTS", null);
                var expected = gateway.request("GET", path, admin, null, null, 200);
                var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "BUSINESS_COUNTS", null); requireOneBusinessInvocation(before, after);
                var proof = identities.addObject().put("realmCode", realm).put("validStatus", 200); proof.set("before", before); proof.set("after", after);
                var denials = proof.putArray("denials");
                for (int failure = 0; failure < 4; failure++) {
                    var denied = failure == 0 ? null : failure == 1 ? identityOnly : failure == 2 ? viewer : foreign;
                    before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "BUSINESS_COUNTS", null);
                    int status = failure == 0 || failure == 3 ? 401 : 403;
                    var rejection = gateway.request("GET", path, denied, null, null, status);
                    after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "BUSINESS_COUNTS", null);
                    if (!before.equals(after)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    var denial = denials.addObject().put("kind", List.of("MISSING_TOKEN", "IDENTITY_ONLY_SCOPE", "VIEWER_PDP", "FOREIGN_AUDIENCE").get(failure))
                            .put("status", status).put("responseSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(rejection)));
                    denial.set("before", before); denial.set("after", after);
                }
                before = after;
                assertThat(gateway.request("GET", path, admin, null, client.formal(realm.equals("default") ? "tenant-b" : "default", admin.getResourceKey(), "admin").getTenantId(), 200)).isEqualTo(expected);
                after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", realm, "BUSINESS_COUNTS", null); requireOneBusinessInvocation(before, after);
                proof.put("deniedBusinessInvocations", 0).put("forgedTenantHeaderIgnored", true).put("subsequentNormalRead", true);
                proof.set("subsequentBefore", before); proof.set("subsequentAfter", after);
            }
            Path intent = evidence.resolve("resource-biz-gateway-policy-running.json");
            if (Files.exists(intent, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, intent, mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText())
                    .put("manifestSha256", digest).put("status", "RUNNING").put("initialOwnerReceiptSha256", UnifiedIdentityLiveClient.sha256(original)).put("recordedAt", Instant.now().toString()));
            for (String fault : List.of("MISSING", "STRING", "DUPLICATE", "IDENTITY_ONLY")) {
                if (!java.util.Arrays.equals(original, UnifiedIdentityLiveClient.protectedBytes(receiptFile, root.resolve("evidence"), 128 * 1024))
                        || !scope.equals(gatewayTaskScope(verifiedProfile(root, manifest, digest, "biz"), verifiedProfile(root, manifest, digest, "mcp"), manifest.path("runId").asText()))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var invalid = new java.util.LinkedHashMap<>(configuration);
                if (fault.equals("MISSING")) { invalid.remove("requiredOAuthScopes"); }
                else { invalid.put("requiredOAuthScopes", fault.equals("STRING") ? "yuheng:read" : fault.equals("DUPLICATE") ? List.of("yuheng:read", "yuheng:read") : List.of("openid")); }
                long revision = replaceOwnedGatewayPolicyAndRoute(owner, reference, groupId, receipt.path("operationId").asText(), invalid, candidatePath);
                var rejected = owner.request("POST", groupPath + "/releases", reference, mapper.createObjectNode().put("expectedDraftRevision", revision).put("changeReason", "Task required scope " + fault), null, 200);
                if (!groupId.equals(rejected.path("gatewayGroupId").asText())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
                JsonNode observed = null; long deadline = System.nanoTime() + timeout.toNanos();
                while (System.nanoTime() < deadline) {
                    observed = owner.request("GET", groupPath + "/runtime-consistency", reference, null, null, 200);
                    if (gatewayRejectedPublicationPreservesTheAppliedRules(rejected, active, observed, Instant.now())) { break; }
                    Thread.sleep(200);
                }
                if (observed == null || !gatewayRejectedPublicationPreservesTheAppliedRules(rejected, active, observed, Instant.now())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
                var denied = gateway.request("GET", candidatePath, reference, null, null, 404);
                if (!"YUHENG_ROUTE_NOT_FOUND".equals(denied.path("code").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
                if (!before.equals(after)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var rejectedBefore = before; var rejectedAfter = after;
                revision = replaceOwnedGatewayPolicyAndRoute(owner, reference, groupId, receipt.path("operationId").asText(), configuration, path);
                var recovered = owner.request("POST", groupPath + "/releases", reference, mapper.createObjectNode().put("expectedDraftRevision", revision).put("changeReason", "Task restore declared business scope"), null, 200);
                if (!groupId.equals(recovered.path("gatewayGroupId").asText()) || recovered.path("releaseId").equals(rejected.path("releaseId")) || recovered.path("releaseId").equals(receipt.path("releaseId"))) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
                var recoveryExpectation = ((com.fasterxml.jackson.databind.node.ObjectNode) receipt).deepCopy().put("releaseId", recovered.path("releaseId").asText())
                        .put("ruleContentSha256", recovered.path("structuredDiff").path("ruleContentSha256").asText());
                var next = verifyGatewayOwnerPublication(owner, reference, recoveryExpectation, scope, configuration, timeout);
                before = after; gateway.request("GET", path, reference, null, null, 200);
                after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null); requireOneBusinessInvocation(before, after);
                var item = faults.addObject().put("fault", fault).put("rejectedReleaseId", rejected.path("releaseId").asText()).put("recoveredReleaseId", recovered.path("releaseId").asText())
                        .put("rejectedReleaseSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(rejected))).put("recoveredReleaseSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(recovered)))
                        .put("partialTechnicalPublication", rejected.path("partialApplied").booleanValue())
                        .put("unactivatedRouteStatus", 404).put("deniedBusinessInvocations", 0).put("recoveryStatus", 200);
                item.set("beforeProjection", active); item.set("rejectedProjection", observed); item.set("recoveredProjection", next); active = next;
                item.set("beforeRejectedRoute", rejectedBefore); item.set("afterRejectedRoute", rejectedAfter); item.set("beforeRecoveryRead", before); item.set("afterRecoveryRead", after);
            }
            var configOwner = new NativeResourceHttpProbe("tianshu", root, manifestFile, digest, http, timeout);
            var configReference = client.formal("default", client.resourceForAlias("edge-tianshu"), "admin"); client.verifiedClaims(configReference);
            for (String fault : List.of("TAMPERED", "UNSIGNED")) {
                requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "biz", "mcp", "tianshu"));
                if (!java.util.Arrays.equals(original, UnifiedIdentityLiveClient.protectedBytes(receiptFile, root.resolve("evidence"), 128 * 1024))
                        || !scope.equals(gatewayTaskScope(verifiedProfile(root, manifest, digest, "biz"), verifiedProfile(root, manifest, digest, "mcp"), manifest.path("runId").asText()))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var goodRelease = owner.request("GET", "/api/v1/yuheng/admin/releases/" + active.path("targetReleaseId").asText(), reference, null, null, 200);
                var expectation = ((com.fasterxml.jackson.databind.node.ObjectNode) receipt).deepCopy().put("releaseId", goodRelease.path("releaseId").asText())
                        .put("ruleContentSha256", goodRelease.path("structuredDiff").path("ruleContentSha256").asText());
                active = verifyGatewayOwnerPublication(owner, reference, expectation, scope, configuration, timeout);
                var attempts = java.util.stream.StreamSupport.stream(goodRelease.path("attempts").spliterator(), false).filter(value -> "SUCCESS".equals(value.path("status").asText()))
                        .sorted(java.util.Comparator.comparingInt((JsonNode value) -> value.path("attemptNo").intValue()).reversed()).toList();
                var apiTargets = java.util.stream.StreamSupport.stream(attempts.getFirst().path("targets").spliterator(), false).filter(value -> "API_RPC".equals(value.path("engineRole").asText())).toList();
                if (apiTargets.size() != 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                String instance = apiTargets.getFirst().path("instanceId").asText(); var draft = owner.request("GET", groupPath + "/draft", reference, null, null, 200);
                var rejected = publishOwnedGatewayActivationFault(configOwner, configReference, evidence, verifiedProfile(root, manifest, digest, "biz"), scope, groupId,
                        receipt.path("operationId").asText(), active, fault, timeout);
                JsonNode observed = null; long deadline = System.nanoTime() + timeout.toNanos();
                while (System.nanoTime() < deadline) {
                    observed = owner.request("GET", groupPath + "/runtime-consistency", reference, null, null, 200);
                    if (gatewayConfigFailurePreservesTheAppliedRules(active, observed, instance, Instant.now())) { break; }
                    Thread.sleep(200);
                }
                if (observed == null || !gatewayConfigFailurePreservesTheAppliedRules(active, observed, instance, Instant.now())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
                var denied = gateway.request("GET", candidatePath, reference, null, null, 404);
                if (!"YUHENG_ROUTE_NOT_FOUND".equals(denied.path("code").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
                if (!before.equals(after) || !draft.equals(owner.request("GET", groupPath + "/draft", reference, null, null, 200))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
                var recovered = owner.request("POST", groupPath + "/releases", reference, mapper.createObjectNode().put("expectedDraftRevision", transactionProofLong(draft, "revision"))
                        .put("changeReason", "Task restore original scope integrity"), null, 200);
                if (!groupId.equals(recovered.path("gatewayGroupId").asText()) || recovered.path("releaseId").equals(goodRelease.path("releaseId"))) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
                var recoveryExpectation = ((com.fasterxml.jackson.databind.node.ObjectNode) receipt).deepCopy().put("releaseId", recovered.path("releaseId").asText())
                        .put("ruleContentSha256", recovered.path("structuredDiff").path("ruleContentSha256").asText());
                var next = verifyGatewayOwnerPublication(owner, reference, recoveryExpectation, scope, configuration, timeout);
                var item = faults.addObject().put("fault", fault).put("rejectionOwner", "TIANSHU_CONFIG").put("recoveredReleaseId", recovered.path("releaseId").asText())
                        .put("recoveredReleaseSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(recovered))).put("unactivatedRouteStatus", 404).put("deniedBusinessInvocations", 0).put("recoveryStatus", 200);
                item.set("publication", rejected); item.set("beforeProjection", active); item.set("rejectedProjection", observed); item.set("recoveredProjection", next);
                item.set("beforeRejectedRoute", before); item.set("afterRejectedRoute", after); item.set("beforeRecoveryRead", after);
                before = after; gateway.request("GET", path, reference, null, null, 200);
                after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null); requireOneBusinessInvocation(before, after);
                item.set("afterRecoveryRead", after); active = next;
            }
        }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "biz", "mcp", "tianshu"));
        output.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-biz-gateway-policy-rejection.json"), output);
    }

    private static long replaceOwnedGatewayPolicyAndRoute(NativeResourceHttpProbe owner, FormalReferenceDTO reference, String groupId, String operationId,
            java.util.Map<String, Object> configuration, String path) throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); String base = "/api/v1/yuheng/admin/yuheng-groups/" + groupId + "/draft";
        var draft = owner.request("GET", base, reference, null, null, 200);
        if (!draft.path("routes").isArray() || draft.path("routes").size() != 1 || !draft.path("policies").isArray() || draft.path("policies").size() != 1
                || !"it-read-security".equals(draft.path("policies").get(0).path("policyId").asText())
                || !"it-protected-read".equals(draft.path("routes").get(0).path("routeId").asText()) || !operationId.equals(draft.path("routes").get(0).path("operationId").asText())) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
        long revision = transactionProofLong(draft, "revision");
        var policy = mapper.createObjectNode().put("policyType", "SECURITY").put("policyScope", "OPERATION").put("enabled", true).put("expectedRevision", revision)
                .put("idempotencyKey", "it-scope-policy-" + UUID.randomUUID()).put("changeReason", "Task required scope verification"); policy.set("content", mapper.valueToTree(configuration));
        var changed = owner.request("PUT", base + "/policies/it-read-security", reference, policy, null, 200);
        if (transactionProofLong(changed, "revision") != Math.addExact(revision, 1) || !"it-read-security".equals(changed.path("resourceId").asText())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        revision = transactionProofLong(changed, "revision");
        var route = mapper.createObjectNode().put("operationId", operationId).put("enabled", true).put("expectedRevision", revision)
                .put("idempotencyKey", "it-scope-route-" + UUID.randomUUID()).put("changeReason", "Task required scope verification"); route.set("content", mapper.valueToTree(gatewayReadRouteContent(path)));
        changed = owner.request("PUT", base + "/routes/it-protected-read", reference, route, null, 200);
        if (transactionProofLong(changed, "revision") != Math.addExact(revision, 1) || !"it-protected-read".equals(changed.path("resourceId").asText())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        return transactionProofLong(changed, "revision");
    }

    static String mutateOwnedGatewayActivation(String json, String groupId, String releaseId, String operationId, GatewayTaskScopeVO scope, String fault) throws Exception {
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > 128 * 1024 || !java.util.Set.of("TAMPERED", "UNSIGNED").contains(fault)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var mapper = UnifiedIdentityLiveClient.strictJson(); var codec = new top.egon.cola.component.yuheng.runtime.rule.adapter.json.GatewayRuleJsonCodec();
        var activation = codec.readActivation(json);
        if (activation.mode() != top.egon.cola.component.yuheng.contract.rule.GatewayRuleActivationMode.INLINE || !releaseId.equals(activation.releaseId())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        byte[] original = activation.inlineSnapshot().getBytes(StandardCharsets.UTF_8); var snapshot = codec.readSnapshot(original); codec.verify(snapshot);
        if (activation.totalSize() != original.length || !activation.ruleContentSha256().equals(snapshot.ruleContentSha256()) || !activation.artifactSha256().equals(snapshot.artifactSha256())
                || !releaseId.equals(snapshot.releaseId()) || !groupId.equals(snapshot.content().gatewayGroupId()) || !scope.groupCode().equals(snapshot.content().gatewayGroupCode())
                || !scope.env().equals(snapshot.content().env()) || !scope.namespace().equals(snapshot.content().namespace())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(original); var content = tree.path("content");
        if (!content.path("securityPolicies").isArray() || content.path("securityPolicies").size() != 1 || !content.path("routes").isArray() || content.path("routes").size() != 1) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var policy = content.path("securityPolicies").get(0); var route = content.path("routes").get(0);
        if (!"it-read-security".equals(policy.path("policyId").asText()) || !mapper.valueToTree(gatewayProtectedReadPolicy("yuheng:read", operationId)).equals(policy.path("configuration"))
                || !"it-protected-read".equals(route.path("routeId").asText()) || !operationId.equals(route.path("operationId").asText())
                || !"/api/v1/yuheng/admin/scopes".equals(route.path("pathPattern").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        ((com.fasterxml.jackson.databind.node.ObjectNode) route).put("pathPattern", "/api/v1/yuheng/admin/scopes/__xingyuan-required-scope");
        var configuration = (com.fasterxml.jackson.databind.node.ObjectNode) policy.path("configuration");
        if (fault.equals("TAMPERED")) { configuration.putArray("requiredOAuthScopes").add("yuheng:catalog:write"); }
        else { tree.remove(List.of("ruleContentSha256", "artifactSha256")); }
        var changed = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
        String inline = mapper.writeValueAsString(tree);
        changed.put("inlineSnapshot", inline).put("totalSize", inline.getBytes(StandardCharsets.UTF_8).length);
        // Retain the original digest for tampering; an unsigned activation has neither original digest.
        if (fault.equals("UNSIGNED")) { changed.remove(List.of("ruleContentSha256", "artifactSha256")); }
        return mapper.writeValueAsString(changed);
    }

    @Test
    void ruleScopeMutationsRetainTheOriginalDigestOrRemoveItAndNeverCreateASignedReplacement() throws Exception {
        var scope = new GatewayTaskScopeVO("xingyuan-it-" + UUID.randomUUID(), "local", "controlled");
        var policy = new top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy("it-read-security", "SECURITY", "OPERATION", gatewayProtectedReadPolicy("yuheng:read", "125"));
        var route = new top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeRoute("it-protected-read", "125", "*", "GET", "/api/v1/yuheng/admin/scopes",
                java.util.Set.of(top.egon.cola.component.yuheng.contract.protocol.AccessZone.PUBLIC), 0, true);
        var content = new top.egon.cola.component.yuheng.contract.rule.GatewayRuleContent("123", scope.groupCode(), scope.env(), scope.namespace(), List.of(), List.of(route),
                List.of(), List.of(), List.of(policy), List.of(), List.of(), top.egon.cola.component.yuheng.contract.mcp.rule.McpRuleContent.empty());
        var canonical = new top.egon.cola.component.yuheng.admin.rule.service.GatewayRuleCanonicalizer();
        var snapshot = canonical.snapshot("124", Instant.parse("2026-10-15T00:00:00Z"), content);
        var codec = new top.egon.cola.component.yuheng.runtime.rule.adapter.json.GatewayRuleJsonCodec(); byte[] original = codec.write(snapshot);
        var activation = new top.egon.cola.component.yuheng.contract.rule.GatewayRuleActivation("v1", "124", top.egon.cola.component.yuheng.contract.rule.GatewayRuleActivationMode.INLINE,
                "v1", original.length, snapshot.ruleContentSha256(), snapshot.artifactSha256(), new String(original, StandardCharsets.UTF_8), List.of());
        String json = new String(codec.write(activation), StandardCharsets.UTF_8);
        var changed = codec.readActivation(mutateOwnedGatewayActivation(json, "123", "124", "125", scope, "TAMPERED"));
        assertThat(changed.artifactSha256()).isEqualTo(activation.artifactSha256()); assertThat(changed.ruleContentSha256()).isEqualTo(activation.ruleContentSha256());
        assertThat(changed.totalSize()).isEqualTo(changed.inlineSnapshot().getBytes(StandardCharsets.UTF_8).length);
        var invalid = codec.readSnapshot(changed.inlineSnapshot().getBytes(StandardCharsets.UTF_8));
        assertThat(invalid.content().securityPolicies().getFirst().configuration().get("requiredOAuthScopes")).isEqualTo(List.of("yuheng:catalog:write"));
        assertThatThrownBy(() -> codec.verify(invalid)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CHECKSUM_MISMATCH");
        String unsigned = mutateOwnedGatewayActivation(json, "123", "124", "125", scope, "UNSIGNED");
        assertThatThrownBy(() -> codec.readActivation(unsigned)).isInstanceOf(IllegalArgumentException.class);
        var unsignedTree = UnifiedIdentityLiveClient.strictJson().readTree(unsigned);
        assertThat(unsignedTree.has("artifactSha256")).isFalse();
        assertThat(UnifiedIdentityLiveClient.strictJson().readTree(unsignedTree.path("inlineSnapshot").asText()).path("content").path("securityPolicies").get(0)
                .path("configuration").path("requiredOAuthScopes")).isEqualTo(UnifiedIdentityLiveClient.strictJson().valueToTree(List.of("yuheng:read")));
        for (String fault : List.of("group", "release", "operation", "mode")) {
            assertThatThrownBy(() -> mutateOwnedGatewayActivation(json, fault.equals("group") ? "foreign" : "123", fault.equals("release") ? "foreign" : "124",
                    fault.equals("operation") ? "foreign" : "125", scope, fault.equals("mode") ? "UNKNOWN" : "TAMPERED")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
    }

    static JsonNode tianshuOwnerData(JsonNode response) {
        if (!response.path("success").isBoolean() || !response.path("success").booleanValue() || !response.path("code").isInt()
                || response.path("code").intValue() != top.egon.cola.component.common.core.enums.ResultCode.SUCCESS.getCode() || response.path("data").isMissingNode() || response.path("data").isNull()) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        return response.path("data");
    }

    static void requireTianshuGatewayNack(JsonNode data, String changeId, long expectedVersion, String checksum) {
        if (!changeId.equals(data.path("changeId").asText()) || !"FAILED".equals(data.path("status").asText()) || !data.path("targetVersion").isIntegralNumber()
                || !data.path("targetVersion").canConvertToLong() || data.path("targetVersion").longValue() != expectedVersion
                || !checksum.matches("[0-9a-f]{64}") || !checksum.equals(data.path("resourceChecksum").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (var expected : java.util.Map.of("targetCount", 1, "ackCount", 0, "failedCount", 1, "ignoredCount", 0, "timeoutCount", 0).entrySet()) {
            if (!data.path(expected.getKey()).isInt() || data.path(expected.getKey()).intValue() != expected.getValue()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
    }

    static boolean gatewayConfigFailurePreservesTheAppliedRules(JsonNode before, JsonNode after, String apiInstance, Instant now) {
        if (!before.path("consistent").isBoolean() || !before.path("consistent").booleanValue() || !"SUCCESS".equals(before.path("targetReleaseStatus").asText())
                || !after.path("consistent").isBoolean() || after.path("consistent").booleanValue() || !"SUCCESS".equals(after.path("targetReleaseStatus").asText())
                || !before.path("targetReleaseId").equals(after.path("targetReleaseId")) || !after.path("readyEngineNodeCount").isInt() || after.path("readyEngineNodeCount").intValue() != 1) { return false; }
        return gatewayActiveRulesUnchanged(gatewayBaselineNodes(before), before, after, java.util.Set.of(apiInstance), now);
    }

    static JsonNode ownedGatewayConfig(JsonNode rows, top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties expected) {
        if (!rows.isArray() || rows.size() > 64 || !"local".equals(expected.getEnv())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var matches = java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(value -> expected.getBizCode().equals(value.path("bizCode").asText()) && expected.getAppCode().equals(value.path("appCode").asText())
                        && expected.getEnv().equals(value.path("env").asText())
                        && top.egon.cola.component.yuheng.admin.rule.service.GatewayDdcYamlDocument.RESOURCE_NAME.equals(value.path("resourceName").asText())).toList();
        if (matches.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var value = matches.getFirst();
        if (!value.path("id").isTextual() || !value.path("id").asText().matches("[1-9][0-9]{0,18}") || !value.path("format").asText().equals("YAML")
                || !value.path("enabled").isBoolean() || !value.path("enabled").booleanValue() || !value.path("deleted").isBoolean() || value.path("deleted").booleanValue()
                || !value.path("currentVersion").isIntegralNumber() || !value.path("currentVersion").canConvertToLong() || value.path("currentVersion").longValue() <= 0
                || !value.path("content").isTextual() || value.path("content").asText().getBytes(StandardCharsets.UTF_8).length > 128 * 1024) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return value;
    }

    static String ownedGatewayUpdateVersion(JsonNode page, String configId, long version, String original, String changed, String fault) {
        if (!page.path("success").isBoolean() || !page.path("success").booleanValue() || !page.path("code").isInt()
                || page.path("code").intValue() != top.egon.cola.component.common.core.enums.ResultCode.SUCCESS.getCode()
                || !page.path("records").isArray() || page.path("records").size() != 1 || !page.path("page").path("pageNo").isInt() || page.path("page").path("pageNo").intValue() != 1
                || !page.path("page").path("pageSize").isInt() || page.path("page").path("pageSize").intValue() != 1 || transactionProofLong(page.path("page"), "total") < 1) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var row = page.path("records").get(0);
        if (!row.path("id").isTextual() || !row.path("id").asText().matches("[1-9][0-9]{0,18}") || !configId.equals(row.path("configId").asText())
                || transactionProofLong(row, "version") != version || !"YAML".equals(row.path("format").asText()) || !"UPDATE".equals(row.path("changeType").asText())
                || !top.egon.cola.component.yuheng.admin.rule.service.GatewayDdcYamlDocument.RESOURCE_NAME.equals(row.path("resourceName").asText())
                || !("Task scope integrity " + fault).equals(row.path("changeReason").asText()) || !original.equals(row.path("oldContent").asText()) || !changed.equals(row.path("newContent").asText())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        return row.path("id").asText();
    }

    @Test
    void configFaultCanOnlySelectTheUniqueOwnerDocumentAndItsExactNewVersion() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var expected = new top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties();
        expected.setBizCode("owned"); expected.setAppCode("owned-gateway"); expected.setEnv("local");
        var row = mapper.createObjectNode().put("id", "123").put("bizCode", "owned").put("appCode", "owned-gateway").put("env", "local")
                .put("resourceName", top.egon.cola.component.yuheng.admin.rule.service.GatewayDdcYamlDocument.RESOURCE_NAME).put("format", "YAML")
                .put("enabled", true).put("deleted", false).put("currentVersion", 3).put("content", "owned: original\n");
        assertThat(ownedGatewayConfig(mapper.createArrayNode().add(row), expected)).isEqualTo(row);
        assertThatThrownBy(() -> ownedGatewayConfig(mapper.createArrayNode().add(row).add(row), expected)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> ownedGatewayConfig(mapper.createArrayNode().add(row.deepCopy().put("appCode", "foreign")), expected)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> ownedGatewayConfig(mapper.createArrayNode().add(row.deepCopy().put("deleted", true)), expected)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var version = new top.egon.cola.component.tianshu.admin.model.vo.DdcConfigVersionVO().setId("127").setConfigId("123").setVersion(4L)
                .setResourceName(top.egon.cola.component.yuheng.admin.rule.service.GatewayDdcYamlDocument.RESOURCE_NAME).setFormat("YAML").setChangeType("UPDATE")
                .setOldContent("owned: original\n").setNewContent("owned: changed\n").setChangeReason("Task scope integrity TAMPERED");
        var page = mapper.valueToTree(top.egon.cola.component.common.core.pojo.PageResultRecord.success(List.of(version), 8, 1, 1));
        assertThat(ownedGatewayUpdateVersion(page, "123", 4, "owned: original\n", "owned: changed\n", "TAMPERED")).isEqualTo("127");
        assertThatThrownBy(() -> ownedGatewayUpdateVersion(page, "foreign", 4, "owned: original\n", "owned: changed\n", "TAMPERED")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> ownedGatewayUpdateVersion(page, "123", 5, "owned: original\n", "owned: changed\n", "TAMPERED")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> ownedGatewayUpdateVersion(page, "123", 4, "owned: original\n", "foreign: content\n", "TAMPERED")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var duplicate = page.deepCopy(); ((com.fasterxml.jackson.databind.node.ArrayNode) duplicate.path("records")).add(page.path("records").get(0));
        assertThatThrownBy(() -> ownedGatewayUpdateVersion(duplicate, "123", 4, "owned: original\n", "owned: changed\n", "TAMPERED")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var response = mapper.valueToTree(top.egon.cola.component.common.core.pojo.ResultRecord.success(row));
        assertThat(tianshuOwnerData(response)).isEqualTo(row);
        assertThatThrownBy(() -> tianshuOwnerData(((com.fasterxml.jackson.databind.node.ObjectNode) response).deepCopy().put("success", false))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    private static JsonNode publishOwnedGatewayActivationFault(NativeResourceHttpProbe tianshu, FormalReferenceDTO reference, Path evidence, Properties profile,
            GatewayTaskScopeVO scope, String groupId, String operationId, JsonNode active, String fault, Duration timeout) throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var flat = new java.util.LinkedHashMap<String, Object>(); profile.forEach((key, value) -> flat.put(key.toString(), value)); profile.clear();
        requireEvidenceDirectory(tianshu.root, evidence);
        var manifest = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(tianshu.manifestFile, null, 128 * 1024));
        if (!"tianshu".equals(tianshu.role) || !("xingyuan-it-" + manifest.path("runId").asText()).equals(scope.groupCode())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var ddc = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(flat))
                .bind("egon.cola.component.tianshu", top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE")); flat.clear();
        if (!ddc.getBizCode().matches("[A-Za-z0-9._-]{1,128}") || !ddc.getAppCode().matches("[A-Za-z0-9._-]{1,128}") || !scope.env().equals(ddc.getEnv())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        String query = "/api/v1/tianshu/configs?bizCode=" + ddc.getBizCode() + "&env=local&appCode=" + ddc.getAppCode() + "&includeDeleted=false";
        var selected = ownedGatewayConfig(tianshuOwnerData(tianshu.request("GET", query, reference, null, null, 200)), ddc);
        String configId = selected.path("id").asText(), original = selected.path("content").asText(); long version = transactionProofLong(selected, "currentVersion");
        var yaml = new top.egon.cola.component.yuheng.admin.rule.service.GatewayDdcYamlDocument(); String key = top.egon.cola.component.yuheng.admin.rule.service.GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY;
        String activation = yaml.leafValue(original, key).orElseThrow(() -> new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"));
        var decoded = new top.egon.cola.component.yuheng.runtime.rule.adapter.json.GatewayRuleJsonCodec().readActivation(activation);
        for (var node : active.path("nodes")) { if (!decoded.artifactSha256().equals(node.path("activeRuleChecksum").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); } }
        String changed = yaml.putLeaf(original, key, mutateOwnedGatewayActivation(activation, groupId, active.path("targetReleaseId").asText(), operationId, scope, fault));
        if (changed.getBytes(StandardCharsets.UTF_8).length > 120 * 1024) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        String checksum = top.egon.cola.component.tianshu.format.DdcChecksum.resource(selected.path("resourceName").asText(), "YAML", changed);
        var update = mapper.createObjectNode().put("id", configId).put("content", changed).put("currentVersion", version).put("changeReason", "Task scope integrity " + fault);
        if (mapper.writeValueAsBytes(update).length > 120 * 1024) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        if (mapper.writeValueAsBytes(java.util.Map.of("oldContent", original, "newContent", changed)).length > 112 * 1024) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        long targetVersion = Math.addExact(version, 2);
        String prefix = "gateway-rule-config-fault-" + fault.toLowerCase(java.util.Locale.ROOT);
        Path intentFile = evidence.resolve(prefix + "-running.json");
        if (Files.exists(intentFile, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        var intent = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", tianshu.digest).put("status", "RUNNING")
                .put("fault", fault).put("configId", configId).put("expectedConfigVersion", version).put("expectedPublishVersion", targetVersion)
                .put("resourceChecksum", checksum).put("originalDocumentSha256", UnifiedIdentityLiveClient.sha256(original.getBytes(StandardCharsets.UTF_8)))
                .put("mutatedDocumentSha256", UnifiedIdentityLiveClient.sha256(changed.getBytes(StandardCharsets.UTF_8))).put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(tianshu.root, intentFile, intent);
        byte[] frozen = UnifiedIdentityLiveClient.protectedBytes(intentFile, evidence, 128 * 1024);
        var updated = tianshuOwnerData(tianshu.request("PUT", "/api/v1/tianshu/configs/" + configId, reference, update, null, 200));
        if (!configId.equals(updated.path("id").asText()) || transactionProofLong(updated, "currentVersion") != Math.addExact(version, 1) || !changed.equals(updated.path("content").asText())
                || !ddc.getBizCode().equals(updated.path("bizCode").asText()) || !ddc.getAppCode().equals(updated.path("appCode").asText()) || !scope.env().equals(updated.path("env").asText())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        if (!java.util.Arrays.equals(frozen, UnifiedIdentityLiveClient.protectedBytes(intentFile, evidence, 128 * 1024))) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        var sourceVersion = tianshu.request("GET", "/api/v1/tianshu/configs/" + configId + "/versions/page?pageNo=1&pageSize=1", reference, null, null, 200);
        String changeId = ownedGatewayUpdateVersion(sourceVersion, configId, Math.addExact(version, 1), original, changed, fault);
        // Correlate this publish with its exact owner-generated update version after checking that the key is unused.
        var absent = tianshu.request("GET", "/api/v1/tianshu/publish-tasks/" + changeId, reference, null, null, 200);
        if (!absent.path("success").isBoolean() || !absent.path("success").booleanValue() || !absent.path("code").isInt()
                || absent.path("code").intValue() != top.egon.cola.component.common.core.enums.ResultCode.SUCCESS.getCode() || !absent.path("data").isNull()) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        var publishing = intent.deepCopy().put("changeId", changeId).put("sourceVersionId", changeId)
                .put("sourceVersionSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(sourceVersion))).put("recordedAt", Instant.now().toString());
        Path publishingFile = evidence.resolve(prefix + "-publish-running.json"); XingyuanNativeAuthenticationIT.publishNewNativeEvidence(tianshu.root, publishingFile, publishing);
        byte[] frozenPublish = UnifiedIdentityLiveClient.protectedBytes(publishingFile, evidence, 128 * 1024);
        var publish = mapper.createObjectNode().put("changeId", changeId).put("content", changed).put("expectedVersion", transactionProofLong(updated, "currentVersion"))
                .put("timeoutMs", Math.max(1000, timeout.toMillis() / 2));
        var result = tianshuOwnerData(tianshu.request("POST", "/api/v1/tianshu/configs/" + configId + "/publish", reference, publish, null, 200));
        requireTianshuGatewayNack(result, changeId, targetVersion, checksum);
        var task = tianshuOwnerData(tianshu.request("GET", "/api/v1/tianshu/publish-tasks/" + changeId, reference, null, null, 200));
        requireTianshuGatewayNack(task, changeId, targetVersion, checksum);
        if (!configId.equals(task.path("configId").asText()) || !ddc.getBizCode().equals(task.path("bizCode").asText()) || !ddc.getAppCode().equals(task.path("appCode").asText())
                || !scope.env().equals(task.path("env").asText()) || !selected.path("resourceName").equals(task.path("resourceName")) || !"ACK".equals(task.path("failureStage").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        if (!java.util.Arrays.equals(frozen, UnifiedIdentityLiveClient.protectedBytes(intentFile, evidence, 128 * 1024))
                || !java.util.Arrays.equals(frozenPublish, UnifiedIdentityLiveClient.protectedBytes(publishingFile, evidence, 128 * 1024))) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(tianshu.root, evidence.resolve(prefix + "-confirmed.json"), publishing.deepCopy().put("status", "NACK_CONFIRMED")
                .put("publicationSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(result))).put("taskSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(task)))
                .put("recordedAt", Instant.now().toString()));
        return mapper.createObjectNode().put("changeId", changeId).put("configId", configId).put("status", "FAILED").put("targetVersion", targetVersion).put("resourceChecksum", checksum)
                .put("sourceVersionSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(sourceVersion)))
                .put("originalDocumentSha256", UnifiedIdentityLiveClient.sha256(original.getBytes(StandardCharsets.UTF_8))).put("mutatedDocumentSha256", UnifiedIdentityLiveClient.sha256(changed.getBytes(StandardCharsets.UTF_8)))
                .put("publicationSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(result))).put("taskSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(task)));
    }

    static void requireGatewayPublicationReceipt(JsonNode value, JsonNode manifest, String digest, String inputHash) {
        var fields = new java.util.HashSet<String>(); value.fieldNames().forEachRemaining(fields::add);
        if (!fields.equals(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "inputSha256", "scope", "status", "gatewayGroupId", "operationId", "definitionId",
                "definitionSha256", "releaseId", "ruleContentSha256", "runtimeProjectionSha256", "recordedAt"))
                || !value.path("schemaVersion").isInt() || value.path("schemaVersion").intValue() != 1 || !manifest.path("runId").asText().equals(value.path("runId").asText())
                || !digest.equals(value.path("manifestSha256").asText()) || !inputHash.equals(value.path("inputSha256").asText())
                || !"ORIGINAL_TASK_GATEWAY_OWNER_PUBLICATION_ONLY".equals(value.path("scope").asText()) || !"COMMITTED".equals(value.path("status").asText())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        for (String field : List.of("gatewayGroupId", "operationId", "definitionId", "releaseId")) {
            if (!value.path(field).isTextual() || !value.path(field).textValue().matches("[A-Za-z0-9_-]{1,128}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        for (String field : List.of("ruleContentSha256", "runtimeProjectionSha256")) {
            if (!value.path(field).asText().matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        if (!value.path("definitionSha256").asText().matches("(?:sha256:)?[0-9a-f]{64}")
                || transactionProofInstant(value, "recordedAt").isAfter(Instant.now())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
    }

    private static JsonNode verifyGatewayOwnerPublication(NativeResourceHttpProbe api, FormalReferenceDTO reference, JsonNode receipt,
            GatewayTaskScopeVO scope, java.util.Map<String, Object> configuration, Duration timeout) throws Exception {
        return verifyGatewayOwnerPublication(api, reference, receipt, scope, configuration, timeout, null);
    }

    private static JsonNode verifyGatewayOwnerPublication(NativeResourceHttpProbe api, FormalReferenceDTO reference, JsonNode receipt,
            GatewayTaskScopeVO scope, java.util.Map<String, Object> configuration, Duration timeout, String mcpRunId) throws Exception {
        String base = "/api/v1/yuheng/admin", groupId = receipt.path("gatewayGroupId").asText(), releaseId = receipt.path("releaseId").asText();
        var groups = api.request("GET", base + "/yuheng-groups", reference, null, null, 200);
        if (!groups.isArray()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var selected = java.util.stream.StreamSupport.stream(groups.spliterator(), false).filter(value -> groupId.equals(value.path("id").asText())).toList();
        if (selected.size() != 1 || !scope.groupCode().equals(selected.getFirst().path("gatewayGroupCode").asText())
                || !scope.env().equals(selected.getFirst().path("env").asText()) || !scope.namespace().equals(selected.getFirst().path("namespace").asText())
                || !("XINGYUAN_TASK:" + receipt.path("runId").asText()).equals(selected.getFirst().path("description").asText())
                || !selected.getFirst().path("enabled").isBoolean() || !selected.getFirst().path("enabled").booleanValue()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var draft = api.request("GET", base + "/yuheng-groups/" + groupId + "/draft", reference, null, null, 200);
        if (!draft.path("routes").isArray() || draft.path("routes").size() != 1 || !draft.path("policies").isArray() || draft.path("policies").size() != (mcpRunId == null ? 1 : 2)
                || !releaseId.equals(draft.path("basedOnReleaseId").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var policy = draft.path("policies").get(0); var route = draft.path("routes").get(0);
        if (mcpRunId != null) {
            if (!mcpRunId.equals(receipt.path("runId").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var expectedPolicies = java.util.Map.of("it-read-security", UnifiedIdentityLiveClient.strictJson().valueToTree(configuration),
                    "it-mcp-security", UnifiedIdentityLiveClient.strictJson().valueToTree(mcpTaskReadPolicy(mcpRunId)));
            var seen = new java.util.HashSet<String>();
            for (var candidate : draft.path("policies")) {
                String id = candidate.path("policyId").asText();
                if (!seen.add(id) || !expectedPolicies.containsKey(id) || !expectedPolicies.get(id).equals(candidate.path("content"))
                        || !"SECURITY".equals(candidate.path("policyType").asText()) || !"OPERATION".equals(candidate.path("policyScope").asText())
                        || !candidate.path("enabled").isBoolean() || !candidate.path("enabled").booleanValue()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                if (id.equals("it-read-security")) { policy = candidate; }
            }
            if (!seen.equals(expectedPolicies.keySet())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        if (!"it-read-security".equals(policy.path("policyId").asText()) || !"SECURITY".equals(policy.path("policyType").asText())
                || !"OPERATION".equals(policy.path("policyScope").asText()) || !policy.path("enabled").isBoolean() || !policy.path("enabled").booleanValue()
                || !UnifiedIdentityLiveClient.strictJson().valueToTree(configuration).equals(policy.path("content"))
                || !"it-protected-read".equals(route.path("routeId").asText()) || !receipt.path("operationId").equals(route.path("operationId"))
                || !route.path("enabled").isBoolean() || !route.path("enabled").booleanValue()
                || !UnifiedIdentityLiveClient.strictJson().valueToTree(gatewayReadRouteContent("/api/v1/yuheng/admin/scopes")).equals(route.path("content"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var release = api.request("GET", base + "/releases/" + releaseId, reference, null, null, 200);
        if (!groupId.equals(release.path("gatewayGroupId").asText()) || !releaseId.equals(release.path("releaseId").asText())
                || !"SUCCESS".equals(release.path("status").asText()) || !release.path("partialApplied").isBoolean() || release.path("partialApplied").booleanValue()
                || !receipt.path("ruleContentSha256").equals(release.path("structuredDiff").path("ruleContentSha256"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            var projection = api.request("GET", base + "/yuheng-groups/" + groupId + "/runtime-consistency", reference, null, null, 200);
            if (gatewayProjectionMatches(projection, release, Instant.now())) { return projection; }
            Thread.sleep(200);
        }
        throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
    }

    private static JsonNode verifyTaskMcpPublication(NativeResourceHttpProbe owner, FormalReferenceDTO reference, JsonNode receipt,
            GatewayTaskScopeVO scope, Duration timeout) throws Exception {
        String runId = receipt.path("runId").asText(), groupId = receipt.path("gatewayGroupId").asText(), serverId = receipt.path("serverId").asText();
        if (!mcpTaskServerCode(runId).equals(receipt.path("serverCode").asText()) || !reference.getResourceUri().toASCIIString().equals(receipt.path("resourceUri").asText())
                || !"yuheng-admin".equals(receipt.path("rbacApplicationCode").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var projection = verifyGatewayOwnerPublication(owner, reference, receipt, scope, gatewayProtectedReadPolicy("yuheng:read", receipt.path("operationId").asText()), timeout, runId);
        String serverPath = "/api/v1/yuheng/admin/mcp/servers";
        var servers = owner.request("GET", serverPath + "?gatewayGroupId=" + groupId, reference, null, null, 200);
        var resources = owner.request("GET", serverPath + '/' + serverId + "/resources?gatewayGroupId=" + groupId, reference, null, null, 200);
        if (!servers.isArray() || servers.size() != 1 || !serverId.equals(servers.get(0).path("id").asText()) || !groupId.equals(servers.get(0).path("gatewayGroupId").asText())
                || !resources.isArray() || resources.size() != 1 || !receipt.path("resourceId").equals(resources.get(0).path("id"))
                || !serverId.equals(resources.get(0).path("serverId").asText()) || !groupId.equals(resources.get(0).path("gatewayGroupId").asText())
                || !"RESOURCE".equals(resources.get(0).path("kind").asText()) || !"scopes".equals(resources.get(0).path("name").asText())
                || !resources.get(0).path("enabled").isBoolean() || !resources.get(0).path("enabled").booleanValue()
                || !UnifiedIdentityLiveClient.strictJson().valueToTree(mcpTaskResourceContent(runId, receipt.path("operationId").asText())).equals(resources.get(0).path("content"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var preview = owner.request("GET", serverPath + '/' + serverId + "/capability-preview", reference, null, null, 200);
        requireTaskMcpPreview(preview, runId, serverId, receipt.path("resourceId").asText(), receipt.path("operationId").asText(), reference.getResourceUri());
        if (!UnifiedIdentityLiveClient.sha256(UnifiedIdentityLiveClient.strictJson().writeValueAsBytes(preview)).equals(receipt.path("previewSha256").asText())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return projection;
    }

    static boolean gatewayProjectionMatches(JsonNode value, JsonNode release, Instant now) {
        String releaseId = release.path("releaseId").asText();
        if (!value.path("stale").isBoolean() || value.path("stale").booleanValue() || !value.path("consistent").isBoolean() || !value.path("consistent").booleanValue()
                || !releaseId.equals(value.path("targetReleaseId").asText()) || !"SUCCESS".equals(value.path("targetReleaseStatus").asText())
                || !"TIANSHU_CONFIG_CLIENT".equals(value.path("source").asText()) || !"SUCCESS".equals(release.path("status").asText())
                || !value.path("engineNodeCount").isIntegralNumber() || value.path("engineNodeCount").longValue() != 2
                || !value.path("readyEngineNodeCount").isIntegralNumber() || value.path("readyEngineNodeCount").longValue() != 2
                || !value.path("nodes").isArray() || value.path("nodes").size() != 2) { return false; }
        var attempts = java.util.stream.StreamSupport.stream(release.path("attempts").spliterator(), false)
                .filter(attempt -> "SUCCESS".equals(attempt.path("status").asText()) && attempt.path("attemptNo").isIntegralNumber()
                        && attempt.path("attemptNo").canConvertToInt() && attempt.path("attemptNo").intValue() > 0)
                .sorted(java.util.Comparator.comparingInt((JsonNode attempt) -> attempt.path("attemptNo").intValue()).reversed()).toList();
        if (attempts.isEmpty() || !attempts.getFirst().path("targets").isArray() || attempts.getFirst().path("targets").size() != 2) { return false; }
        var expectedAcks = new java.util.LinkedHashMap<String, JsonNode>(); var roles = new java.util.HashSet<String>();
        for (var target : attempts.getFirst().path("targets")) {
            if (!"SUCCESS".equals(target.path("status").asText()) || !roles.add(target.path("engineRole").asText())
                    || !target.path("appliedArtifactSha256").asText().matches("[0-9a-f]{64}") || !target.path("appliedVersion").isIntegralNumber()
                    || !target.path("appliedVersion").canConvertToLong() || target.path("appliedVersion").longValue() <= 0
                    || !target.path("instanceId").isTextual() || target.path("instanceId").asText().isBlank()
                    || expectedAcks.putIfAbsent(target.path("instanceId").asText(), target) != null) { return false; }
        }
        if (!roles.equals(java.util.Set.of("API_RPC", "MCP"))) { return false; }
        var instances = new java.util.HashSet<String>(); var leases = new java.util.HashSet<String>();
        for (var node : value.path("nodes")) {
            var expected = expectedAcks.get(node.path("instanceId").asText());
            if (!node.path("instanceId").isTextual() || node.path("instanceId").asText().isBlank() || !instances.add(node.path("instanceId").asText())
                    || !node.path("leaseId").isTextual() || node.path("leaseId").asText().isBlank() || !leases.add(node.path("leaseId").asText())
                    || !releaseId.equals(node.path("activeReleaseId").asText()) || !"CONSISTENT".equals(node.path("status").asText())
                    || !"ACK_SUCCESS".equals(node.path("lastApplyStatus").asText()) || !node.path("activeRuleChecksum").asText().matches("[0-9a-f]{64}")
                    || !node.path("activeRuleVersion").isIntegralNumber() || !node.path("activeRuleVersion").canConvertToLong() || node.path("activeRuleVersion").longValue() <= 0
                    || expected == null || !expected.path("appliedArtifactSha256").equals(node.path("activeRuleChecksum"))
                    || node.path("activeRuleVersion").longValue() < expected.path("appliedVersion").longValue()) { return false; }
            Instant ack = transactionProofInstant(node, "lastAckAt"); if (ack.isAfter(now)) { return false; }
        }
        Instant observed = transactionProofInstant(value, "observedAt");
        return !observed.isAfter(now) && !observed.isBefore(now.minusSeconds(30));
    }

    static boolean gatewayRejectedPublicationPreservesTheAppliedRules(JsonNode release, JsonNode before, JsonNode after, Instant now) {
        if (!"FAILED".equals(release.path("status").asText()) || !release.path("partialApplied").isBoolean()
                || !release.path("releaseId").asText().matches("[A-Za-z0-9_-]{1,128}") || release.path("releaseId").equals(before.path("targetReleaseId"))
                || !before.path("consistent").isBoolean() || !before.path("consistent").booleanValue() || !"SUCCESS".equals(before.path("targetReleaseStatus").asText())
                || !after.path("consistent").isBoolean() || after.path("consistent").booleanValue() || !"FAILED".equals(after.path("targetReleaseStatus").asText())
                || !release.path("releaseId").equals(after.path("targetReleaseId")) || !after.path("readyEngineNodeCount").isIntegralNumber() || after.path("readyEngineNodeCount").longValue() != 0) { return false; }
        var baselines = gatewayBaselineNodes(before); if (baselines.size() != 2) { return false; }
        var attempts = java.util.stream.StreamSupport.stream(release.path("attempts").spliterator(), false)
                .filter(value -> "FAILED".equals(value.path("status").asText()) && value.path("attemptNo").isIntegralNumber() && value.path("attemptNo").canConvertToInt() && value.path("attemptNo").intValue() > 0)
                .sorted(java.util.Comparator.comparingInt((JsonNode value) -> value.path("attemptNo").intValue()).reversed()).toList();
        if (attempts.isEmpty() || !attempts.getFirst().path("targets").isArray() || attempts.getFirst().path("targets").size() != 1) { return false; }
        var targets = new java.util.HashSet<String>(); var roles = new java.util.HashSet<String>();
        for (var target : attempts.getFirst().path("targets")) {
            if (!"FAILED".equals(target.path("status").asText()) || !targets.add(target.path("instanceId").asText()) || !baselines.containsKey(target.path("instanceId").asText())
                    || !roles.add(target.path("engineRole").asText()) || !"TIANSHU_TARGET_ERROR".equals(target.path("errorCode").asText())
                    || !baselines.get(target.path("instanceId").asText()).path("leaseId").equals(target.path("leaseId"))
                    || !(target.path("appliedArtifactSha256").isNull() || target.path("appliedArtifactSha256").isMissingNode())) { return false; }
        }
        if (!roles.equals(java.util.Set.of("API_RPC"))) { return false; }
        return gatewayActiveRulesUnchanged(baselines, before, after, targets, now);
    }

    private static java.util.Map<String, JsonNode> gatewayBaselineNodes(JsonNode before) {
        if (!before.path("nodes").isArray() || before.path("nodes").size() != 2) { return java.util.Map.of(); }
        var baselines = new java.util.LinkedHashMap<String, JsonNode>();
        for (var node : before.path("nodes")) {
            if (!node.path("instanceId").isTextual() || node.path("instanceId").asText().isBlank() || baselines.putIfAbsent(node.path("instanceId").asText(), node) != null
                    || !node.path("leaseId").isTextual() || node.path("leaseId").asText().isBlank() || !"ACK_SUCCESS".equals(node.path("lastApplyStatus").asText())
                    || !before.path("targetReleaseId").equals(node.path("activeReleaseId")) || !node.path("activeRuleVersion").isIntegralNumber()
                    || !node.path("activeRuleVersion").canConvertToLong() || node.path("activeRuleVersion").longValue() <= 0 || !node.path("activeRuleChecksum").asText().matches("[0-9a-f]{64}")) { return java.util.Map.of(); }
        }
        return baselines;
    }

    private static boolean gatewayActiveRulesUnchanged(java.util.Map<String, JsonNode> baselines, JsonNode before, JsonNode after,
            java.util.Set<String> targets, Instant now) {
        if (baselines.size() != 2 || targets.size() != 1 || !baselines.keySet().containsAll(targets)) { return false; }
        for (var value : List.of(before, after)) {
            if (!"TIANSHU_CONFIG_CLIENT".equals(value.path("source").asText()) || !value.path("stale").isBoolean() || value.path("stale").booleanValue()
                    || !value.path("engineNodeCount").isInt() || value.path("engineNodeCount").intValue() != 2 || !value.path("nodes").isArray() || value.path("nodes").size() != 2) { return false; }
        }
        var seen = new java.util.HashSet<String>();
        for (var node : after.path("nodes")) {
            String instance = node.path("instanceId").asText(); var prior = baselines.get(instance);
            if (prior == null || !seen.add(instance) || !(targets.contains(instance) ? "FAILED" : "ACK_SUCCESS").equals(node.path("lastApplyStatus").asText())) { return false; }
            for (String field : List.of("leaseId", "activeReleaseId", "activeRuleVersion", "activeRuleChecksum")) { if (!prior.path(field).equals(node.path(field))) { return false; } }
            Instant ack = transactionProofInstant(node, "lastAckAt");
            if (ack.isBefore(transactionProofInstant(prior, "lastAckAt")) || ack.isAfter(now)) { return false; }
        }
        Instant observed = transactionProofInstant(after, "observedAt");
        return !observed.isAfter(now) && !observed.isBefore(now.minusSeconds(30)) && !observed.isBefore(transactionProofInstant(before, "observedAt"));
    }

    @Test
    void rejectedGatewayReleaseRequiresTheOriginalFirstRoleNackAndUnchangedActiveRules() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.parse("2026-10-15T00:00:30Z");
        var before = mapper.createObjectNode().put("targetReleaseId", "good-release").put("targetReleaseStatus", "SUCCESS").put("consistent", true)
                .put("source", "TIANSHU_CONFIG_CLIENT").put("stale", false).put("engineNodeCount", 2).put("readyEngineNodeCount", 2).put("observedAt", now.minusSeconds(2).toString());
        var nodes = before.putArray("nodes");
        for (String instance : List.of("api-node", "mcp-node")) {
            nodes.addObject().put("instanceId", instance).put("leaseId", instance + "-lease").put("activeReleaseId", "good-release").put("activeRuleVersion", 3)
                    .put("activeRuleChecksum", "a".repeat(64)).put("lastApplyStatus", "ACK_SUCCESS").put("lastAckAt", now.minusSeconds(2).toString());
        }
        var after = before.deepCopy().put("targetReleaseId", "bad-release").put("targetReleaseStatus", "FAILED").put("consistent", false).put("readyEngineNodeCount", 0).put("observedAt", now.toString());
        ((com.fasterxml.jackson.databind.node.ObjectNode) after.path("nodes").get(0)).put("lastApplyStatus", "FAILED").put("lastAckAt", now.toString());
        var release = mapper.createObjectNode().put("releaseId", "bad-release").put("status", "FAILED").put("partialApplied", false);
        var acknowledgements = release.putArray("attempts").addObject().put("attemptNo", 1).put("status", "FAILED").putArray("targets");
        for (String role : List.of("API_RPC")) {
            String instance = role.equals("MCP") ? "mcp-node" : "api-node";
            acknowledgements.addObject().put("instanceId", instance).put("leaseId", instance + "-lease").put("engineRole", role).put("status", "FAILED").put("errorCode", "TIANSHU_TARGET_ERROR").putNull("appliedArtifactSha256");
        }
        assertThat(gatewayRejectedPublicationPreservesTheAppliedRules(release, before, after, now)).isTrue();
        var configProjection = after.deepCopy().put("targetReleaseId", "good-release").put("targetReleaseStatus", "SUCCESS").put("readyEngineNodeCount", 1);
        assertThat(gatewayConfigFailurePreservesTheAppliedRules(before, configProjection, "api-node", now)).isTrue();
        assertThat(gatewayConfigFailurePreservesTheAppliedRules(before, configProjection, "mcp-node", now)).isFalse();
        var result = mapper.createObjectNode().put("changeId", "127").put("status", "FAILED").put("targetVersion", 9).put("resourceChecksum", "c".repeat(64))
                .put("targetCount", 1).put("ackCount", 0).put("failedCount", 1).put("ignoredCount", 0).put("timeoutCount", 0);
        assertThatCode(() -> requireTianshuGatewayNack(result, "127", 9, "c".repeat(64))).doesNotThrowAnyException();
        for (String field : List.of("targetCount", "ackCount", "failedCount", "ignoredCount", "timeoutCount", "targetVersion", "resourceChecksum", "changeId", "status")) {
            var invalid = result.deepCopy(); invalid.remove(field);
            assertThatThrownBy(() -> requireTianshuGatewayNack(invalid, "127", 9, "c".repeat(64))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        // Chunk publication can be partial while neither engine has activated the rejected rule.
        assertThat(gatewayRejectedPublicationPreservesTheAppliedRules(release.deepCopy().put("partialApplied", true), before, after, now)).isTrue();
        for (String fault : List.of("partial", "timeout", "ack", "version", "hash", "lease", "success", "stale", "role", "futureAck")) {
            var bad = release.deepCopy(); var projection = after.deepCopy(); var first = (com.fasterxml.jackson.databind.node.ObjectNode) projection.path("nodes").get(0);
            if (fault.equals("partial")) { bad.put("partialApplied", "true"); }
            else if (fault.equals("timeout")) { bad.put("status", "TIMEOUT"); }
            else if (fault.equals("ack")) { ((com.fasterxml.jackson.databind.node.ObjectNode) bad.path("attempts").get(0).path("targets").get(0)).put("status", "SUCCESS"); }
            else if (fault.equals("version")) { first.put("activeRuleVersion", 4); }
            else if (fault.equals("hash")) { first.put("activeRuleChecksum", "b".repeat(64)); }
            else if (fault.equals("lease")) { first.put("leaseId", "replacement-lease"); }
            else if (fault.equals("success")) { first.put("lastApplyStatus", "ACK_SUCCESS"); }
            else if (fault.equals("stale")) { projection.put("stale", true); }
            else if (fault.equals("futureAck")) { first.put("lastAckAt", now.plusSeconds(1).toString()); }
            else { ((com.fasterxml.jackson.databind.node.ObjectNode) bad.path("attempts").get(0).path("targets").get(0)).put("engineRole", "MCP"); }
            assertThat(gatewayRejectedPublicationPreservesTheAppliedRules(bad, before, projection, now)).as(fault).isFalse();
        }
    }

    @Test
    void gatewayProjectionMatchesOriginalPerRoleArtifactAcksRatherThanContentHashesOrOlderVersions() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.parse("2026-10-15T00:00:30Z");
        var release = mapper.createObjectNode().put("releaseId", "123").put("status", "SUCCESS");
        release.putObject("structuredDiff").put("ruleContentSha256", "a".repeat(64));
        var targets = release.putArray("attempts").addObject().put("status", "SUCCESS").put("attemptNo", 1).putArray("targets");
        var projection = mapper.createObjectNode().put("stale", false).put("consistent", true).put("targetReleaseId", "123").put("targetReleaseStatus", "SUCCESS")
                .put("source", "TIANSHU_CONFIG_CLIENT").put("engineNodeCount", 2).put("readyEngineNodeCount", 2).put("observedAt", now.minusSeconds(1).toString());
        var nodes = projection.putArray("nodes");
        for (String role : List.of("API_RPC", "MCP")) {
            String instance = role.equals("MCP") ? "mcp" : "biz", checksum = (role.equals("MCP") ? "c" : "b").repeat(64); int version = role.equals("MCP") ? 3 : 2;
            targets.addObject().put("engineRole", role).put("instanceId", instance).put("leaseId", instance + "-lease").put("status", "SUCCESS").put("appliedVersion", version).put("appliedArtifactSha256", checksum);
            nodes.addObject().put("instanceId", instance).put("leaseId", instance + "-lease").put("activeReleaseId", "123").put("status", "CONSISTENT")
                    .put("lastApplyStatus", "ACK_SUCCESS").put("activeRuleChecksum", checksum).put("activeRuleVersion", version).put("lastAckAt", now.minusSeconds(2).toString());
        }
        assertThat(gatewayProjectionMatches(projection, release, now)).isTrue();
        var wrongHash = projection.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) wrongHash.path("nodes").get(0)).put("activeRuleChecksum", "a".repeat(64));
        assertThat(gatewayProjectionMatches(wrongHash, release, now)).isFalse();
        var older = projection.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) older.path("nodes").get(0)).put("activeRuleVersion", 1);
        assertThat(gatewayProjectionMatches(older, release, now)).isFalse();
        var duplicate = projection.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) duplicate.path("nodes").get(1)).put("leaseId", "biz-lease");
        assertThat(gatewayProjectionMatches(duplicate, release, now)).isFalse();
        assertThat(gatewayProjectionMatches(projection.deepCopy().put("stale", true), release, now)).isFalse();
        assertThat(gatewayProjectionMatches(projection.deepCopy().put("observedAt", now.minusSeconds(31).toString()), release, now)).isFalse();
        var future = projection.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) future.path("nodes").get(0)).put("lastAckAt", now.plusSeconds(1).toString());
        assertThat(gatewayProjectionMatches(future, release, now)).isFalse();
    }

    static GatewayReadOperationVO gatewayReadOperation(JsonNode application, JsonNode detail,
            top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties expected) {
        var operation = detail.path("operation"); var provider = operation.path("providerServiceIdentity");
        if (!application.path("id").asText().matches("[A-Za-z0-9_-]{1,128}") || !application.path("ddcMatched").isBoolean() || !application.path("ddcMatched").booleanValue()
                || !expected.getBizCode().equals(application.path("bizCode").asText()) || !expected.getAppCode().equals(application.path("applicationCode").asText())
                || !expected.getEnv().equals(application.path("env").asText()) || !expected.getNamespace().equals(application.path("namespace").asText())
                || !application.path("id").equals(operation.path("applicationId")) || !operation.path("id").asText().matches("[A-Za-z0-9_-]{1,128}")
                || !"HTTP".equals(operation.path("protocol").asText()) || !"GET /api/v1/yuheng/admin/scopes".equals(operation.path("methodIdentity").asText())
                || !"OPENAPI31".equals(operation.path("sourceType").asText()) || !"DISCOVERED".equals(operation.path("lifecycleStatus").asText())
                || !operation.path("externalAccessible").isBoolean() || !operation.path("externalAccessible").booleanValue()
                || !operation.path("currentDefinitionId").asText().matches("[A-Za-z0-9_-]{1,128}") || !detail.path("definitions").isArray()
                || !expected.getBizCode().equals(provider.path("bizCode").asText()) || !expected.getAppCode().equals(provider.path("appCode").asText())
                || !expected.getEnv().equals(provider.path("env").asText()) || !expected.getNamespace().equals(provider.path("namespace").asText())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var selected = java.util.stream.StreamSupport.stream(detail.path("definitions").spliterator(), false)
                .filter(value -> operation.path("currentDefinitionId").equals(value.path("id"))).toList();
        if (selected.size() != 1 || !operation.path("id").equals(selected.getFirst().path("operationId"))
                || !selected.getFirst().path("definitionSha256").asText().matches("(?:sha256:)?[0-9a-f]{64}")
                || transactionProofLong(selected.getFirst(), "definitionVersion") <= 0) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        for (String field : List.of("serviceName", "group", "version", "transport")) {
            if (!provider.path(field).isTextual() || !provider.path(field).textValue().matches("[A-Za-z0-9._:-]{1,128}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        return new GatewayReadOperationVO(application.path("id").asText(), operation.path("id").asText(), selected.getFirst().path("id").asText(),
                transactionProofLong(selected.getFirst(), "definitionVersion"), selected.getFirst().path("definitionSha256").asText(),
                new top.egon.cola.component.yuheng.contract.rule.GatewayProviderServiceRef(expected.getBizCode(), expected.getAppCode(), expected.getEnv(), expected.getNamespace(),
                        top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol.HTTP, provider.path("serviceName").asText(), provider.path("group").asText(),
                        provider.path("version").asText(), provider.path("transport").asText()));
    }

    @Test
    void gatewayReadSelectionRequiresTheActualExternalOpenApiDefinitionAndProviderTuple() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var expected = new top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties();
        expected.setBizCode("reviewed-biz"); expected.setAppCode("reviewed-app"); expected.setEnv("reviewed-env"); expected.setNamespace("reviewed-namespace");
        var application = mapper.createObjectNode().put("id", "123").put("ddcMatched", true).put("bizCode", expected.getBizCode()).put("applicationCode", expected.getAppCode())
                .put("env", expected.getEnv()).put("namespace", expected.getNamespace());
        var detail = mapper.createObjectNode(); var operation = detail.putObject("operation").put("id", "124").put("applicationId", "123").put("protocol", "HTTP")
                .put("methodIdentity", "GET /api/v1/yuheng/admin/scopes").put("sourceType", "OPENAPI31").put("lifecycleStatus", "DISCOVERED").put("externalAccessible", true).put("currentDefinitionId", "125");
        operation.putObject("providerServiceIdentity").put("bizCode", expected.getBizCode()).put("appCode", expected.getAppCode()).put("env", expected.getEnv()).put("namespace", expected.getNamespace())
                .put("serviceName", "reviewed-http").put("group", "default").put("version", "v1").put("transport", "http");
        detail.putArray("definitions").addObject().put("id", "125").put("operationId", "124").put("definitionVersion", 1).put("definitionSha256", "a".repeat(64));
        assertThat(gatewayReadOperation(application, detail, expected).operationId()).isEqualTo("124");
        for (String field : List.of("sourceType", "lifecycleStatus", "methodIdentity", "applicationId", "currentDefinitionId")) {
            var wrong = detail.deepCopy(); wrong.withObject("operation").put(field, "unreviewed");
            assertThatThrownBy(() -> gatewayReadOperation(application, wrong, expected)).isInstanceOf(IllegalArgumentException.class);
        }
        var internal = detail.deepCopy(); internal.withObject("operation").put("externalAccessible", false);
        assertThatThrownBy(() -> gatewayReadOperation(application, internal, expected)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var foreign = detail.deepCopy(); foreign.withObject("operation").withObject("providerServiceIdentity").put("appCode", "another-app");
        assertThatThrownBy(() -> gatewayReadOperation(application, foreign, expected)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var duplicate = detail.deepCopy(); duplicate.withArray("definitions").add(detail.path("definitions").get(0));
        assertThatThrownBy(() -> gatewayReadOperation(application, duplicate, expected)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void taskGatewayPolicyUsesOriginalMandatoryScopeAndFineProvidersAndSignedConfiguration() throws Exception {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var extractor = top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserCookieCredentialExtractor.createConfigured(
                    new top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpReservedHeaderSanitizer());
            var authentication = top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpIdentityAuthenticationProvider.createConfigured(
                    (context, token) -> { throw new AssertionError("Configuration compilation must not authenticate"); }, validators.getValidator());
            var mandatory = new top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpOAuthScopeAuthorizationProvider(null,
                    top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.NOOP, validators.getValidator());
            var ordinary = new top.egon.cola.platform.tianquan.jianshen.yuheng.security.Rbac3BizAppScopeAuthorizationProvider(
                    context -> { throw new AssertionError("Configuration compilation must not evaluate PDP"); });
            var registry = new top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityCapabilityRegistry(List.of(extractor), List.of(authentication),
                    List.of(mandatory, ordinary), List.of(new top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpTrustedIdentityMapper()));
            var compiler = new top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityPolicyCompiler(registry);
            var configuration = gatewayProtectedReadPolicy("yuheng:read", "123");
            var source = new top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy("it-light-security", "SECURITY", "OPERATION", configuration);
            var compiled = compiler.compile(List.of(source), java.util.Map.of(source.policyId(), java.util.Set.of(top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol.HTTP))).get(source.policyId());
            assertThat(compiled.requiredOAuthScopes()).containsExactly("yuheng:read");
            assertThat(registry.requiredAuthorizations(compiled)).containsExactly(mandatory.providerId());
            assertThat(compiled.authorizationProviderIds()).containsExactly(ordinary.providerId());
            assertThat(compiled.credentialForwardingMode()).isEqualTo(top.egon.cola.component.yuheng.core.security.CredentialForwardingMode.ORIGINAL_BEARER);
            for (String fault : List.of("missing", "string", "duplicate", "identity")) {
                var changed = new java.util.LinkedHashMap<>(configuration);
                if (fault.equals("missing")) { changed.remove("requiredOAuthScopes"); }
                else { changed.put("requiredOAuthScopes", fault.equals("string") ? "yuheng:read" : fault.equals("duplicate")
                        ? List.of("yuheng:read", "yuheng:read") : List.of("openid")); }
                var invalid = new top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy(source.policyId(), source.type(), source.scope(), changed);
                assertThatThrownBy(() -> compiler.compile(List.of(invalid), java.util.Map.of())).isInstanceOf(IllegalArgumentException.class);
            }
            var codec = new top.egon.cola.component.yuheng.runtime.rule.adapter.json.GatewayRuleJsonCodec();
            var changed = new java.util.LinkedHashMap<>(configuration); changed.put("requiredOAuthScopes", List.of("yuheng:catalog:write"));
            assertThat(UnifiedIdentityLiveClient.sha256(codec.write(source))).isNotEqualTo(UnifiedIdentityLiveClient.sha256(codec.write(
                    new top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy(source.policyId(), source.type(), source.scope(), changed))));
        }
    }

    @Test
    void taskUserGatewayReadRelaysTheOriginalBearerWithoutServiceIdentityHeaders() throws Exception {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var now = Instant.now();
            var user = new top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal("user-1", "1001", "jti-1", java.util.Set.of("yuheng"),
                    now.minusSeconds(1), now.plusSeconds(300), top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
            var proof = new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO(
                    URI.create("https://idp.example/tenants/1001"), URI.create("https://resource.example/yuheng"), null, "a".repeat(64), "b".repeat(64));
            var extractor = top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserCookieCredentialExtractor.createConfigured(
                    new top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpReservedHeaderSanitizer());
            var authentication = top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpIdentityAuthenticationProvider.createConfigured(
                    (context, token) -> new top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpGatewayJwtVerifier.Verification(
                            user, top.egon.cola.component.yuheng.core.security.AuthenticationFailure.NONE, null, java.util.Set.of("yuheng:read"), proof), validators.getValidator());
            var mandatory = new top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpOAuthScopeAuthorizationProvider(null,
                    top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.NOOP, validators.getValidator());
            var ordinary = new top.egon.cola.platform.tianquan.jianshen.yuheng.security.Rbac3BizAppScopeAuthorizationProvider(
                    context -> top.egon.cola.component.yuheng.core.security.AuthorizationDecision.allow());
            var registry = new top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityCapabilityRegistry(List.of(extractor), List.of(authentication),
                    List.of(mandatory, ordinary), List.of(new top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpTrustedIdentityMapper()));
            var source = new top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy("task-read", "SECURITY", "OPERATION", gatewayProtectedReadPolicy("yuheng:read", "123"));
            var policy = new top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityPolicyCompiler(registry)
                    .compile(List.of(source), java.util.Map.of(source.policyId(), java.util.Set.of(top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol.HTTP))).get(source.policyId());
            var exchange = org.mockito.Mockito.mock(top.egon.cola.component.yuheng.core.exchange.GatewayExchange.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
            org.mockito.Mockito.when(exchange.request().headers()).thenReturn(new top.egon.cola.component.yuheng.core.exchange.ImmutableGatewayHeaders(
                    java.util.Map.of("authorization", List.of("Bearer original-user-bearer"))));
            var context = new top.egon.cola.component.yuheng.core.security.GatewayAuthContext(top.egon.cola.component.yuheng.contract.protocol.AccessZone.PUBLIC,
                    top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol.HTTP, "123", null, source.policyId(), "/api/v1/yuheng/admin/scopes", "GET", java.util.Set.of(),
                    top.egon.cola.component.yuheng.core.context.GatewayPrincipal.anonymous(), "127.0.0.1", UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    Instant.now().plusSeconds(5), "task-release", java.util.Map.of());
            var result = new top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityChain(registry)
                    .execute(exchange, context, policy, top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol.HTTP).block(Duration.ofSeconds(5));
            assertThat(result.context().principal().principalType()).isEqualTo("USER");
            assertThat(result.forwardingCredential().tokenReference()).isEqualTo("original-user-bearer");
            assertThat(result.trustedIdentity().httpHeaders()).isEmpty();
        }
    }

    @Test
    void gatewayPublicationScopeBindsBothOriginalModelsToTheExactTask() throws Exception {
        String runId = UUID.randomUUID().toString(), expected = "xingyuan-it-" + runId;
        var biz = new Properties(); var mcp = new Properties();
        String engine = "egon.cola.component.yuheng.engine.", protocol = "egon.cola.component.yuheng.mcp-engine.";
        biz.setProperty(engine + "gateway-group-code", expected); biz.setProperty(engine + "env", "local"); biz.setProperty(engine + "namespace", expected);
        mcp.setProperty(protocol + "yuheng-group-code", expected); mcp.setProperty(protocol + "env", "local"); mcp.setProperty(protocol + "namespace", expected);
        mcp.setProperty(protocol + "node-id", "controlled-mcp"); mcp.setProperty(protocol + "instance-id", "controlled-mcp"); mcp.setProperty(protocol + "data-directory", "./controlled");
        biz.setProperty("egon.cola.component.tianshu.env", "local"); mcp.setProperty("egon.cola.component.tianshu.env", "local");
        var scope = gatewayTaskScope(biz, mcp, runId);
        assertThat(scope).isEqualTo(new GatewayTaskScopeVO(expected, "local", expected));
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            assertThat(validators.getValidator().validate(top.egon.cola.component.tianshu.admin.model.dto.DdcConfigQueryRequest.builder().env(scope.env()).build())).isEmpty();
        }
        for (var profile : List.of(biz, mcp)) {
            String prefix = profile == biz ? engine : protocol;
            for (String field : List.of(profile == biz ? "gateway-group-code" : "yuheng-group-code", "env", "namespace")) {
                profile.setProperty(prefix + field, "default");
                assertThatThrownBy(() -> gatewayTaskScope(biz, mcp, runId)).hasMessage("XINGYUAN_TARGET_UNSAFE");
                profile.setProperty(prefix + field, field.equals("env") ? "local" : expected);
            }
        }
        mcp.setProperty("egon.cola.component.tianshu.env", "foreign");
        assertThatThrownBy(() -> gatewayTaskScope(biz, mcp, runId)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        mcp.setProperty("egon.cola.component.tianshu.env", "local");
        assertThatThrownBy(() -> gatewayTaskScope(biz, mcp, UUID.randomUUID().toString())).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> gatewayTaskScope(biz, mcp, "unreviewed")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    private static final java.util.Map<String, String> PLATFORM_READ_PATHS = java.util.Map.of(
            "shoubing", "/api/v1/tianquan-shoubing/clients", "jianshen", "/api/tianquan-jianshen/v1/iam/roles",
            "yuheng", "/api/v1/yuheng/admin/scopes", "tianshu", "/api/v1/tianshu/registry/services/page?pageNo=1&pageSize=10");

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void nativeUserInfoRequestsUseRealRealmTokensAndReturnAuditCorrelation() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(bytes);
        String digest = UnifiedIdentityLiveClient.sha256(bytes); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("shoubing"));
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "NATIVE_USERINFO_HTTP_REQUESTS_ONLY"); var observations = proof.putArray("observations");
        for (String realm : List.of("default", "tenant-b")) {
            var reference = client.formal(realm, client.resourceForAlias("edge-shoubing"), "admin");
            if (!reference.getScopes().contains("openid")) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            var requests = List.of(client.observeNativeUserInfo(reference, true), client.observeNativeUserInfo(reference, false), client.observeNativeUserInfo(reference, true));
            var item = observations.addObject().put("realmCode", realm).put("issuer", reference.getIssuer().toASCIIString()).put("resourceUri", reference.getResourceUri().toASCIIString())
                    .put("tokenReference", reference.getTokenSha256()); item.set("requests", mapper.valueToTree(requests));
        }
        proof.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-native-userinfo.json"), proof);
        observeHttpAuditOwner(root, evidence, manifestFile, manifest, digest, "native-userinfo");
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("shoubing"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualOwnedStopRetainsTheOriginalCohortAndBoundedPreservationEvidence() throws Exception {
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] raw = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var mapper = UnifiedIdentityLiveClient.strictJson();
        var manifest = mapper.readTree(raw); String digest = UnifiedIdentityLiveClient.sha256(raw); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory")); requireEvidenceDirectory(root, evidence);
        Path result = evidence.resolve("resource-owned-stop-preservation.json");
        var stop = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "owned-stop-preservation", manifestFile.toString(), result.toString()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!stop.waitFor(manifest.path("deadlines").path("operationSeconds").intValue(), TimeUnit.SECONDS) || stop.exitValue() != 0) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        } finally { if (stop.isAlive()) { stop.destroy(); stop.waitFor(10, TimeUnit.SECONDS); } }
        var proof = XingyuanNativeAuthenticationIT.requireNativeEvidence(root, evidence, manifest, digest, result.getFileName().toString());
        int expected = manifest.path("processes").size() - (UnifiedIdentityLiveClient.activeNativeRuntimeRoles(root, manifest, digest).contains("native-agent") ? 0 : 1);
        if (!"ORIGINAL_OWNED_COHORT_STOP_AND_BOUNDED_PRESERVATION_ONLY".equals(proof.path("scope").asText())
                || transactionProofLong(proof, "ownedProcessCount") != expected || transactionProofLong(proof, "retainedCaseCount") != 23
                || transactionProofLong(proof, "retainedSecretFileCount") != manifest.path("secretFiles").size()
                || transactionProofLong(proof, "retainedDatabaseReceiptCount") != 9 || transactionProofLong(proof, "retainedDatabaseIdentityCount") != 9
                || !proof.path("proofs").isArray() || proof.path("proofs").size() != 4) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var expectedFiles = java.util.Set.of("owned-stop.json", "owned-preservation/original-databases-preserved.json",
                "owned-preservation/original-nginx-preserved.json", "owned-preservation/original-redis-preserved.json");
        var actual = new java.util.HashSet<String>();
        for (var reference : proof.path("proofs")) {
            Path path = root.resolve(reference.path("path").asText());
            if (!path.startsWith(evidence) || !path.normalize().equals(path) || !actual.add(evidence.relativize(path).toString())
                    || !UnifiedIdentityLiveClient.sha256(UnifiedIdentityLiveClient.protectedBytes(path, evidence, 128 * 1024)).equals(reference.path("sha256").asText())) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
        if (!actual.equals(expectedFiles) || !digest.equals(UnifiedIdentityLiveClient.sha256(UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024)))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void platformHostsRejectTamperingAndForeignAudienceBeforeTheirOriginalBusiness() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] manifestBytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(manifestBytes);
        String digest = UnifiedIdentityLiveClient.sha256(manifestBytes); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        requireEvidenceDirectory(root, evidence);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("shoubing", "jianshen", "yuheng", "tianshu"));
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "FOUR_PLATFORM_DIRECT_HTTP_AUTHENTICATION_AND_COUNTERS"); var observations = proof.putArray("observations");
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            for (String realm : List.of("default", "tenant-b")) {
                for (String role : List.of("shoubing", "jianshen", "yuheng", "tianshu")) {
                    var reference = client.formal(realm, client.resourceForAlias("edge-" + role), "admin"); client.verifiedClaims(reference);
                    var viewer = client.formal(realm, reference.getResourceKey(), "viewer"); client.verifiedClaims(viewer);
                    String foreignRole = role.equals("yuheng") ? "shoubing" : "yuheng";
                    var foreign = client.formal(realm, client.resourceForAlias("edge-" + foreignRole), "admin"); client.verifiedClaims(foreign);
                    if (reference.getResourceUri().equals(foreign.getResourceUri())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    var api = new NativeResourceHttpProbe(role, root, manifestFile, digest, http, timeout);
                    var calls = new ArrayList<HttpAuditObservationVO>();
                    var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, role, realm, "BUSINESS_COUNTS", null);
                    api.request("GET", PLATFORM_READ_PATHS.get(role), reference, null, null, 200, calls::add);
                    var successful = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, role, realm, "BUSINESS_COUNTS", null);
                    requireOneBusinessInvocation(before, successful);
                    api.request("GET", PLATFORM_READ_PATHS.get(role), viewer, null, null, 403, calls::add);
                    var scopeOnly = acquireIdentityOnlyProbe(client, root, evidence, manifestFile, manifest, digest, realm, reference.getResourceKey());
                    api.request("GET", PLATFORM_READ_PATHS.get(role), scopeOnly, null, null, 403, calls::add);
                    api.request("GET", PLATFORM_READ_PATHS.get(role), foreign, null, null, 401, calls::add);
                    api.request("GET", PLATFORM_READ_PATHS.get(role), null, null, null, 401, calls::add);
                    for (String mutation : List.of("SIGNATURE", "ISSUER", "TYPE", "DUPLICATE")) {
                        calls.add(platformTamperedRequest(http, timeout, root, reference, nativeResourceEndpoint(role, "GET", PLATFORM_READ_PATHS.get(role)), mutation));
                    }
                    var denied = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, role, realm, "BUSINESS_COUNTS", null);
                    if (!successful.equals(denied)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    api.request("GET", PLATFORM_READ_PATHS.get(role), reference, null, null, 200, calls::add);
                    var recovered = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, role, realm, "BUSINESS_COUNTS", null);
                    requireOneBusinessInvocation(denied, recovered);
                    var item = observations.addObject().put("role", role).put("realmCode", realm).put("tenantId", reference.getTenantId())
                            .put("tokenReference", reference.getTokenSha256()).put("viewerReference", viewer.getTokenSha256()).put("foreignAudienceReference", foreign.getTokenSha256())
                            .put("identityOnlyTokenReference", scopeOnly.getTokenSha256()).put("identityOnlyExpiresAt", scopeOnly.getExpiresAt().toString())
                            .put("successStatus", 200).put("pdpDeniedStatus", 403).put("authenticationDeniedStatus", 401).put("tamperedRequestCount", 4);
                    item.set("before", before); item.set("successful", successful); item.set("denied", denied); item.set("recovered", recovered);
                    item.set("requests", mapper.valueToTree(calls));
                    item.set("identityOnlyScopes", mapper.valueToTree(scopeOnly.getScopes()));
                }
            }
        }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("shoubing", "jianshen", "yuheng", "tianshu"));
        proof.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-platform-http-boundaries.json"), proof);
        observePlatformHttpAuditOwner(root, evidence, manifestFile, manifest, digest);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("shoubing", "jianshen", "yuheng", "tianshu"));
    }

    private static FormalReferenceDTO acquireIdentityOnlyProbe(UnifiedIdentityLiveClient.IsolatedResourceProbe client, Path root, Path evidence,
            Path manifestFile, JsonNode manifest, String digest, String realm, String resource) throws Exception {
        if (!java.util.Set.of("default", "tenant-b").contains(realm) || !resource.matches("[a-z][a-z0-9-]{2,63}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        Path canonical = root.resolve("secrets/token-index.json"); byte[] before = UnifiedIdentityLiveClient.protectedBytes(canonical, root.resolve("secrets"), 128 * 1024);
        String nonce = UUID.randomUUID().toString(); Path token = root.resolve("secrets/scope-probe-" + nonce + ".at"), result = evidence.resolve("scope-probe-" + nonce + ".json");
        var process = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "authorize-scope-probe", manifestFile.toString(), result.toString(), realm, resource, "admin", token.toString(), manifest.path("repoRoot").asText())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!process.waitFor(manifest.path("deadlines").path("operationSeconds").intValue(), TimeUnit.SECONDS) || process.exitValue() != 0) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        } finally { if (process.isAlive()) { process.destroy(); process.waitFor(10, TimeUnit.SECONDS); } }
        var outcome = UnifiedIdentityLiveClient.strictJson().readTree(UnifiedIdentityLiveClient.protectedBytes(result, evidence, 128 * 1024));
        if (!"CLI-006".equals(outcome.path("operationId").asText()) || !"COMMITTED".equals(outcome.path("status").asText()) || !outcome.path("error").isNull()
                || !manifest.path("runId").asText().equals(outcome.path("runId").asText()) || !digest.equals(outcome.path("manifestSha256").asText())
                || !java.util.Arrays.equals(before, UnifiedIdentityLiveClient.protectedBytes(canonical, root.resolve("secrets"), 128 * 1024))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var reference = client.identityOnly(realm, resource, Path.of(token + ".metadata.json"));
        if (!reference.getScopes().equals(java.util.Set.of("openid"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return reference;
    }

    private static void observePlatformHttpAuditOwner(Path root, Path evidence, Path manifestFile, JsonNode manifest, String digest) throws Exception {
        observeHttpAuditOwner(root, evidence, manifestFile, manifest, digest, "platform-http");
    }

    static void observeNativeRpcAuditOwner(Path root, Path evidence, Path manifestFile, JsonNode manifest, String digest) throws Exception {
        observeHttpAuditOwner(root, evidence, manifestFile, manifest, digest, "native-rpc");
    }

    private static void observeHttpAuditOwner(Path root, Path evidence, Path manifestFile, JsonNode manifest, String digest, String protocol) throws Exception {
        if (!java.util.Set.of("platform-http", "native-userinfo", "native-rpc", "mcp").contains(protocol)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        String stage = protocol + "-audit";
        String method = switch (protocol) { case "mcp" -> "observeMcpHttpAudits"; case "native-rpc" -> "observeNativeRpcAudits"; case "native-userinfo" -> "observeNativeUserInfoAudits"; default -> "observePlatformHttpAudits"; };
        String sourceName = switch (protocol) { case "mcp" -> "resource-mcp-boundaries.json"; case "native-rpc" -> "native-rpc-contract-probe.json"; case "native-userinfo" -> "resource-native-userinfo.json"; default -> "resource-platform-http-boundaries.json"; };
        String proofScope = switch (protocol) { case "mcp" -> "MCP_HTTP_PG_STREAM_SIGNED_RECEIPTS_ONLY"; case "native-rpc" -> "NATIVE_RPC_PG_STREAM_SIGNED_RECEIPTS_ONLY"; case "native-userinfo" -> "NATIVE_USERINFO_PG_STREAM_SIGNED_RECEIPTS_ONLY"; default -> "FOUR_PLATFORM_HTTP_PG_STREAM_SIGNED_RECEIPTS_ONLY"; };
        long requests = protocol.equals("mcp") ? 12 : protocol.equals("native-rpc") ? 16 : protocol.equals("native-userinfo") ? 6 : 80;
        long events = protocol.equals("mcp") || protocol.equals("native-rpc") ? 20 : protocol.equals("native-userinfo") ? 6 : 96;
        Path result = evidence.resolve(stage + "-owner.json");
        var command = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "observe-" + protocol + "-audits", manifestFile.toString(), result.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!command.waitFor(manifest.path("deadlines").path("operationSeconds").intValue(), TimeUnit.SECONDS) || command.exitValue() != 0) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        } finally {
            if (command.isAlive()) { command.destroy(); command.waitFor(10, TimeUnit.SECONDS); }
        }
        var mapper = UnifiedIdentityLiveClient.strictJson(); var receipt = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(result, evidence, 128 * 1024));
        byte[] source = UnifiedIdentityLiveClient.protectedBytes(evidence.resolve(sourceName), evidence, 128 * 1024);
        Path proofFile = evidence.resolve("protocol-" + protocol + "-audits.json");
        byte[] proofBytes = UnifiedIdentityLiveClient.protectedBytes(proofFile, evidence, 128 * 1024);
        var execution = receipt.path("execution");
        if (!manifest.path("runId").asText().equals(receipt.path("runId").asText()) || !digest.equals(receipt.path("manifestSha256").asText())
                || !"PASSED".equals(receipt.path("status").asText()) || !(stage.toUpperCase(java.util.Locale.ROOT).replace('-', '_') + "_OWNER_EXECUTION_ONLY").equals(receipt.path("scope").asText())
                || !UnifiedIdentityLiveClient.sha256(source).equals(receipt.path("sourceSha256").asText())
                || !root.relativize(proofFile).toString().equals(receipt.path("proof").path("path").asText())
                || !UnifiedIdentityLiveClient.sha256(proofBytes).equals(receipt.path("proof").path("sha256").asText())
                || !"top.egon.cola.platform.tianquan.shoubing.admin.integration.IsolatedOAuthProvisioningIT".equals(execution.path("classname").asText())
                || !execution.path("testCount").isIntegralNumber() || execution.path("testCount").intValue() != 1
                || !execution.path("methods").isArray() || execution.path("methods").size() != 1 || !method.equals(execution.path("methods").get(0).asText())
                || !execution.path("reportSha256").asText().matches("[0-9a-f]{64}")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var proof = mapper.readTree(proofBytes);
        if (!"PASSED".equals(proof.path("status").asText()) || !manifest.path("runId").asText().equals(proof.path("runId").asText())
                || !digest.equals(proof.path("manifestSha256").asText()) || !UnifiedIdentityLiveClient.sha256(source).equals(proof.path("sourceSha256").asText())
                || !proofScope.equals(proof.path("scope").asText()) || transactionProofLong(proof, "requestCount") != requests
                || transactionProofLong(proof, "storedEventCount") != events || transactionProofLong(proof, "spoolAbsentAfterReceiptCount") != events) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    private static void requireOneBusinessInvocation(JsonNode before, JsonNode after) {
        if (before.path("counts").size() != 1 || after.path("counts").size() != 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        String method = before.path("counts").fieldNames().next();
        if (Math.addExact(transactionProofLong(before.path("counts"), method), 1) != transactionProofLong(after.path("counts"), method)) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    private static HttpAuditObservationVO platformTamperedRequest(HttpClient http, Duration timeout, Path root, FormalReferenceDTO reference, URI endpoint, String mutation) throws Exception {
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(reference.getTokenFile(), root.resolve("secrets"), 128 * 1024);
        try {
            if (!reference.getExpiresAt().isAfter(Instant.now()) || !reference.getTokenSha256().equals(UnifiedIdentityLiveClient.sha256(bytes))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            String trace = UUID.randomUUID().toString().replace("-", ""); Instant started = Instant.now();
            var request = HttpRequest.newBuilder(endpoint).timeout(timeout).GET().header("X-Request-Id", UUID.randomUUID().toString())
                    .header("traceparent", "00-" + trace + '-' + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01");
            String token = new String(bytes, StandardCharsets.UTF_8);
            request.header("Authorization", "Bearer " + (mutation.equals("DUPLICATE") ? token : tamperedBearer(token, mutation)));
            if (mutation.equals("DUPLICATE")) { request.header("Authorization", "Bearer " + token); }
            var response = http.send(request.build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 401 || response.headers().firstValue("Location").isPresent()
                    || response.headers().firstValue("Set-Cookie").isPresent()
                    || !response.headers().allValues("Cache-Control").stream().anyMatch(value -> value.contains("no-store"))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            return observedHttpCall(response, trace, started);
        } finally { java.util.Arrays.fill(bytes, (byte) 0); }
    }

    @Test
    void httpAuditCorrelationRequiresTheActualSingleReturnedTrace() throws Exception {
        java.util.function.Function<List<String>, HttpResponse<Void>> response = values -> new HttpResponse<>() {
            @Override public int statusCode() { return 401; }
            @Override public HttpRequest request() { return HttpRequest.newBuilder(URI.create("https://127.0.0.1:28140/api/v1/yuheng/admin/scopes")).GET().build(); }
            @Override public java.util.Optional<HttpResponse<Void>> previousResponse() { return java.util.Optional.empty(); }
            @Override public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(java.util.Map.of("traceparent", values), (key, value) -> true); }
            @Override public Void body() { return null; }
            @Override public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
            @Override public URI uri() { return request().uri(); }
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
        String trace = "a".repeat(32), header = "00-" + trace + '-' + "b".repeat(16) + "-01";
        var observed = observedHttpCall(response.apply(List.of(header)), trace, Instant.now().minusSeconds(1));
        assertThat(observed.traceId()).isEqualTo(trace); assertThat(observed.status()).isEqualTo(401);
        assertThat(UnifiedIdentityLiveClient.strictJson().valueToTree(observed).size()).isEqualTo(4);
        for (var invalid : List.of(List.<String>of(), List.of(header, header), List.of(header.replace(trace, "c".repeat(32))),
                List.of(header.replace("b".repeat(16), "0".repeat(16))), List.of("malformed"))) {
            assertThatThrownBy(() -> observedHttpCall(response.apply(invalid), trace, Instant.now().minusSeconds(1)))
                    .hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    static String tamperedBearer(String original, String mutation) throws Exception {
        String[] parts = original.split("\\.", -1);
        if (!java.util.Set.of("SIGNATURE", "ISSUER", "TYPE").contains(mutation) || parts.length != 3
                || java.util.Arrays.stream(parts).anyMatch(value -> !value.matches("[A-Za-z0-9_-]{1,131072}"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var decoder = java.util.Base64.getUrlDecoder(); var encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        if (mutation.equals("SIGNATURE")) {
            byte[] signature = decoder.decode(parts[2]); signature[0] ^= 1; parts[2] = encoder.encodeToString(signature);
        } else {
            int selected = mutation.equals("TYPE") ? 0 : 1;
            var value = UnifiedIdentityLiveClient.strictJson().readTree(decoder.decode(parts[selected]));
            if (!value.isObject()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            ((com.fasterxml.jackson.databind.node.ObjectNode) value).put(mutation.equals("TYPE") ? "typ" : "iss", mutation.equals("TYPE") ? "JWT" : "https://foreign.invalid/platform");
            parts[selected] = encoder.encodeToString(UnifiedIdentityLiveClient.strictJson().writeValueAsBytes(value));
        }
        return String.join(".", parts);
    }

    @Test
    void bearerTamperingNeverSignsOrReplacesAnUnchangedRequest() throws Exception {
        var encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"typ\":\"at+jwt\",\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String claims = encoder.encodeToString("{\"iss\":\"https://owned.invalid/tenants/1\"}".getBytes(StandardCharsets.UTF_8));
        String signature = encoder.encodeToString(new byte[] {1, 2, 3, 4}); String original = header + '.' + claims + '.' + signature;
        for (String mutation : List.of("SIGNATURE", "ISSUER", "TYPE")) {
            var changed = tamperedBearer(original, mutation).split("\\."); assertThat(String.join(".", changed)).isNotEqualTo(original);
            if (mutation.equals("SIGNATURE")) { assertThat(changed[0]).isEqualTo(header); assertThat(changed[1]).isEqualTo(claims); }
            else { assertThat(changed[2]).isEqualTo(signature); }
        }
        assertThatThrownBy(() -> tamperedBearer(original, "SIGN")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void threeIssuerMetadataUsesDistinctPublicKeysAndRetiresTheRoot() throws Exception {
        // Reuse the original manifest, registry, profile and trusted-issuer input gate before HTTP.
        UnifiedIdentityLiveClient.isolated();
        var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] manifestBytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024);
        var manifest = mapper.readTree(manifestBytes); String digest = UnifiedIdentityLiveClient.sha256(manifestBytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        requireEvidenceDirectory(root, evidence);
        Path registryFile = root.resolve("secrets/runtime-registry.json");
        byte[] registryBytes = UnifiedIdentityLiveClient.protectedBytes(registryFile, root.resolve("secrets"), 2 * 1024 * 1024);
        var registry = mapper.readTree(registryBytes);
        String origin = manifest.path("publicOrigin").asText();
        if (!"https://127.0.0.1:28443".equals(origin) || !origin.equals(registry.path("publicOrigin").asText())
                || !digest.equals(registry.path("manifestSha256").asText()) || !manifest.path("runId").asText().equals(registry.path("runId").asText())
                || registry.path("realms").size() != 3 || registry.path("tenantIds").size() != 2) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("shoubing", "edge"));
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText())
                .put("manifestSha256", digest).put("scope", "THREE_ISSUER_PROTOCOL_ONLY");
        var observations = proof.putArray("issuers");
        var signingKeys = new ArrayList<java.util.Map<String, String>>();
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        try (var transport = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            for (String realm : List.of("platform", "default", "tenant-b")) {
                String suffix = realm.equals("platform") ? "/platform" : "/tenants/" + registry.path("tenantIds").path(realm).asText();
                if (!suffix.equals("/platform") && !suffix.matches("/tenants/[1-9][0-9]{0,18}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                String issuer = origin + suffix;
                if (!issuer.equals(registry.path("realms").path(realm).path("issuer").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                var metadata = protocolResponse(transport, URI.create(issuer + "/.well-known/openid-configuration"), timeout, false, 200);
                requireCanonicalIssuerMetadata(issuer, metadata);
                var forwarded = protocolResponse(transport, URI.create(issuer + "/.well-known/openid-configuration"), timeout, true, 200);
                requireCanonicalIssuerMetadata(issuer, forwarded);
                var jwks = protocolResponse(transport, URI.create(issuer + "/.well-known/jwks.json"), timeout, false, 200);
                var keys = verifiedIssuerPublicKeys(jwks); signingKeys.add(keys);
                var item = observations.addObject().put("realmCode", realm).put("issuer", issuer)
                        .put("metadataReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(metadata)))
                        .put("forwardedReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(forwarded)))
                        .put("jwksReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(jwks)));
                var references = item.putArray("signingKeyReferences"); keys.values().stream().sorted().forEach(references::add);
            }
            requireDistinctIssuerKeys(signingKeys);
            var retired = protocolResponse(transport, URI.create(origin + "/.well-known/oauth-authorization-server"), timeout, false, 410);
            if (!"legacy_endpoint_retired".equals(retired.path("error").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            proof.put("retiredRootStatus", 410).put("retiredRootReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(retired)));
        }
        if (!java.util.Arrays.equals(manifestBytes, UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024))
                || !java.util.Arrays.equals(registryBytes, UnifiedIdentityLiveClient.protectedBytes(registryFile, root.resolve("secrets"), 2 * 1024 * 1024))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        proof.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        publishResourceEvidence(root, evidence, "resource-issuer-discovery.json", proof);
    }

    private static JsonNode protocolResponse(HttpClient http, URI endpoint, Duration timeout, boolean forwarded, int expected) throws Exception {
        if (!"https".equals(endpoint.getScheme()) || !"127.0.0.1".equals(endpoint.getHost()) || endpoint.getPort() != 28443
                || endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null
                || !endpoint.getPath().matches("(?:(?:/platform|/tenants/[1-9][0-9]{0,18})/\\.well-known/(?:openid-configuration|jwks\\.json)|/\\.well-known/oauth-authorization-server)")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var request = HttpRequest.newBuilder(endpoint).timeout(timeout).GET();
        if (forwarded) { request.header("Forwarded", "host=foreign.invalid;proto=http").header("X-Forwarded-Host", "foreign.invalid").header("X-Forwarded-Proto", "http"); }
        var output = new ByteArrayOutputStream(); var response = http.send(request.build(), boundedBody(output));
        if (response.statusCode() != expected || response.headers().firstValue("Location").isPresent()
                || response.headers().firstValue("Set-Cookie").isPresent()
                || !response.headers().firstValue("Content-Type").orElse("").toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        return UnifiedIdentityLiveClient.strictJson().readTree(output.toByteArray());
    }

    static void requireCanonicalIssuerMetadata(String issuer, JsonNode value) {
        if (value == null || !value.isObject() || !issuer.equals(value.path("issuer").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (var field : java.util.Map.of("authorization_endpoint", "/oauth2/authorize", "token_endpoint", "/oauth2/token",
                "userinfo_endpoint", "/userinfo", "jwks_uri", "/.well-known/jwks.json").entrySet()) {
            if (!value.path(field.getKey()).isTextual() || !(issuer + field.getValue()).equals(value.path(field.getKey()).textValue())) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
        var algorithms = value.path("id_token_signing_alg_values_supported");
        if (!algorithms.isArray() || algorithms.isEmpty() || java.util.stream.StreamSupport.stream(algorithms.spliterator(), false)
                .anyMatch(algorithm -> !algorithm.isTextual() || !"RS256".equals(algorithm.textValue()))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    static java.util.Map<String, String> verifiedIssuerPublicKeys(JsonNode document) {
        if (document == null || !document.isObject() || !document.path("keys").isArray() || document.path("keys").isEmpty()
                || document.path("keys").size() > 16) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var result = new java.util.LinkedHashMap<String, String>();
        for (var key : document.path("keys")) {
            String id = key.path("kid").asText(), modulus = key.path("n").asText(), exponent = key.path("e").asText();
            if (!key.isObject() || !"RSA".equals(key.path("kty").asText()) || !"RS256".equals(key.path("alg").asText()) || !"sig".equals(key.path("use").asText())
                    || !key.path("kid").isTextual() || id.isBlank() || id.length() > 128 || id.chars().anyMatch(Character::isISOControl)
                    || !key.path("n").isTextual() || !modulus.matches("[A-Za-z0-9_-]{1,16384}") || !key.path("e").isTextual() || !exponent.matches("[A-Za-z0-9_-]{1,16}")
                    || java.util.stream.Stream.of("d", "p", "q", "dp", "dq", "qi", "oth").anyMatch(key::has)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            try {
                var decoder = java.util.Base64.getUrlDecoder();
                var n = new java.math.BigInteger(1, decoder.decode(modulus)); var e = new java.math.BigInteger(1, decoder.decode(exponent));
                if (n.bitLength() < 3072 || !n.testBit(0) || e.compareTo(java.math.BigInteger.valueOf(3)) < 0 || !e.testBit(0)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                String reference = UnifiedIdentityLiveClient.sha256((n.toString(16) + ":" + e.toString(16)).getBytes(StandardCharsets.US_ASCII));
                if (result.containsValue(reference) || result.putIfAbsent(id, reference) != null) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            } catch (IllegalArgumentException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED", invalid); }
        }
        return java.util.Map.copyOf(result);
    }

    static void requireDistinctIssuerKeys(List<java.util.Map<String, String>> realms) {
        if (realms == null || realms.size() != 3) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var ids = new java.util.HashSet<String>(); var keys = new java.util.HashSet<String>();
        for (var realm : realms) {
            if (realm == null || realm.isEmpty()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            for (var entry : realm.entrySet()) {
                if (!ids.add(entry.getKey()) || !keys.add(entry.getValue())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            }
        }
    }

    private static void publishResourceEvidence(Path root, Path evidence, String filename, JsonNode proof) throws Exception {
        requireEvidenceDirectory(root, evidence);
        if (!"resource-issuer-discovery.json".equals(filename)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        Path temporary = Files.createTempFile(evidence, ".resource-protocol-", ".tmp", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            try (var output = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) {
                var bytes = java.nio.ByteBuffer.wrap(UnifiedIdentityLiveClient.strictJson().writeValueAsBytes(proof));
                while (bytes.hasRemaining()) { output.write(bytes); }
                output.force(true);
            }
            Files.createLink(evidence.resolve(filename), temporary);
            try (var directory = java.nio.channels.FileChannel.open(evidence, java.nio.file.StandardOpenOption.READ)) { directory.force(true); }
        } finally { Files.deleteIfExists(temporary); }
    }

    @Test
    void issuerMetadataRejectsForeignCanonicalEndpointsAndAlgorithms() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); String issuer = "https://127.0.0.1:28443/tenants/1001";
        var metadata = mapper.createObjectNode().put("issuer", issuer).put("authorization_endpoint", issuer + "/oauth2/authorize")
                .put("token_endpoint", issuer + "/oauth2/token").put("userinfo_endpoint", issuer + "/userinfo").put("jwks_uri", issuer + "/.well-known/jwks.json");
        metadata.putArray("id_token_signing_alg_values_supported").add("RS256");
        assertThatCode(() -> requireCanonicalIssuerMetadata(issuer, metadata)).doesNotThrowAnyException();
        for (String field : List.of("issuer", "authorization_endpoint", "token_endpoint", "userinfo_endpoint", "jwks_uri")) {
            var changed = metadata.deepCopy().put(field, "https://foreign.invalid");
            assertThatThrownBy(() -> requireCanonicalIssuerMetadata(issuer, changed)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        metadata.withArray("id_token_signing_alg_values_supported").add("none");
        assertThatThrownBy(() -> requireCanonicalIssuerMetadata(issuer, metadata)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    void issuerKeysRejectPrivateWeakDuplicateAndCrossRealmMaterial() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var document = mapper.createObjectNode();
        String modulus = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(java.math.BigInteger.ONE.shiftLeft(3071).add(java.math.BigInteger.valueOf(17)).toByteArray());
        var key = document.putArray("keys").addObject().put("kty", "RSA").put("alg", "RS256").put("use", "sig").put("kid", "realm-a").put("n", modulus).put("e", "AQAB");
        var original = verifiedIssuerPublicKeys(document); assertThat(original).hasSize(1);
        var privateKey = document.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) privateKey.path("keys").get(0)).put("d", "private");
        assertThatThrownBy(() -> verifiedIssuerPublicKeys(privateKey)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var weak = document.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) weak.path("keys").get(0)).put("n", "AQAB");
        assertThatThrownBy(() -> verifiedIssuerPublicKeys(weak)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        document.withArray("keys").add(key.deepCopy().put("kid", "same-material"));
        assertThatThrownBy(() -> verifiedIssuerPublicKeys(document)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatCode(() -> requireDistinctIssuerKeys(List.of(java.util.Map.of("a", "key-a"), java.util.Map.of("b", "key-b"), java.util.Map.of("c", "key-c")))).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireDistinctIssuerKeys(List.of(original, java.util.Map.of("b", original.values().iterator().next()), java.util.Map.of("c", "key-c"))))
                .hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> requireDistinctIssuerKeys(List.of(original, original, java.util.Map.of("c", "key-c")))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    /** This is only the JPA/MP portion of LIVE002; the identity transaction cases remain separate. */
    static List<Path> readJpaMpTransactionProofs(Path root, Path evidence, JsonNode manifest, String digest) throws Exception {
        requireEvidenceDirectory(root, evidence);
        if (!root.resolve("evidence").equals(evidence.getParent()) || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var mapper = UnifiedIdentityLiveClient.strictJson(); String runId = manifest.path("runId").asText();
        var attempt = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("intent.json"), evidence, 128 * 1024));
        var database = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/db-rbac.json"), root.resolve("evidence"), 128 * 1024));
        if (!runId.equals(attempt.path("runId").asText()) || !digest.equals(attempt.path("manifestSha256").asText())
                || !"full".equals(attempt.path("caseSet").asText()) || !runId.equals(database.path("runId").asText()) || !digest.equals(database.path("manifestSha256").asText())
                || !"rbac".equals(database.path("role").asText()) || !"xingyuan_it_rbac_20261011".equals(database.path("databaseName").asText())
                || !("xingyuan-it:" + runId + ":" + digest).equals(database.path("ownerComment").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        long oid = transactionProofLong(database, "oid");
        Instant attemptStartedAt = transactionProofInstant(attempt, "startedAt");
        long operationSeconds = transactionProofLong(manifest.path("deadlines"), "operationSeconds");
        if (operationSeconds < 1L || operationSeconds > 900L || attemptStartedAt.isBefore(Instant.now().minusSeconds(operationSeconds))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var proofs = new ArrayList<JsonNode>(); var paths = new ArrayList<Path>();
        for (String outcome : List.of("ROLLBACK", "APPEND_FAULT", "COMMIT")) {
            Path file = evidence.resolve("jpa-mp-" + outcome.toLowerCase(java.util.Locale.ROOT).replace('_', '-') + ".json");
            var proof = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(file, evidence, 128 * 1024));
            if (!outcome.equals(proof.path("outcome").asText()) || Files.getLastModifiedTime(file, java.nio.file.LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(attemptStartedAt)) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            proofs.add(proof); paths.add(file);
        }
        validateJpaMpTransactionProofs(proofs, runId, digest, oid, attemptStartedAt, Instant.now());
        return List.copyOf(paths);
    }

    static void validateJpaMpTransactionProofs(List<JsonNode> proofs, String runId, String digest, long oid, Instant attemptStartedAt, Instant now) {
        if (proofs == null || proofs.size() != 3 || oid <= 0 || attemptStartedAt == null || now == null || attemptStartedAt.isAfter(now)) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var outcomes = new java.util.HashSet<String>(); var transactions = new java.util.HashSet<Long>();
        var expected = java.util.Set.of("schemaVersion", "runId", "manifestSha256", "databaseOid", "scope", "outcome", "status", "backendPid", "transactionId",
                "sameConnectionConfirmed", "baseline", "observed", "restored", "attemptedInsertCount", "restoreInsertCount", "committedOutboxDelta", "restoreOutboxDelta",
                "restorePerformed", "originalGrantsRestored", "startedAt", "recordedAt");
        for (var proof : proofs) {
            if (proof == null || !proof.isObject()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            var fields = new java.util.HashSet<String>(); proof.fieldNames().forEachRemaining(fields::add);
            String outcome = proof.path("outcome").asText();
            if (!expected.equals(fields) || transactionProofLong(proof, "schemaVersion") != 1L || !runId.equals(proof.path("runId").asText())
                    || !digest.equals(proof.path("manifestSha256").asText()) || transactionProofLong(proof, "databaseOid") != oid
                    || !"JPA_MP_SAME_TRANSACTION_ONLY".equals(proof.path("scope").asText()) || !"PASSED".equals(proof.path("status").asText())
                    || !java.util.Set.of("ROLLBACK", "APPEND_FAULT", "COMMIT").contains(outcome) || !outcomes.add(outcome)
                    || transactionProofLong(proof, "backendPid") == 0 || transactionProofLong(proof, "backendPid") > Integer.MAX_VALUE
                    || transactionProofLong(proof, "transactionId") == 0 || !transactions.add(transactionProofLong(proof, "transactionId"))
                    || transactionProofLong(proof, "attemptedInsertCount") != 1 || !proof.path("sameConnectionConfirmed").isBoolean() || !proof.path("sameConnectionConfirmed").booleanValue()
                    || !proof.path("originalGrantsRestored").isBoolean() || !proof.path("originalGrantsRestored").booleanValue() || !proof.path("restorePerformed").isBoolean()) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            Instant started = transactionProofInstant(proof, "startedAt"), recorded = transactionProofInstant(proof, "recordedAt");
            if (started.isBefore(attemptStartedAt) || recorded.isBefore(started) || recorded.isAfter(now)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            for (String name : List.of("baseline", "observed", "restored")) {
                var state = proof.path(name); var stateFields = new java.util.HashSet<String>(); state.fieldNames().forEachRemaining(stateFields::add);
                if (!state.isObject() || !stateFields.equals(java.util.Set.of("independentBackendPid", "roleVersion", "policyVersion", "authorizationStateVersion", "outboxCount", "rowsSha256"))
                        || transactionProofLong(state, "independentBackendPid") == 0 || transactionProofLong(state, "independentBackendPid") > Integer.MAX_VALUE
                        || transactionProofLong(state, "independentBackendPid") == transactionProofLong(proof, "backendPid")
                        || !state.path("rowsSha256").isTextual() || !state.path("rowsSha256").textValue().matches("[0-9a-f]{64}")) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
                for (String value : List.of("roleVersion", "policyVersion", "authorizationStateVersion", "outboxCount")) { transactionProofLong(state, value); }
            }
            var before = proof.path("baseline"); var after = proof.path("observed"); var restored = proof.path("restored");
            long delta = transactionProofLong(proof, "committedOutboxDelta"), restoreDelta = transactionProofLong(proof, "restoreOutboxDelta");
            if (delta != transactionProofLong(after, "outboxCount") - transactionProofLong(before, "outboxCount")
                    || restoreDelta != transactionProofLong(restored, "outboxCount") - transactionProofLong(after, "outboxCount")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            if ("COMMIT".equals(outcome)) {
                if (!proof.path("restorePerformed").booleanValue() || transactionProofLong(proof, "restoreInsertCount") != 1 || delta != 1 || restoreDelta != 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                for (String version : List.of("roleVersion", "policyVersion", "authorizationStateVersion")) {
                    if (transactionProofLong(after, version) <= transactionProofLong(before, version) || transactionProofLong(restored, version) <= transactionProofLong(after, version)) {
                        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                    }
                }
            } else {
                if (proof.path("restorePerformed").booleanValue() || transactionProofLong(proof, "restoreInsertCount") != 0 || delta != 0 || restoreDelta != 0) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                for (String field : List.of("roleVersion", "policyVersion", "authorizationStateVersion", "outboxCount", "rowsSha256")) {
                    if (!before.path(field).equals(after.path(field)) || !after.path(field).equals(restored.path(field))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                }
            }
        }
    }

    static long transactionProofLong(JsonNode document, String field) {
        var value = document.path(field);
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0L) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return value.longValue();
    }

    static Instant transactionProofInstant(JsonNode document, String field) {
        var value = document.path(field);
        if (!value.isTextual() || !value.textValue().endsWith("Z")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        try { return Instant.parse(value.textValue()); }
        catch (java.time.format.DateTimeParseException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED", invalid); }
    }

    @Test
    void jpaMpEvidenceCannotReplaceAnOutcomeOrHideRestoreVersionAndEventChanges() {
        String runId = UUID.randomUUID().toString(), digest = "a".repeat(64); Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var proofs = transactionProofDocuments(runId, digest, now);
        assertThatCode(() -> validateJpaMpTransactionProofs(proofs, runId, digest, 71L, now.minusSeconds(5), now)).doesNotThrowAnyException();
        for (String fault : List.of("duplicate", "foreignRun", "sameBackend", "restoreVersion", "restoreEvent", "oldTime", "offsetTime", "overflow", "unknownScope")) {
            List<JsonNode> changed = proofs.stream().map(value -> (JsonNode) value.deepCopy()).toList();
            var last = (com.fasterxml.jackson.databind.node.ObjectNode) changed.get(2);
            if (fault.equals("duplicate")) { last.put("outcome", "ROLLBACK"); }
            else if (fault.equals("foreignRun")) { last.put("runId", UUID.randomUUID().toString()); }
            else if (fault.equals("sameBackend")) { last.withObject("observed").put("independentBackendPid", 301); }
            else if (fault.equals("restoreVersion")) { last.withObject("restored").put("roleVersion", 2); }
            else if (fault.equals("restoreEvent")) { last.put("restoreOutboxDelta", 0); }
            else if (fault.equals("oldTime")) { last.put("startedAt", now.minusSeconds(10).toString()); }
            else if (fault.equals("offsetTime")) { last.put("recordedAt", "2026-10-15T08:00:00+08:00"); }
            else if (fault.equals("overflow")) { last.put("transactionId", new java.math.BigInteger("18446744073709551616")); }
            else { last.put("scope", "LIVE002_COMPLETE"); }
            assertThatThrownBy(() -> validateJpaMpTransactionProofs(changed, runId, digest, 71L, now.minusSeconds(5), now)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        assertThatThrownBy(() -> validateJpaMpTransactionProofs(proofs.subList(0, 2), runId, digest, 71L, now.minusSeconds(5), now)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    private static List<JsonNode> transactionProofDocuments(String runId, String digest, Instant now) {
        var result = new ArrayList<JsonNode>(); int ordinal = 0;
        for (String outcome : List.of("ROLLBACK", "APPEND_FAULT", "COMMIT")) {
            var value = UnifiedIdentityLiveClient.strictJson().createObjectNode().put("schemaVersion", 1).put("runId", runId).put("manifestSha256", digest)
                    .put("databaseOid", 71).put("scope", "JPA_MP_SAME_TRANSACTION_ONLY").put("outcome", outcome).put("status", "PASSED")
                    .put("backendPid", 301).put("transactionId", 701 + ordinal++).put("sameConnectionConfirmed", true).put("attemptedInsertCount", 1)
                    .put("restoreInsertCount", outcome.equals("COMMIT") ? 1 : 0).put("committedOutboxDelta", outcome.equals("COMMIT") ? 1 : 0)
                    .put("restoreOutboxDelta", outcome.equals("COMMIT") ? 1 : 0).put("restorePerformed", outcome.equals("COMMIT")).put("originalGrantsRestored", true)
                    .put("startedAt", now.minusSeconds(1).toString()).put("recordedAt", now.toString());
            for (int index = 0; index < 3; index++) {
                int offset = outcome.equals("COMMIT") ? index : 0;
                value.putObject(List.of("baseline", "observed", "restored").get(index)).put("independentBackendPid", 302 + index)
                        .put("roleVersion", 2 + offset).put("policyVersion", 7 + offset).put("authorizationStateVersion", 5 + offset)
                        .put("outboxCount", 4 + offset).put("rowsSha256", String.valueOf((char) ('b' + offset)).repeat(64));
            }
            result.add(value);
        }
        return List.copyOf(result);
    }

    record NativeSqlRowVO(@Positive long id, @Positive long tenantId, @NotBlank String externalId,
                          @NotBlank String name, @NotBlank String email, @NotBlank String status) { }

    static void requireOwnedRows(List<NativeSqlRowVO> rows, long tenantA, long tenantB,
                                 long idA, long idB, String nameA, String nameB) {
        if (rows == null || rows.size() != 2 || tenantA <= 0 || tenantB <= 0 || tenantA == tenantB
                || idA <= 0 || idB <= 0 || idA == idB || rows.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
        }
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (var row : rows) {
                if (!factory.getValidator().validate(row).isEmpty()) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
                boolean first = row.id() == idA && row.tenantId() == tenantA && row.name().equals(nameA);
                boolean second = row.id() == idB && row.tenantId() == tenantB && row.name().equals(nameB);
                if (!"ext-1".equals(row.externalId()) || !first && !second) {
                    throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
                }
            }
        }
        if (rows.stream().map(NativeSqlRowVO::id).distinct().count() != 2) {
            throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void nativeLightSqlIsolationMatchesTheSameFormalTenantCalls() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated();
        var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] manifestBytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024);
        var manifest = mapper.readTree(manifestBytes);
        String digest = UnifiedIdentityLiveClient.sha256(manifestBytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        requireEvidenceDirectory(root, evidence);
        if (Files.exists(evidence.resolve("native-tenant-isolation.json"))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("native-light"));
        var profile = verifiedProfile(root, manifest, digest, "native-light");
        // ext-1 is the original local adapter's only external identity; never replace it with a fake upstream.
        boolean localExternalUser = "false".equals(profile.getProperty("app.integrations.external-http.enabled", "false"));
        profile.clear();
        if (!localExternalUser) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        String resource = client.resourceForProcess("native-light");
        var first = client.formal("default", resource, "admin");
        var second = client.formal("tenant-b", resource, "admin");
        var viewer = client.formal("default", resource, "viewer");
        client.verifiedClaims(first); client.verifiedClaims(second); client.verifiedClaims(viewer);
        long tenantA = Long.parseLong(first.getTenantId());
        long tenantB = Long.parseLong(second.getTenantId());
        if (tenantA == tenantB || !first.getResourceUri().equals(second.getResourceUri())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var probe = new NativeResourceHttpProbe("native-light", root, manifestFile, digest,
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
                        .connectTimeout(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue())).build(),
                Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()));
        var receipt = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/db-light.json"), root.resolve("evidence"), 128 * 1024));
        requireLightReceipt(manifest, digest, receipt);
        try (var connection = ownedLightConnection(manifest, receipt)) {
            String table = userTable(connection);
            if (!readRows(connection, table, tenantA, tenantB).isEmpty()) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            String nameA = "xingyuan-it-" + manifest.path("runId").asText() + "-a";
            String nameB = "xingyuan-it-" + manifest.path("runId").asText() + "-b";
            var bodyA = mapper.createObjectNode().put("externalId", "ext-1").put("name", nameA).put("email", nameA + "@xingyuan.invalid");
            var bodyB = mapper.createObjectNode().put("externalId", "ext-1").put("name", nameB).put("email", nameB + "@xingyuan.invalid");
            verifyLightConnection(connection, receipt);
            var createdA = probe.request("POST", "/api/users", first, bodyA, null, 200);
            verifyLightConnection(connection, receipt);
            var createdB = probe.request("POST", "/api/users", second, bodyB, null, 200);
            long idA = responseId(createdA), idB = responseId(createdB);
            var rows = readRows(connection, table, tenantA, tenantB);
            requireOwnedRows(rows, tenantA, tenantB, idA, idB, nameA, nameB);
            requireResponseMatchesSql(createdA, rows.stream().filter(row -> row.id() == idA).findFirst().orElseThrow());
            requireResponseMatchesSql(createdB, rows.stream().filter(row -> row.id() == idB).findFirst().orElseThrow());
            var readA = probe.request("GET", "/api/users/" + idA, first, null, null, 200);
            var readB = probe.request("GET", "/api/users/" + idB, second, null, null, 200);
            assertThat(readA).isEqualTo(createdA); assertThat(readB).isEqualTo(createdB);
            var forged = probe.request("GET", "/api/users/" + idA, first, null, second.getTenantId(), 200);
            assertThat(forged).isEqualTo(readA);
            var foreign = probe.request("GET", "/api/users/" + idA, second, null, null, 400);
            assertThat(foreign.path("success").isBoolean() && !foreign.path("success").booleanValue()).isTrue();
            assertThat(foreign.path("status").asText()).isEqualTo("USER_NOT_FOUND");
            probe.request("GET", "/api/users/" + idA, null, null, null, 401);
            verifyLightConnection(connection, receipt);
            probe.request("POST", "/api/users", viewer, bodyA, null, 403);
            assertThat(probe.request("GET", "/api/users/" + idA, first, null, null, 200)).isEqualTo(readA);
            assertThat(readRows(connection, table, tenantA, tenantB)).isEqualTo(rows);
            var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText())
                    .put("manifestSha256", digest).put("status", "PASSED").put("resourceUri", first.getResourceUri().toASCIIString())
                    .put("tenantA", first.getTenantId()).put("tenantB", second.getTenantId())
                    .put("tenantARowId", Long.toString(idA)).put("tenantBRowId", Long.toString(idB))
                    .put("defaultRowsHash", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(readA)))
                    .put("tenantBRowsHash", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(readB)))
                    .put("forgedHeaderRowsHash", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(forged)))
                    .put("sqlRowsSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(rows)))
                    .put("tokenASha256", first.getTokenSha256()).put("tokenBSha256", second.getTokenSha256())
                    .put("sqlIsolationConfirmed", true).put("observedAt", Instant.now().toString());
            client.publishNativeTenantIsolationProof(evidence, proof);
        }
    }

    static void requireLightReceipt(JsonNode manifest, String digest, JsonNode receipt) {
        if (!manifest.path("runId").asText().equals(receipt.path("runId").asText())
                || !digest.equals(receipt.path("manifestSha256").asText()) || !"light".equals(receipt.path("role").asText())
                || !"xingyuan_it_light_20261011".equals(receipt.path("databaseName").asText())
                || !receipt.path("oid").isIntegralNumber() || !receipt.path("oid").canConvertToLong() || receipt.path("oid").longValue() <= 0
                || !receipt.path("databaseOwner").asText().matches("[A-Za-z_][A-Za-z0-9_-]{0,62}")
                || !("xingyuan-it:" + manifest.path("runId").asText() + ":" + digest).equals(receipt.path("ownerComment").asText())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    static Connection ownedLightConnection(JsonNode manifest, JsonNode receipt) throws Exception {
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(Path.of(manifest.path("postgres").path("credentialFile").asText()), null, 4096);
        var credentials = new Properties();
        credentials.setProperty("user", receipt.path("databaseOwner").asText());
        try {
            String password = new String(bytes, StandardCharsets.UTF_8);
            if (password.endsWith("\r\n")) { password = password.substring(0, password.length() - 2); }
            else if (password.endsWith("\n")) { password = password.substring(0, password.length() - 1); }
            if (password.isEmpty()) { throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID"); }
            credentials.setProperty("password", password);
            var connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/xingyuan_it_light_20261011?connectTimeout=5&socketTimeout=30", credentials);
            try {
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);
                connection.setReadOnly(true);
                verifyLightConnection(connection, receipt);
                return connection;
            } catch (java.sql.SQLException | RuntimeException failure) {
                try { connection.close(); } catch (java.sql.SQLException closeFailure) { failure.addSuppressed(closeFailure); }
                throw failure;
            }
        } finally { java.util.Arrays.fill(bytes, (byte) 0); credentials.clear(); }
    }

    static void verifyLightConnection(Connection connection, JsonNode receipt) throws java.sql.SQLException {
        try (var query = connection.prepareStatement("""
                SELECT current_database(), inet_server_addr()::text, inet_server_port(), d.oid::bigint,
                       obj_description(d.oid,'pg_database'), pg_get_userbyid(d.datdba),
                       current_setting('transaction_read_only'), current_setting('transaction_isolation')
                FROM pg_database d WHERE d.datname=current_database()
                """)) {
            query.setQueryTimeout(5);
            try (var result = query.executeQuery()) {
                if (!result.next() || !"xingyuan_it_light_20261011".equals(result.getString(1)) || !"127.0.0.1".equals(result.getString(2))
                        || result.getInt(3) != 5432 || result.getLong(4) != receipt.path("oid").longValue()
                        || !receipt.path("ownerComment").asText().equals(result.getString(5))
                        || !receipt.path("databaseOwner").asText().equals(result.getString(6))
                        || !"on".equals(result.getString(7)) || !"read committed".equals(result.getString(8)) || result.next()) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            }
        }
    }

    static String userTable(Connection connection) throws java.sql.SQLException {
        try (var query = connection.prepareStatement("SELECT n.nspname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relname='users' AND c.relkind='r' AND n.nspname NOT LIKE 'pg_%' AND n.nspname<>'information_schema'")) {
            query.setQueryTimeout(5);
            try (var result = query.executeQuery()) {
                if (!result.next()) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
                String schema = result.getString(1);
                if (!schema.matches("[a-z][a-z0-9_]{0,62}") || result.next()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                return '"' + schema + "\".\"users\"";
            }
        }
    }

    static List<NativeSqlRowVO> readRows(Connection connection, String table, long tenantA, long tenantB) throws java.sql.SQLException {
        if (!table.matches("\"[a-z][a-z0-9_]{0,62}\"\\.\"users\"")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        try (var query = connection.prepareStatement("SELECT id,tenant_id,external_id,name,email,status FROM " + table + " WHERE tenant_id IN (?,?) AND external_id=? AND deleted_at IS NULL ORDER BY id LIMIT 3")) {
            query.setQueryTimeout(5); query.setLong(1, tenantA); query.setLong(2, tenantB); query.setString(3, "ext-1");
            try (var result = query.executeQuery()) {
                var rows = new ArrayList<NativeSqlRowVO>();
                while (result.next()) { rows.add(new NativeSqlRowVO(result.getLong(1), result.getLong(2), result.getString(3), result.getString(4), result.getString(5), result.getString(6))); }
                return List.copyOf(rows);
            }
        }
    }

    static long responseId(JsonNode value) {
        if (!value.path("id").isTextual() || !value.path("id").asText().matches("[1-9][0-9]{0,18}")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return Long.parseLong(value.path("id").asText());
    }

    static void requireResponseMatchesSql(JsonNode response, NativeSqlRowVO row) {
        assertThat(responseId(response)).isEqualTo(row.id());
        assertThat(response.path("name").asText()).isEqualTo(row.name());
        assertThat(response.path("email").asText()).isEqualTo(row.email());
        assertThat(response.path("status").asText()).isEqualTo(row.status());
    }

    static void requireEvidenceDirectory(Path root, Path evidence) throws Exception {
        if (!evidence.equals(root.resolve("evidence")) && (!root.resolve("evidence").equals(evidence.getParent())
                || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        for (Path directory = evidence; directory != null && directory.startsWith(root); directory = directory.getParent()) {
            if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory) || !Files.getOwner(root).equals(Files.getOwner(directory))
                    || !Files.getPosixFilePermissions(directory).equals(PosixFilePermissions.fromString("rwx------"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
    }

    static Properties verifiedProfile(Path root, JsonNode manifest, String digest, String role) throws Exception {
        if (!java.util.Set.of("native-light", "native-web", "native-service", "native-agent", "yuheng").contains(role)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var ready = UnifiedIdentityLiveClient.strictJson().readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/profiles-ready.json"), root.resolve("evidence"), 128 * 1024));
        Path profile = root.resolve("profiles/" + role + ".properties");
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(profile, root.resolve("profiles"), 2 * 1024 * 1024);
        if (!manifest.path("runId").asText().equals(ready.path("runId").asText()) || !digest.equals(ready.path("manifestSha256").asText())
                || !ready.path("schemaVersion").isIntegralNumber() || !ready.path("schemaVersion").canConvertToInt() || ready.path("schemaVersion").intValue() != 1
                || !profile.toString().equals(ready.path("profiles").path(role).path("path").asText())
                || !UnifiedIdentityLiveClient.sha256(bytes).equals(ready.path("profiles").path(role).path("sha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var properties = new Properties();
        try { properties.load(new java.io.StringReader(new String(bytes, StandardCharsets.UTF_8))); }
        finally { java.util.Arrays.fill(bytes, (byte) 0); }
        return properties;
    }

    static void requireRunningProcesses(Path root, Path manifestFile, JsonNode manifest, String digest, Path evidence, java.util.Set<String> expected) throws Exception {
        if (expected.isEmpty() || !java.util.Set.of("shoubing", "jianshen", "edge", "tianshu", "yuheng", "biz", "mcp", "llm", "native-light", "native-web", "native-service", "native-agent").containsAll(expected)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        Path output = evidence.resolve("native-sql-status-" + UUID.randomUUID() + ".json");
        var command = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText()).resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "process-state", "status", manifestFile.toString(), output.toString()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!command.waitFor(manifest.path("deadlines").path("operationSeconds").intValue(), TimeUnit.SECONDS) || command.exitValue() != 0) { throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE"); }
        } finally { if (command.isAlive()) { command.destroy(); command.waitFor(5, TimeUnit.SECONDS); } }
        var observed = UnifiedIdentityLiveClient.strictJson().readTree(UnifiedIdentityLiveClient.protectedBytes(output, root.resolve("evidence"), 128 * 1024));
        if (!manifest.path("runId").asText().equals(observed.path("runId").asText()) || !digest.equals(observed.path("manifestSha256").asText())
                || !"CLI-008".equals(observed.path("operationId").asText()) || !observed.path("targets").isArray()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        for (String role : expected) {
            var targets = java.util.stream.StreamSupport.stream(observed.path("targets").spliterator(), false)
                    .filter(value -> role.equals(value.path("targetKey").asText())).toList();
            if (targets.size() != 1 || !"RUNNING".equals(targets.getFirst().path("state").asText())) { throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE"); }
        }
    }

    @RequiredArgsConstructor
    @Slf4j
    static final class NativeResourceHttpProbe {
        private final String role;
        private final Path root;
        private final Path manifestFile;
        private final String digest;
        private final HttpClient http;
        private final Duration timeout;

        JsonNode observedGraphQl(FormalReferenceDTO reference, JsonNode body, String tenantHeader, int callbacks, int matched,
                com.fasterxml.jackson.databind.node.ArrayNode observations) throws Exception {
            if (!java.util.Set.of("native-light", "native-web").contains(role) || reference == null) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var mapper = UnifiedIdentityLiveClient.strictJson(); byte[] raw = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024);
            if (!digest.equals(UnifiedIdentityLiveClient.sha256(raw))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var manifest = mapper.readTree(raw); Path evidence = root.resolve("evidence"); requireEvidenceDirectory(root, evidence);
            var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, role, reference.getRealmCode(), "GRAPHQL_CONTEXT_COUNTS", null);
            var actual = request("POST", "/graphql", reference, body, tenantHeader, 200);
            var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, role, reference.getRealmCode(), "GRAPHQL_CONTEXT_COUNTS", null);
            requireGraphQlAsyncDelta(before, after, callbacks, matched);
            var item = observations.addObject().put("realmCode", reference.getRealmCode()).put("responseSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(actual)))
                    .put("callbacks", callbacks).put("matched", matched);
            item.set("before", before); item.set("after", after); return actual;
        }

        JsonNode request(String method, String path, FormalReferenceDTO reference, JsonNode body, String tenantHeader, int expected) throws Exception {
            return request(method, path, reference, body, tenantHeader, expected, null);
        }

        JsonNode mcpOwnerRequest(String method, String path, FormalReferenceDTO reference, JsonNode body, String idempotencyKey, int expected) throws Exception {
            if (!"yuheng".equals(role) || !mcpOwnerPath(method, path) || idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{1,128}")) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            return request(method, path, reference, body, null, expected, null, idempotencyKey);
        }

        JsonNode request(String method, String path, FormalReferenceDTO reference, JsonNode body, String tenantHeader, int expected,
                java.util.function.Consumer<HttpAuditObservationVO> observations) throws Exception {
            return request(method, path, reference, body, tenantHeader, expected, observations, null);
        }

        private JsonNode request(String method, String path, FormalReferenceDTO reference, JsonNode body, String tenantHeader, int expected,
                java.util.function.Consumer<HttpAuditObservationVO> observations, String idempotencyKey) throws Exception {
            if (!digest.equals(UnifiedIdentityLiveClient.sha256(UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024)))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var builder = HttpRequest.newBuilder(nativeResourceEndpoint(role, method, path)).timeout(timeout).header("X-Request-Id", UUID.randomUUID().toString());
            String trace = UUID.randomUUID().toString().replace("-", ""); Instant started = Instant.now();
            if (observations != null) { builder.header("traceparent", "00-" + trace + '-' + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01"); }
            if (reference != null) {
                byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(reference.getTokenFile(), root.resolve("secrets"), 128 * 1024);
                try {
                    if (!reference.getExpiresAt().isAfter(Instant.now()) || !reference.getTokenSha256().equals(UnifiedIdentityLiveClient.sha256(bytes))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    builder.header("Authorization", "Bearer " + new String(bytes, StandardCharsets.UTF_8));
                } finally { java.util.Arrays.fill(bytes, (byte) 0); }
            }
            if (tenantHeader != null) { builder.header("X-Tenant-Id", tenantHeader); }
            if (idempotencyKey != null) { builder.header("Idempotency-Key", idempotencyKey); }
            if (method.equals("POST")) { builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())); }
            else if (method.equals("PUT")) { builder.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(body.toString())); }
            else { builder.GET(); }
            var captured = new ByteArrayOutputStream();
            var response = http.send(builder.build(), boundedBody(captured));
            assertThat(response.statusCode()).isEqualTo(expected);
            if (observations != null) { observations.accept(observedHttpCall(response, trace, started)); }
            return UnifiedIdentityLiveClient.strictJson().readTree(captured.toByteArray());
        }
    }

    static void requireGraphQlAsyncDelta(JsonNode before, JsonNode after, long callbacks, long matched) {
        if (callbacks < 1 || callbacks > 2 || matched < 0 || matched > callbacks) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        for (var value : List.of(before, after)) {
            var fields = new java.util.HashSet<String>(); value.path("counts").fieldNames().forEachRemaining(fields::add);
            if (!fields.equals(java.util.Set.of("differentThread", "matchedContext", "finallyRestored", "failure"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            for (String key : fields) { transactionProofLong(value.path("counts"), key); }
            if (transactionProofLong(value.path("counts"), "failure") != 0
                    || transactionProofLong(value.path("counts"), "matchedContext") > transactionProofLong(value.path("counts"), "differentThread")
                    || transactionProofLong(value.path("counts"), "finallyRestored") != transactionProofLong(value.path("counts"), "differentThread")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        for (String key : List.of("differentThread", "matchedContext", "finallyRestored")) {
            long expected = key.equals("matchedContext") ? matched : callbacks;
            if (Math.subtractExact(transactionProofLong(after.path("counts"), key), transactionProofLong(before.path("counts"), key)) != expected) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
    }

    static void requireGraphQlAsyncEvidence(JsonNode value, String role) {
        if (!java.util.Set.of("native-light", "native-web").contains(role) || !value.path("asyncObservations").isArray()
                || value.path("asyncObservations").size() != 3) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var seen = new java.util.HashSet<String>();
        for (var observation : value.path("asyncObservations")) {
            String realm = observation.path("realmCode").asText(); long matched = transactionProofLong(observation, "matched");
            long callbacks = transactionProofLong(observation, "callbacks");
            if (!java.util.Set.of("default", "tenant-b").contains(realm) || !seen.add(realm + ':' + matched)
                    || !observation.path("responseSha256").asText().matches("[0-9a-f]{64}")
                    || matched != 0 && matched != 2 || callbacks != (matched == 0 && role.equals("native-web") ? 1 : 2)) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            requireGraphQlAsyncDelta(observation.path("before"), observation.path("after"), callbacks, matched);
        }
        if (!seen.equals(java.util.Set.of("default:2", "tenant-b:2", "default:0"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    @Test
    void graphQlAsyncProofRequiresActualWorkerMatchedScopeAndFinallyCounts() throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        var before = mapper.readTree("{\"counts\":{\"differentThread\":10,\"matchedContext\":7,\"finallyRestored\":10,\"failure\":0}}");
        var after = mapper.readTree("{\"counts\":{\"differentThread\":12,\"matchedContext\":9,\"finallyRestored\":12,\"failure\":0}}");
        assertThatCode(() -> requireGraphQlAsyncDelta(before, after, 2, 2)).doesNotThrowAnyException();
        for (String key : List.of("differentThread", "matchedContext", "finallyRestored")) {
            var invalid = after.deepCopy(); invalid.withObject("counts").put(key, before.path("counts").path(key).longValue());
            assertThatThrownBy(() -> requireGraphQlAsyncDelta(before, invalid, 2, 2)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        var leaked = after.deepCopy(); leaked.withObject("counts").put("failure", 1);
        assertThatThrownBy(() -> requireGraphQlAsyncDelta(before, leaked, 2, 2)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var sensitive = after.deepCopy(); sensitive.withObject("counts").put("subject", "forbidden");
        assertThatThrownBy(() -> requireGraphQlAsyncDelta(before, sensitive, 2, 2)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var denied = after.deepCopy(); denied.withObject("counts").put("matchedContext", 7);
        assertThatCode(() -> requireGraphQlAsyncDelta(before, denied, 2, 0)).doesNotThrowAnyException();
    }

    static URI nativeResourceEndpoint(String role, String method, String path) {
        if ("tianshu".equals(role) && tianshuConfigOwnerPath(method, path)) { return URI.create("https://127.0.0.1:28150" + path); }
        if ("biz".equals(role) && "GET".equals(method) && java.util.Set.of("/api/v1/yuheng/admin/scopes", "/api/v1/yuheng/admin/scopes/__xingyuan-required-scope").contains(path)) {
            return URI.create("https://127.0.0.1:28180" + path);
        }
        if ("yuheng".equals(role) && mcpOwnerPath(method, path)) { return URI.create("https://127.0.0.1:28140" + path); }
        if ("yuheng".equals(role) && knowledgeManagementPath(method, path)) { return URI.create("https://127.0.0.1:28140" + path); }
        if ("yuheng".equals(role) && gatewayOwnerPath(method, path)) { return URI.create("https://127.0.0.1:28140" + path); }
        if ("llm".equals(role)) {
            if (!("GET".equals(method) && "/v1/models".equals(path) || "POST".equals(method)
                    && java.util.Set.of("/v1/chat/completions", "/v1/embeddings", "/v1/responses", "/v1/messages").contains(path))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            return URI.create("https://127.0.0.1:28190" + path);
        }
        if (PLATFORM_READ_PATHS.containsKey(role) && "GET".equals(method) && PLATFORM_READ_PATHS.get(role).equals(path)) {
            int port = switch (role) { case "shoubing" -> 28120; case "jianshen" -> 28130; case "yuheng" -> 28140; case "tianshu" -> 28150; default -> throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); };
            return URI.create("https://127.0.0.1:" + port + path);
        }
        if ("native-agent".equals(role)) {
            String prefix = "/api/v1/knowledge-bases";
            if (!("POST".equals(method) && prefix.equals(path) || "GET".equals(method)
                    && (path.matches(prefix + "/[1-9][0-9]{0,18}") || path.matches(prefix + "\\?page=1&size=100&keyword=[A-Za-z0-9._-]{2,64}")))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            return URI.create("https://127.0.0.1:28313" + path);
        }
        String prefix; int port;
        if ("native-light".equals(role)) { prefix = "/api/users"; port = 28310; }
        else if ("native-web".equals(role)) { prefix = "/api/v1/users"; port = 28311; }
        else { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        if (!("POST".equals(method) && (prefix.equals(path) || "/graphql".equals(path)) || "GET".equals(method) && path.matches(prefix + "/[1-9][0-9]{0,18}"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return URI.create("https://127.0.0.1:" + port + path);
    }

    static boolean gatewayOwnerPath(String method, String path) {
        if (path == null || path.length() > 1024 || path.contains("..") || path.contains("\\") || path.contains("#")) { return false; }
        String base = "/api/v1/yuheng/admin", id = "[A-Za-z0-9_-]{1,128}";
        if (method.equals("GET")) {
            return path.equals(base + "/yuheng-groups") || path.equals(base + "/applications")
                    || path.matches(base + "/applications/" + id + "/catalog") || path.matches(base + "/operations/" + id)
                    || path.matches(base + "/releases/" + id) || path.matches(base + "/yuheng-groups/" + id + "/(?:draft|runtime-consistency|engine-nodes)");
        }
        if (method.equals("POST")) { return path.equals(base + "/yuheng-groups") || path.matches(base + "/yuheng-groups/" + id + "/releases"); }
        return method.equals("PUT") && path.matches(base + "/yuheng-groups/" + id + "/draft/(?:policies/it-(?:read|mcp)-security|routes/it-protected-read)");
    }

    static boolean mcpOwnerPath(String method, String path) {
        if (path == null || path.length() > 1024 || path.contains("..") || path.contains("\\") || path.contains("#")) { return false; }
        String base = "/api/v1/yuheng/admin/mcp", id = "[A-Za-z0-9_-]{1,128}";
        if ("GET".equals(method)) { return path.matches(base + "/servers\\?gatewayGroupId=" + id) || path.matches(base + "/servers/" + id)
                || path.matches(base + "/servers/" + id + "/resources\\?gatewayGroupId=" + id) || path.matches(base + "/servers/" + id + "/capability-preview"); }
        if ("POST".equals(method)) { return path.equals(base + "/servers") || path.matches(base + "/servers/" + id + "/resources"); }
        return "PUT".equals(method) && (path.matches(base + "/servers/" + id) || path.matches(base + "/resources/" + id));
    }

    @Test
    void mcpOwnerBoundaryOnlyAllowsTheOriginalServerAndResourceMethods() {
        assertThat(mcpOwnerPath("GET", "/api/v1/yuheng/admin/mcp/servers?gatewayGroupId=group-1")).isTrue();
        assertThat(mcpOwnerPath("POST", "/api/v1/yuheng/admin/mcp/servers")).isTrue();
        assertThat(mcpOwnerPath("POST", "/api/v1/yuheng/admin/mcp/servers/server-1/resources")).isTrue();
        for (String path : List.of("/api/v1/yuheng/admin/mcp/servers?gatewayGroupId=group-1&foreign=true", "/api/v1/yuheng/admin/mcp/servers/server-1/tools", "/api/v1/yuheng/admin/mcp/servers/../foreign")) {
            assertThat(mcpOwnerPath("POST", path)).isFalse(); assertThat(mcpOwnerPath("GET", path)).isFalse();
        }
        assertThat(mcpOwnerPath("DELETE", "/api/v1/yuheng/admin/mcp/servers/server-1")).isFalse();
    }

    static boolean tianshuConfigOwnerPath(String method, String path) {
        if (path == null || path.length() > 1024 || path.contains("..") || path.contains("\\") || path.contains("#")) { return false; }
        String base = "/api/v1/tianshu", id = "[1-9][0-9]{0,18}", code = "[A-Za-z0-9._-]{1,128}";
        return "GET".equals(method) && (path.matches(base + "/configs\\?bizCode=" + code + "&env=local&appCode=" + code + "&includeDeleted=false") || path.matches(base + "/publish-tasks/" + id)
                || path.matches(base + "/configs/" + id + "/versions/page\\?pageNo=1&pageSize=1"))
                || "PUT".equals(method) && path.matches(base + "/configs/" + id) || "POST".equals(method) && path.matches(base + "/configs/" + id + "/publish");
    }

    static boolean knowledgeManagementPath(String method, String path) {
        if (path == null) { return false; }
        String base = "/api/v1/yuheng/admin/knowledge-bases";
        return method.equals("GET") && (path.equals(base + "?page=1&size=100") || path.matches(base + "/[1-9][0-9]{0,18}") || path.equals("/api/v1/yuheng/admin/llm/models"))
                || method.equals("POST") && path.equals(base);
    }

    @Test
    void gatewayOwnerCallsStayOnTheOriginalTaskManagementMethodsAndPaths() {
        String base = "/api/v1/yuheng/admin";
        assertThat(nativeResourceEndpoint("yuheng", "GET", base + "/applications/123/catalog").toASCIIString()).isEqualTo("https://127.0.0.1:28140" + base + "/applications/123/catalog");
        assertThat(nativeResourceEndpoint("yuheng", "PUT", base + "/yuheng-groups/123/draft/policies/it-read-security").getPort()).isEqualTo(28140);
        assertThat(nativeResourceEndpoint("yuheng", "POST", base + "/yuheng-groups/123/releases").getHost()).isEqualTo("127.0.0.1");
        for (String path : List.of("https://foreign.invalid" + base, base + "/yuheng-groups/../draft", base + "/yuheng-groups/123/draft/policies/other-policy", base + "/operations/123/manual-definition")) {
            assertThatThrownBy(() -> nativeResourceEndpoint("yuheng", "PUT", path)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        assertThatThrownBy(() -> nativeResourceEndpoint("yuheng", "DELETE", base + "/yuheng-groups/123")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        String config = "/api/v1/tianshu/configs";
        assertThat(nativeResourceEndpoint("tianshu", "GET", config + "?bizCode=owned&env=local&appCode=owned-gateway&includeDeleted=false").getPort()).isEqualTo(28150);
        assertThat(nativeResourceEndpoint("tianshu", "PUT", config + "/123").getPort()).isEqualTo(28150);
        assertThat(nativeResourceEndpoint("tianshu", "POST", config + "/123/publish").getPort()).isEqualTo(28150);
        assertThat(nativeResourceEndpoint("tianshu", "GET", config + "/123/versions/page?pageNo=1&pageSize=1").getPort()).isEqualTo(28150);
        for (String path : List.of(config, config + "/123/rollback", config + "?bizCode=owned&env=prod&appCode=owned&includeDeleted=false", config + "/../other", "/api/v1/tianshu/publish-tasks/123/retry")) {
            assertThatThrownBy(() -> nativeResourceEndpoint("tianshu", "POST", path)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        assertThatThrownBy(() -> nativeResourceEndpoint("tianshu", "DELETE", config + "/123")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    static void requireLlmSecurityEnvelope(JsonNode response, String path, int status) {
        String code = status == 401 ? "authentication_error" : "permission_denied", type = status == 401 ? "authentication_error" : "permission_error";
        if (!response.isObject() || !response.path("error").isObject() || !type.equals(response.path("error").path("type").asText())
                || !response.path("error").path("message").isTextual() || response.path("error").path("message").asText().isBlank()
                || !path.equals("/v1/messages") && !code.equals(response.path("error").path("code").asText())
                || path.equals("/v1/messages") && !"error".equals(response.path("type").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void formalPublicFamilyRevocationRejectsTheSameUnexpiredTokenBeforeBusiness() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] raw = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(raw);
        String digest = UnifiedIdentityLiveClient.sha256(raw); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        var first = client.formal("default", client.resourceForProcess("native-light"), "admin"); var second = client.formal("tenant-b", first.getResourceKey(), "admin");
        client.bindNativeTenantReadEndpoints(first, second); var verified = client.verifiedClaims(first);
        if (!first.getExpiresAt().isAfter(Instant.now().plusSeconds(30))) { throw new IllegalStateException("XINGYUAN_INPUT_EXPIRED"); }
        var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
        if (client.read(first) != 200) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var successful = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
        UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(before, successful, "get", 1);
        client.awaitFormalRevocation(first, verified);
        var deniedBefore = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
        if (!first.getExpiresAt().isAfter(Instant.now()) || client.read(first) != 401 || !first.getExpiresAt().isAfter(Instant.now())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var deniedAfter = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
        if (!deniedBefore.equals(deniedAfter)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        Path receipt = evidence.resolve("formal-revocation-" + first.getTokenSha256() + ".json");
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "ORIGINAL_PUBLIC_CLI_FAMILY_REVOKE_AND_ZERO_BUSINESS_ONLY").put("status", "PASSED").put("tokenReference", first.getTokenSha256())
                .put("tokenExpiresAt", first.getExpiresAt().toString()).put("beforeStatus", 200).put("afterStatus", 401)
                .put("ownerReceipt", root.relativize(receipt).toString()).put("ownerReceiptSha256", UnifiedIdentityLiveClient.sha256(UnifiedIdentityLiveClient.protectedBytes(receipt, evidence, 128 * 1024)))
                .put("recordedAt", Instant.now().toString());
        proof.set("before", before); proof.set("successful", successful); proof.set("deniedBefore", deniedBefore); proof.set("deniedAfter", deniedAfter);
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-formal-family-revocation.json"), proof);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualLlmProtocolsRejectMissingAudienceAndScopeBeforeInference() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(bytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText()); String digest = UnifiedIdentityLiveClient.sha256(bytes);
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("llm", "shoubing", "edge"));
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "ACTUAL_LLM_PROTOCOL_IDENTITY_SCOPE_GUARDS_ONLY"); var observations = proof.putArray("observations");
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var api = new NativeResourceHttpProbe("llm", root, manifestFile, digest, http, timeout);
            for (String realm : List.of("default", "tenant-b")) {
                var full = client.formal(realm, client.resourceForLlm(), "admin"); client.verifiedClaims(full);
                if (!full.getScopes().containsAll(java.util.Set.of("yuheng:llm:read", "yuheng:llm:invoke"))) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
                var foreign = client.formal(realm, client.resourceForAlias("edge-yuheng"), "admin"); client.verifiedClaims(foreign);
                var restricted = acquireIdentityOnlyProbe(client, root, evidence, manifestFile, manifest, digest, realm, full.getResourceKey());
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "llm", realm, "BUSINESS_COUNTS", null);
                var models = api.request("GET", "/v1/models", full, null, null, 200);
                if (!"list".equals(models.path("object").asText()) || !models.path("data").isArray()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var baseline = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "llm", realm, "BUSINESS_COUNTS", null);
                UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(before, baseline, "listLlmModels", 1);
                var results = mapper.createArrayNode();
                for (String path : List.of("/v1/models", "/v1/chat/completions", "/v1/embeddings", "/v1/responses", "/v1/messages")) {
                    String method = path.equals("/v1/models") ? "GET" : "POST";
                    var body = mapper.createObjectNode().put("model", "scope-probe").put("stream", false);
                    requireLlmSecurityEnvelope(api.request(method, path, null, body, null, 401), path, 401);
                    requireLlmSecurityEnvelope(api.request(method, path, foreign, body, null, 401), path, 401);
                    requireLlmSecurityEnvelope(api.request(method, path, restricted, body, null, 403), path, 403);
                    results.addObject().put("path", path).put("bareStatus", 401).put("foreignAudienceStatus", 401).put("missingScopeStatus", 403);
                }
                var denied = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "llm", realm, "BUSINESS_COUNTS", null);
                if (!baseline.equals(denied)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                api.request("GET", "/v1/models", full, null, null, 200);
                var recovered = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "llm", realm, "BUSINESS_COUNTS", null);
                UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(denied, recovered, "listLlmModels", 1);
                var item = observations.addObject().put("realmCode", realm).put("tokenReference", full.getTokenSha256()).put("identityOnlyTokenReference", restricted.getTokenSha256())
                        .put("catalogStatus", 200).put("externalInferenceExecuted", false);
                item.set("before", before); item.set("afterCatalog", baseline); item.set("afterDenied", denied); item.set("recovered", recovered); item.set("protocols", results);
            }
        }
        proof.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-llm-protocol-guards.json"), proof);
    }

    @Test
    void llmTargetsAndErrorsPreserveAllFiveOriginalProtocolPaths() {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        assertThat(nativeResourceEndpoint("llm", "GET", "/v1/models").toASCIIString()).isEqualTo("https://127.0.0.1:28190/v1/models");
        assertThatThrownBy(() -> nativeResourceEndpoint("llm", "POST", "/v1/models")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> nativeResourceEndpoint("llm", "GET", "https://foreign.invalid/v1/models")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        var error = mapper.createObjectNode().put("type", "error"); error.putObject("error").put("type", "permission_error").put("message", "Denied").put("code", "permission_denied");
        assertThatCode(() -> requireLlmSecurityEnvelope(error, "/v1/messages", 403)).doesNotThrowAnyException();
        error.remove("type"); assertThatThrownBy(() -> requireLlmSecurityEnvelope(error, "/v1/messages", 403)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatCode(() -> requireLlmSecurityEnvelope(error, "/v1/chat/completions", 403)).doesNotThrowAnyException();
        error.withObject("error").put("type", "permission_denied");
        assertThatThrownBy(() -> requireLlmSecurityEnvelope(error, "/v1/chat/completions", 403)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    static TaskSpoolFault createTaskSpoolFault(Path root, Path directory, String kind, long maximumBytes) throws Exception {
        if (!root.isAbsolute() || !root.equals(root.normalize()) || !directory.equals(root.resolve("evidence/oauth-spool-yuheng"))
                || !java.util.Set.of("DEAD", "CAPACITY").contains(kind) || maximumBytes <= 0 || maximumBytes > 104857600) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        requirePrivateSpoolDirectory(root, directory);
        Object directoryKey = Files.readAttributes(directory, java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS).fileKey();
        Path path = directory.resolve(".xingyuan-task-fault-" + UUID.randomUUID() + (kind.equals("DEAD") ? ".dead" : ".capacity"));
        long size = kind.equals("DEAD") ? 1 : maximumBytes;
        try (var channel = java.nio.channels.FileChannel.open(path, java.util.Set.of(java.nio.file.StandardOpenOption.CREATE_NEW,
                java.nio.file.StandardOpenOption.WRITE, java.nio.file.LinkOption.NOFOLLOW_LINKS),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
            channel.position(size - 1); channel.write(java.nio.ByteBuffer.wrap(new byte[] {0})); channel.force(true);
        }
        requirePrivateSpoolDirectory(root, directory);
        if (directoryKey == null || !directoryKey.equals(Files.readAttributes(directory, java.nio.file.attribute.BasicFileAttributes.class,
                java.nio.file.LinkOption.NOFOLLOW_LINKS).fileKey())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var attributes = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.fileKey() == null || attributes.size() != size
                || !Files.getOwner(root).equals(Files.getOwner(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                || !Files.getPosixFilePermissions(path, java.nio.file.LinkOption.NOFOLLOW_LINKS).equals(PosixFilePermissions.fromString("rw-------"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        try (var parent = java.nio.channels.FileChannel.open(directory, java.nio.file.StandardOpenOption.READ)) { parent.force(true); }
        return new TaskSpoolFault(root, directoryKey, path, attributes.fileKey(), size, attributes.lastModifiedTime());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualYuhengSpoolFaultsFailClosedAndRecoverWithoutEnteringBusiness() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] manifestBytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(manifestBytes);
        String digest = UnifiedIdentityLiveClient.sha256(manifestBytes); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        requireEvidenceDirectory(root, evidence);
        var profile = verifiedProfile(root, manifest, digest, "yuheng"); var flat = new java.util.LinkedHashMap<String, Object>();
        profile.forEach((key, value) -> flat.put(key.toString(), value));
        var audit = new org.springframework.boot.context.properties.bind.Binder(new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(flat))
                .bind("egon.cola.platform.tianquan.shoubing.audit", top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties.class)
                .orElseThrow(() -> new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"));
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(audit);
        }
        if (!root.resolve("evidence/oauth-spool-yuheng").equals(audit.spoolDirectory()) || audit.spoolMaxBytes() > 104857600
                || !Path.of(manifest.path("secretFiles").path("audit-hmac").asText()).equals(audit.hmacKeyFile())
                || !("xingyuan-it:" + manifest.path("runId").asText() + ":oauth:audit:v1").equals(audit.stream())
                || !"true".equals(profile.getProperty("egon.cola.platform.tianquan.shoubing.usage-observation.enabled"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        requirePrivateSpoolDirectory(root, audit.spoolDirectory());
        var reference = client.formal("default", client.resourceForAlias("edge-yuheng"), "admin"); client.verifiedClaims(reference);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "shoubing", "edge"));
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "YUHENG_TASK_SPOOL_IO_READINESS_AND_RECOVERY").put("tokenReference", reference.getTokenSha256())
                .put("resourceUri", reference.getResourceUri().toASCIIString()); var observations = proof.putArray("observations");
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var api = new NativeResourceHttpProbe("yuheng", root, manifestFile, digest, http, timeout);
            for (String kind : List.of("DEAD", "CAPACITY")) {
                awaitYuhengReadiness(http, timeout, true);
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
                api.request("GET", "/api/v1/yuheng/admin/scopes", reference, null, null, 200);
                var baseline = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
                if (Math.addExact(before.path("counts").path("list").longValue(), 1) != baseline.path("counts").path("list").longValue()) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
                // An owned IO marker exercises the actual receiver. Signed DEAD receipts have a separate original-frame test.
                try (var fault = createTaskSpoolFault(root, audit.spoolDirectory(), kind, audit.spoolMaxBytes())) {
                    XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-spool-fault-" + UUID.randomUUID() + ".json"),
                            mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                                    .put("kind", kind).put("ownedFile", root.relativize(fault.path).toString()).put("logicalSize", fault.size).put("recordedAt", Instant.now().toString()));
                    awaitYuhengReadiness(http, timeout, false);
                    var denied = api.request("GET", "/api/v1/yuheng/admin/scopes", reference, null, null, 503);
                    if (!"OAUTH_AUDIT_DELIVERY_UNAVAILABLE".equals(denied.path("code").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
                    if (!baseline.equals(after)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    var observation = observations.addObject().put("kind", kind).put("readinessStatus", 503).put("businessStatus", 503);
                    observation.set("before", baseline); observation.set("after", after);
                }
                awaitYuhengReadiness(http, timeout, true);
                api.request("GET", "/api/v1/yuheng/admin/scopes", reference, null, null, 200);
                var recovered = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
                if (Math.addExact(baseline.path("counts").path("list").longValue(), 1) != recovered.path("counts").path("list").longValue()) {
                    throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
                }
                ((com.fasterxml.jackson.databind.node.ObjectNode) observations.get(observations.size() - 1)).put("recoveryReadinessStatus", 200).set("recovered", recovered);
            }
        }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "shoubing", "edge"));
        if (!digest.equals(UnifiedIdentityLiveClient.sha256(UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024)))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        proof.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-spool-readiness.json"), proof);
    }

    private static void awaitYuhengReadiness(HttpClient http, Duration timeout, boolean expected) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            var body = new ByteArrayOutputStream();
            var response = http.send(HttpRequest.newBuilder(URI.create("https://127.0.0.1:28140/actuator/health/readiness"))
                    .GET().timeout(Duration.ofNanos(Math.max(1, deadline - System.nanoTime()))).build(), boundedBody(body));
            var value = UnifiedIdentityLiveClient.strictJson().readTree(body.toByteArray());
            if (response.headers().firstValue("Location").isPresent() || response.headers().firstValue("Set-Cookie").isPresent()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            if (response.statusCode() == (expected ? 200 : 503) && (expected ? "UP" : "DOWN").equals(value.path("status").asText())) { return; }
            Thread.sleep(100);
        }
        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
    }

    static long securityMetricCount(JsonNode snapshot, String signal, String category) {
        var matches = java.util.stream.StreamSupport.stream(snapshot.path("samples").spliterator(), false)
                .filter(value -> signal.equals(value.path("signal").asText()) && category.equals(value.path("result").asText())).toList();
        if (matches.size() > 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        return matches.isEmpty() ? 0 : transactionProofLong(matches.getFirst(), "count");
    }

    private static void awaitYuhengSpoolDrained(Path root, Duration timeout) throws Exception {
        Path directory = root.resolve("evidence/oauth-spool-yuheng"); long deadline = System.nanoTime() + timeout.toNanos();
        while (deadline - System.nanoTime() > 0) {
            requirePrivateSpoolDirectory(root, directory);
            try (var files = Files.list(directory)) {
                boolean pending = false; int count = 0;
                for (var path : files.toList()) {
                    if (++count > 4096 || Files.isSymbolicLink(path)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                    String name = path.getFileName().toString();
                    if (name.endsWith(".dead")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    if (name.endsWith(".evt") || name.endsWith(".sent")) { pending = true; }
                }
                if (!pending) { return; }
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualYuhengSharedStateIoUnavailabilityAndFixedMetricsRecover() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(bytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText()); String digest = UnifiedIdentityLiveClient.sha256(bytes);
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        var profile = verifiedProfile(root, manifest, digest, "yuheng");
        if (!"STRICT_SHARED".equals(profile.getProperty("egon.cola.platform.tianquan.shoubing.revocation.mode")) || timeout.compareTo(Duration.ofSeconds(20)) < 0) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        profile.clear(); var reference = client.formal("default", client.resourceForAlias("edge-yuheng"), "admin"); client.verifiedClaims(reference);
        if (!reference.getExpiresAt().isAfter(Instant.now().plusSeconds(90))) { throw new IllegalStateException("XINGYUAN_INPUT_EXPIRED"); }
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "YUHENG_ACTUAL_SHARED_STATE_IO_AND_METRICS_ONLY").put("tokenReference", reference.getTokenSha256()).put("tokenExpiresAt", reference.getExpiresAt().toString());
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var api = new NativeResourceHttpProbe("yuheng", root, manifestFile, digest, http, timeout);
            api.request("GET", PLATFORM_READ_PATHS.get("yuheng"), reference, null, null, 200);
            var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "SECURITY_METRICS", null);
            var counters = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
            for (int call = 0; call < 8; call++) { api.request("GET", PLATFORM_READ_PATHS.get("yuheng"), reference, null, null, 200); }
            var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "SECURITY_METRICS", null);
            var successful = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
            if (transactionProofLong(successful.path("counts"), "list") != Math.addExact(transactionProofLong(counters.path("counts"), "list"), 8)
                    || securityMetricCount(after, "oauth.authentication", "VALID") < Math.addExact(securityMetricCount(before, "oauth.authentication", "VALID"), 8)
                    || securityMetricCount(after, "jwt.verify.duration", "SUCCESS") < Math.addExact(securityMetricCount(before, "jwt.verify.duration", "SUCCESS"), 8)
                    || securityMetricCount(after, "jwks.cache.access", "HIT") < Math.addExact(securityMetricCount(before, "jwks.cache.access", "HIT"), 8)
                    || securityMetricCount(after, "jwks.cache.access", "MISS") != securityMetricCount(before, "jwks.cache.access", "MISS")
                    || securityMetricCount(after, "jwks.cache.access", "EXPIRED") != securityMetricCount(before, "jwks.cache.access", "EXPIRED")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            proof.set("metricsBefore", before); proof.set("metricsAfterRepeatedValidCalls", after);
            awaitYuhengSpoolDrained(root, timeout); awaitYuhengReadiness(http, timeout, true);
            var baseline = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
            String trace = UUID.randomUUID().toString().replace("-", ""); var request = HttpRequest.newBuilder(nativeResourceEndpoint("yuheng", "GET", PLATFORM_READ_PATHS.get("yuheng")))
                    .GET().timeout(timeout).header("X-Request-Id", UUID.randomUUID().toString()).header("traceparent", "00-" + trace + '-' + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01");
            byte[] token = UnifiedIdentityLiveClient.protectedBytes(reference.getTokenFile(), root.resolve("secrets"), 128 * 1024);
            try {
                if (!reference.getTokenSha256().equals(UnifiedIdentityLiveClient.sha256(token))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                request.header("Authorization", "Bearer " + new String(token, StandardCharsets.UTF_8));
            } finally { java.util.Arrays.fill(token, (byte) 0); }
            boolean restore = false; var body = new ByteArrayOutputStream(); HttpResponse<Void> unavailable; Instant requestedAt = Instant.now();
            try {
                restore = true;
                var paused = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "SHARED_STATE_PAUSE", null); proof.set("pause", paused);
                Instant resumesAt = transactionProofInstant(paused, "resumesAt");
                if (!resumesAt.isAfter(Instant.now().plusSeconds(5))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                requestedAt = Instant.now(); var future = http.sendAsync(request.build(), boundedBody(body));
                try { unavailable = future.get(Math.max(1, Math.min(timeout.toNanos(), Duration.between(Instant.now(), resumesAt.minusSeconds(1)).toNanos())), TimeUnit.NANOSECONDS); }
                finally { if (!future.isDone()) { future.cancel(true); } }
                if (unavailable.statusCode() != 503 || !resumesAt.isAfter(Instant.now())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var error = mapper.readTree(body.toByteArray());
                if (!java.util.Set.of("OAUTH_SECURITY_STATE_TIMEOUT", "OAUTH_SECURITY_STATE_UNAVAILABLE", "OAUTH_AUDIT_DELIVERY_UNAVAILABLE").contains(error.path("code").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                awaitYuhengReadiness(http, Duration.ofSeconds(2), false);
                proof.put("unavailableCode", error.path("code").asText()).put("readinessDuringFault", 503);
            } finally {
                if (restore) { proof.set("resume", XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "SHARED_STATE_RESUME", null)); }
            }
            proof.set("unavailableRequest", mapper.valueToTree(observedHttpCall(unavailable, trace, requestedAt)));
            var denied = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null);
            if (!baseline.equals(denied)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            awaitYuhengSpoolDrained(root, timeout); awaitYuhengReadiness(http, timeout, true);
            api.request("GET", PLATFORM_READ_PATHS.get("yuheng"), reference, null, null, 200);
            var recovered = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "BUSINESS_COUNTS", null); requireOneBusinessInvocation(denied, recovered);
            var finalMetrics = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "yuheng", "default", "SECURITY_METRICS", null);
            if (securityMetricCount(finalMetrics, "oauth.authentication", "UNAVAILABLE") <= securityMetricCount(after, "oauth.authentication", "UNAVAILABLE")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            proof.set("businessBeforeFault", baseline); proof.set("businessAfterUnavailable", denied); proof.set("businessAfterRecovery", recovered); proof.set("metricsAfterRecovery", finalMetrics);
            proof.put("recoveryReadiness", 200).put("recoveryStatus", 200).put("repeatedValidRequestCount", 8);
        }
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("yuheng", "shoubing", "edge"));
        proof.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("resource-shared-state-metrics.json"), proof);
    }

    private static void requirePrivateSpoolDirectory(Path root, Path directory) throws Exception {
        var owner = root.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
        for (Path current = directory; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            if (current.startsWith(root) && (!Files.isDirectory(current, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    || !owner.equals(Files.getOwner(current, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    || !Files.getPosixFilePermissions(current, java.nio.file.LinkOption.NOFOLLOW_LINKS).equals(PosixFilePermissions.fromString("rwx------")))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
    }

    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    static final class TaskSpoolFault implements AutoCloseable {
        private final Path root;
        private final Object directoryKey;
        private final Path path;
        private final Object fileKey;
        private final long size;
        private final java.nio.file.attribute.FileTime modifiedAt;
        @Override public void close() throws Exception {
            requirePrivateSpoolDirectory(root, path.getParent());
            var directory = Files.readAttributes(path.getParent(), java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            var actual = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
            if (!directoryKey.equals(directory.fileKey()) || !actual.isRegularFile() || !fileKey.equals(actual.fileKey()) || size != actual.size()
                    || !modifiedAt.equals(actual.lastModifiedTime()) || !Files.getOwner(root).equals(Files.getOwner(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    || !Files.getPosixFilePermissions(path, java.nio.file.LinkOption.NOFOLLOW_LINKS).equals(PosixFilePermissions.fromString("rw-------"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            Files.delete(path);
            try (var parent = java.nio.channels.FileChannel.open(path.getParent(), java.nio.file.StandardOpenOption.READ)) { parent.force(true); }
        }
    }

    @Test
    void taskSpoolFaultRejectsForeignDirectoryBeforeAnyWrite(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        var foreign = root.resolve("foreign"); Files.createDirectory(foreign, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        assertThatThrownBy(() -> createTaskSpoolFault(root, foreign, "DEAD", 512)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        try (var contents = Files.list(foreign)) { assertThat(contents.count()).isZero(); }
    }

    @Test
    void taskSpoolFaultUsesTheOriginalReadinessAndOnlyRemovesItsOwnFile(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Path evidence = root.resolve("evidence"), directory = evidence.resolve("oauth-spool-yuheng");
        Files.createDirectory(evidence, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Path key = root.resolve("test-hmac"); Files.write(key, new byte[32]); Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"));
        var audit = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties("task:audit", directory, 512, key);
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var original = new top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReadinessComponent(audit, validators.getValidator());
            try (var lease = original.acquireSpoolLease()) {
                var ready = original.readiness(lease); assertThat(ready.getAsBoolean()).isTrue();
                for (String kind : List.of("DEAD", "CAPACITY")) {
                    try (var fault = createTaskSpoolFault(root, directory, kind, 512)) {
                        assertThat(Files.getPosixFilePermissions(fault.path)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
                        assertThat(ready.getAsBoolean()).isFalse();
                    }
                    assertThat(ready.getAsBoolean()).isTrue();
                }
                var fault = createTaskSpoolFault(root, directory, "DEAD", 512);
                Files.move(fault.path, fault.path.resolveSibling("retained-original.dead"));
                Files.writeString(fault.path, "foreign replacement", java.nio.file.StandardOpenOption.CREATE_NEW);
                Files.setPosixFilePermissions(fault.path, PosixFilePermissions.fromString("rw-------"));
                assertThatThrownBy(fault::close).hasMessage("XINGYUAN_TARGET_UNSAFE");
                assertThat(Files.readString(fault.path)).isEqualTo("foreign replacement");
            }
        }
    }

    @Test
    void nativeUserHttpTargetsCannotChooseAnUnreviewedHostOrMutation() {
        assertThat(nativeResourceEndpoint("native-web", "POST", "/api/v1/users").toString()).isEqualTo("https://127.0.0.1:28311/api/v1/users");
        assertThat(nativeResourceEndpoint("native-light", "POST", "/graphql").toString()).isEqualTo("https://127.0.0.1:28310/graphql");
        assertThatThrownBy(() -> nativeResourceEndpoint("native-light", "GET", "/graphql")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> nativeResourceEndpoint("native-agent", "POST", "/api/v1/users")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> nativeResourceEndpoint("native-web", "DELETE", "/api/v1/users/1")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> nativeResourceEndpoint("native-web", "GET", "/api/v1/users/../1")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void nativeAgentKnowledgeTargetsUseOnlyTheOriginalFrozenPaths() {
        assertThat(nativeResourceEndpoint("native-agent", "POST", "/api/v1/knowledge-bases").toString())
                .isEqualTo("https://127.0.0.1:28313/api/v1/knowledge-bases");
        assertThatThrownBy(() -> nativeResourceEndpoint("native-agent", "DELETE", "/api/v1/knowledge-bases/1")).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> nativeResourceEndpoint("native-agent", "GET", "/api/v1/knowledge-bases/../secrets")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void nativeAgentKnowledgeBasesKeepTenantAndAuthorizationBoundariesWithoutInference() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024);
        var manifest = mapper.readTree(bytes); String digest = UnifiedIdentityLiveClient.sha256(bytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        requireEvidenceDirectory(root, evidence);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("native-agent"));
        var profile = verifiedProfile(root, manifest, digest, "native-agent");
        String model = profile.getProperty("egon.cola.component.rag.default-embedding-model"); profile.clear();
        if (model == null || model.isBlank() || model.length() > 32 || model.contains("${")) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        String resource = client.resourceForProcess("native-agent");
        var first = client.formal("default", resource, "admin"); var second = client.formal("tenant-b", resource, "admin");
        var viewer = client.formal("default", resource, "viewer");
        client.verifiedClaims(first); client.verifiedClaims(second); client.verifiedClaims(viewer);
        String code = "xingyuan-it-" + UUID.randomUUID();
        var body = mapper.createObjectNode().put("code", code).put("name", code).put("description", "Task-owned knowledge metadata verification")
                .put("embeddingModel", model).put("chunkStrategy", "TOKEN");
        body.putObject("chunkConfig").put("maxTokensPerChunk", 256).put("overlapTokens", 32).put("minChunkChars", 1).putArray("headingLevels");
        try (var transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()))
                .followRedirects(HttpClient.Redirect.NEVER).build()) {
            var http = new NativeResourceHttpProbe("native-agent", root, manifestFile, digest, transport,
                    Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()));
            var createdA = http.request("POST", "/api/v1/knowledge-bases", first, body, null, 200);
            var createdB = http.request("POST", "/api/v1/knowledge-bases", second, body, null, 200);
            String idA = requireAgentKnowledgeBase(createdA, body), idB = requireAgentKnowledgeBase(createdB, body);
            assertThat(idA).isNotEqualTo(idB);
            assertThat(requireAgentKnowledgeBase(http.request("GET", "/api/v1/knowledge-bases/" + idA, first, null, null, 200), body)).isEqualTo(idA);
            assertThat(requireAgentKnowledgeBase(http.request("GET", "/api/v1/knowledge-bases/" + idB, second, null, null, 200), body)).isEqualTo(idB);
            String list = "/api/v1/knowledge-bases?page=1&size=100&keyword=" + code;
            var pageA = http.request("GET", list, first, null, null, 200); var pageB = http.request("GET", list, second, null, null, 200);
            requireAgentKnowledgePage(pageA, idA, body, 1); requireAgentKnowledgePage(pageB, idB, body, 1);
            assertThat(requireAgentKnowledgeBase(http.request("GET", "/api/v1/knowledge-bases/" + idA, first, null, second.getTenantId(), 200), body)).isEqualTo(idA);
            var foreign = http.request("GET", "/api/v1/knowledge-bases/" + idA, second, null, null, 404);
            assertThat(foreign.path("code").asText()).isEqualTo("KNOWLEDGE_BASE_NOT_FOUND");
            http.request("GET", "/api/v1/knowledge-bases/" + idA, null, null, null, 401);
            var deniedBody = body.deepCopy().put("code", code + "-deny");
            http.request("POST", "/api/v1/knowledge-bases", viewer, deniedBody, null, 403);
            requireAgentKnowledgePage(http.request("GET", list + "-deny", first, null, null, 200), null, deniedBody, 0);
            assertThat(requireAgentKnowledgeBase(http.request("GET", "/api/v1/knowledge-bases/" + idA, first, null, null, 200), body)).isEqualTo(idA);
            var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                    .put("status", "PASSED").put("scope", "AGENT_KNOWLEDGE_METADATA_ONLY").put("resourceUri", first.getResourceUri().toASCIIString())
                    .put("tokenAReference", first.getTokenSha256()).put("tokenBReference", second.getTokenSha256())
                    .put("knowledgeBaseAId", idA).put("knowledgeBaseBId", idB)
                    .put("baseAReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(createdA)))
                    .put("baseBReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(createdB)))
                    .put("pageAReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(pageA)))
                    .put("pageBReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(pageB)))
                    .put("viewerDeniedStatus", 403).put("deniedRecords", 0).put("foreignTenantCode", "KNOWLEDGE_BASE_NOT_FOUND")
                    .put("bareStatus", 401).put("recordedAt", Instant.now().toString());
            client.publishNativeAgentKnowledgeProof(evidence, proof);
        }
    }

    static String requireAgentKnowledgeBase(JsonNode actual, JsonNode expected) {
        if (actual == null || !actual.isObject() || !actual.path("knowledgeBaseId").asText().matches("[1-9][0-9]{0,18}")) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        String id = actual.path("knowledgeBaseId").asText();
        try { if (Long.parseLong(id) <= 0) { throw new NumberFormatException(); } }
        catch (NumberFormatException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (String field : List.of("code", "name", "embeddingModel", "chunkStrategy")) {
            if (!expected.path(field).asText().equals(actual.path(field).asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        for (String field : List.of("maxTokensPerChunk", "overlapTokens")) {
            if (!actual.path("chunkConfig").path(field).isIntegralNumber() || !actual.path("chunkConfig").path(field).canConvertToInt()
                    || actual.path("chunkConfig").path(field).intValue() != expected.path("chunkConfig").path(field).intValue()) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
        if (!"ACTIVE".equals(actual.path("status").asText()) || !actual.path("documentCount").isIntegralNumber() || !actual.path("documentCount").canConvertToLong()
                || actual.path("documentCount").longValue() != 0) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        for (String field : List.of("createdAt", "updatedAt")) {
            if (!actual.path(field).isTextual() || !actual.path(field).textValue().endsWith("Z")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            try { Instant.parse(actual.path(field).textValue()); } catch (java.time.format.DateTimeParseException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        return id;
    }

    static void requireAgentKnowledgePage(JsonNode page, String expectedId, JsonNode body, int total) {
        if (page == null || !page.path("success").isBoolean() || !page.path("success").booleanValue() || !page.path("records").isArray()
                || page.path("records").size() != total || !page.path("page").path("total").isIntegralNumber() || !page.path("page").path("total").canConvertToLong()
                || page.path("page").path("total").longValue() != total) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        if (total == 1 && !expectedId.equals(requireAgentKnowledgeBase(page.path("records").get(0), body))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    @Test
    void agentMetadataAndPageCannotSubstituteAnotherRowOrIgnoreFrozenConfiguration() {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        var body = mapper.createObjectNode().put("code", "owned-base").put("name", "Owned base").put("embeddingModel", "configured-model").put("chunkStrategy", "TOKEN");
        body.putObject("chunkConfig").put("maxTokensPerChunk", 256).put("overlapTokens", 32);
        var actual = body.deepCopy().put("knowledgeBaseId", 11).put("status", "ACTIVE").put("documentCount", 0)
                .put("createdAt", "2026-10-15T00:00:00Z").put("updatedAt", "2026-10-15T00:00:00Z");
        assertThat(requireAgentKnowledgeBase(actual, body)).isEqualTo("11");
        var changedConfig = actual.deepCopy(); changedConfig.withObject("chunkConfig").put("maxTokensPerChunk", 512);
        assertThatThrownBy(() -> requireAgentKnowledgeBase(changedConfig, body)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var page = mapper.createObjectNode().put("success", true); page.putArray("records").add(actual); page.putObject("page").put("total", 1);
        assertThatCode(() -> requireAgentKnowledgePage(page, "11", body, 1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireAgentKnowledgePage(page, "12", body, 1)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> requireAgentKnowledgePage(page, null, body, 0)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var empty = mapper.createObjectNode().put("success", true); empty.putArray("records"); empty.putObject("page").put("total", 0);
        assertThatCode(() -> requireAgentKnowledgePage(empty, null, body, 0)).doesNotThrowAnyException();
        var overflow = actual.deepCopy().put("documentCount", new java.math.BigInteger("18446744073709551616"));
        assertThatThrownBy(() -> requireAgentKnowledgeBase(overflow, body)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    static void requireGraphQlUserAliases(JsonNode response, JsonNode expected) {
        if (response == null || !response.isObject() || response.has("errors") && (!response.path("errors").isArray() || !response.path("errors").isEmpty())
                || !response.path("data").isObject() || !expected.equals(response.path("data").path("left")) || !expected.equals(response.path("data").path("right"))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    static void requireGraphQlMissingAliases(JsonNode response) {
        if (response == null || !response.path("data").isObject() || !response.path("data").path("left").isNull() || !response.path("data").path("right").isNull()
                || !response.path("errors").isArray() || response.path("errors").isEmpty()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (var error : response.path("errors")) {
            if (!"USER_NOT_FOUND".equals(error.path("extensions").path("code").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
    }

    static void requireLightGraphQlScopeDenial(JsonNode response) {
        if (!response.path("data").isObject() || response.path("data").size() != 2
                || !response.path("data").path("left").isNull() || !response.path("data").path("right").isNull()
                || !response.path("errors").isArray() || response.path("errors").size() != 2) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var aliases = new java.util.HashSet<String>();
        for (var error : response.path("errors")) {
            if (!"AUTHORIZATION_DENIED".equals(error.path("extensions").path("code").asText()) || !error.path("path").isArray() || error.path("path").size() != 1
                    || !error.path("path").get(0).isTextual() || !aliases.add(error.path("path").get(0).textValue())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        if (!aliases.equals(java.util.Set.of("left", "right"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    @Test
    void lightGraphQlScopeDenialRequiresBothActualAliasesAndNoData() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var response = mapper.createObjectNode();
        response.putObject("data").putNull("left").putNull("right"); var errors = response.putArray("errors");
        for (String alias : List.of("left", "right")) {
            var error = errors.addObject(); error.putObject("extensions").put("code", "AUTHORIZATION_DENIED"); error.putArray("path").add(alias);
        }
        assertThatCode(() -> requireLightGraphQlScopeDenial(response)).doesNotThrowAnyException();
        var missing = response.deepCopy(); missing.withArray("errors").remove(1);
        assertThatThrownBy(() -> requireLightGraphQlScopeDenial(missing)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var duplicate = response.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) duplicate.path("errors").get(1)).putArray("path").add("left");
        assertThatThrownBy(() -> requireLightGraphQlScopeDenial(duplicate)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var data = response.deepCopy(); data.withObject("data").putObject("left").put("id", "11");
        assertThatThrownBy(() -> requireLightGraphQlScopeDenial(data)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var unavailable = response.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) unavailable.path("errors").get(0).path("extensions")).put("code", "OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
        assertThatThrownBy(() -> requireLightGraphQlScopeDenial(unavailable)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void nativeLightGraphQlAliasesUseTheSameVerifiedTenantAsRest() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated();
        var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024);
        var manifest = mapper.readTree(bytes); String digest = UnifiedIdentityLiveClient.sha256(bytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        requireEvidenceDirectory(root, evidence);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("native-light"));
        var first = client.formal("default", client.resourceForProcess("native-light"), "admin");
        var second = client.formal("tenant-b", first.getResourceKey(), "admin");
        client.verifiedClaims(first); client.verifiedClaims(second); client.bindNativeTenantReadEndpoints(first, second);
        String firstPath = first.getReadEndpoint().getPath(), secondPath = second.getReadEndpoint().getPath();
        String firstId = firstPath.substring(firstPath.lastIndexOf('/') + 1), secondId = secondPath.substring(secondPath.lastIndexOf('/') + 1);
        var query = mapper.createObjectNode().put("query", "query OwnedUser($id: ID!) { left: user(id: $id) { id name email status } right: user(id: $id) { id name email status } }");
        var bodyA = query.deepCopy(); bodyA.putObject("variables").put("id", firstId);
        var bodyB = query.deepCopy(); bodyB.putObject("variables").put("id", secondId);
        var asyncObservations = mapper.createArrayNode();
        try (var transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()))
                .followRedirects(HttpClient.Redirect.NEVER).build()) {
            var http = new NativeResourceHttpProbe("native-light", root, manifestFile, digest, transport,
                    Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()));
            var restA = http.request("GET", firstPath, first, null, null, 200);
            var restB = http.request("GET", secondPath, second, null, null, 200);
            var resultA = http.observedGraphQl(first, bodyA, null, 2, 2, asyncObservations);
            var resultB = http.observedGraphQl(second, bodyB, null, 2, 2, asyncObservations);
            requireGraphQlUserAliases(resultA, restA); requireGraphQlUserAliases(resultB, restB);
            var forged = http.request("POST", "/graphql", first, bodyA, second.getTenantId(), 200);
            requireGraphQlUserAliases(forged, restA);
            requireGraphQlMissingAliases(http.request("POST", "/graphql", second, bodyA, null, 200));
            var identityOnly = acquireIdentityOnlyProbe(client, root, evidence, manifestFile, manifest, digest, "default", first.getResourceKey());
            var beforeScope = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
            requireLightGraphQlScopeDenial(http.observedGraphQl(identityOnly, bodyA, null, 2, 0, asyncObservations));
            var afterScope = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
            UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(beforeScope, afterScope, "get", 0);
            http.request("POST", "/graphql", null, bodyA, null, 401);
            var afterBare = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
            UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(afterScope, afterBare, "get", 0);
            requireGraphQlUserAliases(http.request("POST", "/graphql", first, bodyA, null, 200), restA);
            var afterRecovery = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
            UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(afterBare, afterRecovery, "get", 2);
            client.requireNativeTenantIsolationProof(first, second);
            var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                    .put("status", "PASSED").put("scope", "LIGHT_GRAPHQL_TWO_TENANT_READ").put("resourceUri", first.getResourceUri().toASCIIString())
                    .put("tokenAReference", first.getTokenSha256()).put("tokenBReference", second.getTokenSha256())
                    .put("queryReference", UnifiedIdentityLiveClient.sha256(query.path("query").asText().getBytes(StandardCharsets.UTF_8)))
                    .put("tenantAResponseReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(resultA)))
                    .put("tenantBResponseReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(resultB)))
                    .put("forgedHeaderResponseReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(forged)))
                    .put("identityOnlyTokenReference", identityOnly.getTokenSha256()).put("scopeDeniedCode", "AUTHORIZATION_DENIED")
                    .put("foreignTenantCode", "USER_NOT_FOUND").put("bareStatus", 401).put("recordedAt", Instant.now().toString());
            proof.set("asyncObservations", asyncObservations);
            proof.set("beforeScopeDenied", beforeScope); proof.set("afterScopeDenied", afterScope); proof.set("afterBare", afterBare); proof.set("afterRecovery", afterRecovery);
            client.publishNativeGraphQlProof(evidence, proof);
        }
    }

    @Test
    void graphQlEnvelopeCannotHideErrorsOrAnotherTenantInOneAlias() {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        var user = mapper.createObjectNode().put("id", "11").put("name", "owned").put("email", "owned@xingyuan.invalid").put("status", "ACTIVE");
        var success = mapper.createObjectNode(); success.putObject("data").set("left", user); success.withObject("data").set("right", user);
        assertThatCode(() -> requireGraphQlUserAliases(success, user)).doesNotThrowAnyException();
        var mixed = success.deepCopy(); mixed.withObject("data").set("right", user.deepCopy().put("id", "12"));
        assertThatThrownBy(() -> requireGraphQlUserAliases(mixed, user)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var partial = success.deepCopy(); partial.putArray("errors").addObject().putObject("extensions").put("code", "AUTHORIZATION_DENIED");
        assertThatThrownBy(() -> requireGraphQlUserAliases(partial, user)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        var missing = mapper.createObjectNode(); missing.putObject("data").putNull("left").putNull("right");
        missing.putArray("errors").addObject().putObject("extensions").put("code", "USER_NOT_FOUND");
        assertThatCode(() -> requireGraphQlMissingAliases(missing)).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireGraphQlMissingAliases(success)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    static void requireWebGraphQlDenied(JsonNode response, String alias, String code) {
        if (!response.has("data") || !response.path("data").isNull() || !response.path("errors").isArray() || response.path("errors").size() != 1) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var error = response.path("errors").get(0);
        if (!error.path("path").isArray() || error.path("path").size() != 1 || !alias.equals(error.path("path").get(0).asText())
                || !code.equals(error.path("extensions").path("code").asText()) || !error.path("extensions").path("traceId").isTextual()
                || !error.path("extensions").path("timestamp").isTextual() || !error.path("extensions").path("fieldErrors").isObject()) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void actualNativeWebGraphQlKeepsItsNonNullContractAndVerifiedTenant() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(bytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText()); String digest = UnifiedIdentityLiveClient.sha256(bytes);
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString())); requireEvidenceDirectory(root, evidence);
        requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("native-web", "shoubing", "jianshen"));
        String resource = client.resourceForProcess("native-web"); var a = client.formal("default", resource, "admin"); var b = client.formal("tenant-b", resource, "admin");
        client.verifiedClaims(a); client.verifiedClaims(b);
        var scopeOnly = acquireIdentityOnlyProbe(client, root, evidence, manifestFile, manifest, digest, "default", resource);
        Duration timeout = Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                .put("scope", "WEB_GRAPHQL_ACTUAL_USER_ALIASES_AND_CONTEXT_ONLY").put("tokenAReference", a.getTokenSha256()).put("tokenBReference", b.getTokenSha256());
        var observations = proof.putArray("observations"); var asyncObservations = proof.putArray("asyncObservations");
        try (var http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var api = new NativeResourceHttpProbe("native-web", root, manifestFile, digest, http, timeout); JsonNode first = null;
            for (var reference : List.of(a, b)) {
                String marker = "xingyuan-it-" + manifest.path("runId").asText() + "-graphql-" + UUID.randomUUID();
                var created = api.request("POST", "/api/v1/users", reference, mapper.createObjectNode().put("name", marker).put("email", marker + "@xingyuan.invalid"), null, 201);
                long id = responseId(created); var expected = mapper.createObjectNode();
                for (String field : List.of("id", "name", "email", "status")) { expected.set(field, created.path(field)); }
                expected.put("id", Long.toString(id));
                String query = "query($id: ID!) { left: user(id: $id) { id name email status } right: user(id: $id) { id name email status } }";
                var request = mapper.createObjectNode().put("query", query); request.putObject("variables").put("id", Long.toString(id));
                var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-web", reference.getRealmCode(), "BUSINESS_COUNTS", null);
                var actual = api.observedGraphQl(reference, request, null, 2, 2, asyncObservations);
                requireGraphQlUserAliases(actual, expected);
                var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-web", reference.getRealmCode(), "BUSINESS_COUNTS", null);
                if (transactionProofLong(after.path("counts"), "getUser") != Math.addExact(transactionProofLong(before.path("counts"), "getUser"), 2)
                        || !before.path("counts").path("createUser").equals(after.path("counts").path("createUser"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                var forged = api.request("POST", "/graphql", reference, request, reference == a ? b.getTenantId() : a.getTenantId(), 200); requireGraphQlUserAliases(forged, expected);
                var item = observations.addObject().put("realmCode", reference.getRealmCode()).put("rowReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(expected)))
                        .put("queryReference", UnifiedIdentityLiveClient.sha256(query.getBytes(StandardCharsets.UTF_8))).put("responseReference", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(actual)));
                item.set("before", before); item.set("afterAliases", after);
                if (reference == a) {
                    first = created; var deniedRequest = mapper.createObjectNode().put("query", "query($id: ID!) { denied: user(id: $id) { id name email status } }");
                    deniedRequest.putObject("variables").put("id", Long.toString(id));
                    var beforeDenied = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-web", "default", "BUSINESS_COUNTS", null);
                    var denied = api.observedGraphQl(scopeOnly, deniedRequest, null, 1, 0, asyncObservations); requireWebGraphQlDenied(denied, "denied", "AUTHORIZATION_DENIED");
                    api.request("POST", "/graphql", null, deniedRequest, null, 401);
                    var afterDenied = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-web", "default", "BUSINESS_COUNTS", null);
                    if (!beforeDenied.equals(afterDenied)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                    item.put("missingScopeGraphQlStatus", 200).put("bareHttpStatus", 401); item.set("afterDenied", afterDenied);
                } else {
                    var foreign = mapper.createObjectNode().put("query", "query($id: ID!) { foreign: user(id: $id) { id name email status } }");
                    foreign.putObject("variables").put("id", Long.toString(responseId(first)));
                    requireWebGraphQlDenied(api.request("POST", "/graphql", b, foreign, null, 200), "foreign", "ORG_NOT_FOUND");
                    requireGraphQlUserAliases(api.request("POST", "/graphql", b, request, null, 200), expected);
                    item.put("foreignTenantCode", "ORG_NOT_FOUND");
                }
            }
        }
        proof.put("status", "PASSED").put("recordedAt", Instant.now().toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("native-web-graphql.json"), proof);
    }

    @Test
    void webGraphQlDenialCannotHideTheOriginalNonNullPropagationOrPath() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var response = mapper.createObjectNode().putNull("data");
        var error = response.putArray("errors").addObject(); error.putArray("path").add("denied");
        error.putObject("extensions").put("code", "AUTHORIZATION_DENIED").put("traceId", "controlled").put("timestamp", "2026-10-15T00:00:00Z").putObject("fieldErrors");
        assertThatCode(() -> requireWebGraphQlDenied(response, "denied", "AUTHORIZATION_DENIED")).doesNotThrowAnyException();
        response.putObject("data").putNull("denied");
        assertThatThrownBy(() -> requireWebGraphQlDenied(response, "denied", "AUTHORIZATION_DENIED")).hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    void sqlIsolationProofRejectsCrossTenantAndDuplicateRows() {
        var first = new NativeSqlRowVO(11, 101, "ext-1", "owned-a", "a@xingyuan.invalid", "ACTIVE");
        var second = new NativeSqlRowVO(12, 102, "ext-1", "owned-b", "b@xingyuan.invalid", "ACTIVE");
        assertThatCode(() -> requireOwnedRows(List.of(first, second), 101, 102, 11, 12, "owned-a", "owned-b"))
                .doesNotThrowAnyException();
        var foreign = new NativeSqlRowVO(12, 101, "ext-1", "owned-b", "b@xingyuan.invalid", "ACTIVE");
        assertThatThrownBy(() -> requireOwnedRows(List.of(first, foreign), 101, 102, 11, 12, "owned-a", "owned-b"))
                .hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> requireOwnedRows(List.of(first, first), 101, 102, 11, 12, "owned-a", "owned-b"))
                .hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThatThrownBy(() -> requireOwnedRows(List.of(first), 101, 102, 11, 12, "owned-a", "owned-b"))
                .hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

    @Test
    void nativeSqlTargetsRejectForeignReceiptsAndIdentifiersBeforeConnecting() {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        String runId = UUID.randomUUID().toString(), digest = "a".repeat(64);
        var manifest = mapper.createObjectNode().put("runId", runId);
        var receipt = mapper.createObjectNode().put("runId", runId).put("manifestSha256", digest).put("role", "light")
                .put("databaseName", "xingyuan_it_light_20261011").put("oid", 71).put("databaseOwner", "controlled_owner")
                .put("ownerComment", "xingyuan-it:" + runId + ":" + digest);
        assertThatCode(() -> requireLightReceipt(manifest, digest, receipt)).doesNotThrowAnyException();
        for (String field : List.of("databaseName", "runId", "ownerComment")) {
            var foreign = receipt.deepCopy().put(field, "foreign");
            assertThatThrownBy(() -> requireLightReceipt(manifest, digest, foreign)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var fractional = receipt.deepCopy().put("oid", 71.5);
        assertThatThrownBy(() -> requireLightReceipt(manifest, digest, fractional)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        assertThatThrownBy(() -> readRows(null, "public.users;SELECT 1", 101, 102)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void nativeResponseSubscriberRejectsOversizeBeforeAccumulation() {
        var output = new ByteArrayOutputStream();
        var subscriber = boundedBody(output).apply(new HttpResponse.ResponseInfo() {
            @Override public int statusCode() { return 200; }
            @Override public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(java.util.Map.of(), (name, value) -> true); }
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        });
        subscriber.onSubscribe(new java.util.concurrent.Flow.Subscription() {
            @Override public void request(long count) { }
            @Override public void cancel() { }
        });
        assertThatThrownBy(() -> subscriber.onNext(List.of(java.nio.ByteBuffer.wrap(new byte[128 * 1024 + 1]))))
                .isInstanceOf(IllegalStateException.class).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        assertThat(output.size()).isZero();
    }

    @Test
    void nativeEvidenceRequiresTheCurrentPrivateDirectoryBeforeBusinessWrites(@org.junit.jupiter.api.io.TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath();
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        Path evidence = Files.createDirectory(root.resolve("evidence"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        assertThatCode(() -> requireEvidenceDirectory(root, evidence)).doesNotThrowAnyException();
        assertThatThrownBy(() -> requireEvidenceDirectory(root, root.resolve("foreign"))).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Files.setPosixFilePermissions(evidence, PosixFilePermissions.fromString("rwxr-xr-x"));
        assertThatThrownBy(() -> requireEvidenceDirectory(root, evidence)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        Files.setPosixFilePermissions(evidence, PosixFilePermissions.fromString("rwx------"));
        Path link = evidence.resolve("verify-" + UUID.randomUUID());
        Files.createSymbolicLink(link, root);
        assertThatThrownBy(() -> requireEvidenceDirectory(root, link)).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }
}
