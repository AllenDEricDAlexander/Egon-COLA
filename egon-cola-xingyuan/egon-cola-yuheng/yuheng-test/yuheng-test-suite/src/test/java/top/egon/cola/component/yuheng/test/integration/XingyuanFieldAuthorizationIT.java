package top.egon.cola.component.yuheng.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import top.egon.cola.component.yuheng.test.tianquan.shoubing.UnifiedIdentityLiveClient;
import top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.NativeResourceHttpProbe;
import top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.NativeSqlRowVO;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.transactionProofLong;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.transactionProofInstant;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.requireEvidenceDirectory;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.requireRunningProcesses;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.verifiedProfile;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.requireLightReceipt;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.ownedLightConnection;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.userTable;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.readRows;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.verifyLightConnection;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.requireResponseMatchesSql;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.requireOwnedRows;
import static top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT.responseId;

/** Field writes prepare the one real Light data set reused by the later resource matrix. */
@lombok.extern.slf4j.Slf4j
class XingyuanFieldAuthorizationIT {

    private static final List<String> FIELD_PHASES = List.of("CAP_REJECT", "EMPTY", "READ", "WRITE", "RESTORE");

    static void validateFieldPhaseEvidence(JsonNode value, JsonNode request, String tenant, String app, long oid, Instant now) {
        var fields = new java.util.HashSet<String>(); value.fieldNames().forEachRemaining(fields::add);
        if (!value.isObject() || !fields.equals(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "databaseOid", "nonce", "sequence", "phase", "previousSha256", "status", "tenantId", "applicationCode", "roleId", "before", "after", "snapshot", "outcome", "recordedAt"))
                || !value.path("schemaVersion").isIntegralNumber() || !value.path("schemaVersion").canConvertToInt() || value.path("schemaVersion").intValue() != 1
                || !value.path("roleId").isTextual() || !value.path("roleId").asText().matches("[1-9][0-9]{0,18}")
                || !"CONFIRMED".equals(value.path("status").asText()) || !tenant.equals(value.path("tenantId").asText()) || !app.equals(value.path("applicationCode").asText())
                || transactionProofLong(value, "databaseOid") != oid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        try { if (Long.parseLong(value.path("roleId").asText()) <= 0) { throw new NumberFormatException(); } }
        catch (NumberFormatException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (String field : List.of("runId", "manifestSha256", "nonce", "sequence", "phase", "previousSha256")) {
            if (!value.path(field).equals(request.path(field))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        Instant completed = transactionProofInstant(value, "recordedAt"), requested = transactionProofInstant(request, "requestedAt");
        if (completed.isBefore(requested) || completed.isAfter(transactionProofInstant(request, "expiresAt")) || completed.isAfter(now)) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        var before = value.path("before"); var after = value.path("after"); var snapshot = value.path("snapshot");
        String phase = request.path("phase").asText(), outcome = value.path("outcome").asText();
        boolean changed = "APPLIED".equals(outcome);
        if (!FIELD_PHASES.contains(phase) || "CAP_REJECT".equals(phase) && (!"REJECTED_AT_DEFINITION_CAP".equals(outcome) || !before.equals(after))
                || !"CAP_REJECT".equals(phase) && !java.util.Set.of("APPLIED", "UNCHANGED").contains(outcome)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (String field : List.of("roleVersion", "policyVersion", "roleEventCount")) {
            if (transactionProofLong(after, field) != transactionProofLong(before, field) + (changed ? 1 : 0)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        if (!before.path("rulesSha256").asText().matches("[0-9a-f]{64}") || !after.path("rulesSha256").asText().matches("[0-9a-f]{64}")
                || transactionProofLong(snapshot, "policyVersion") < transactionProofLong(after, "policyVersion")
                || !snapshot.path("checksum").asText().matches("(?:sha256:)?[0-9a-f]{64}") || !snapshot.path("levels").isArray()) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        transactionProofLong(snapshot, "userAuthVersion");
        if ("EMPTY".equals(phase)) {
            if (transactionProofLong(snapshot, "missingFields") != 3 || !snapshot.path("levels").isEmpty()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        } else {
            String level = "READ".equals(phase) ? "READ" : "WRITE";
            if (transactionProofLong(snapshot, "missingFields") != 0 || snapshot.path("levels").size() != 3
                    || java.util.stream.StreamSupport.stream(snapshot.path("levels").spliterator(), false).anyMatch(valueLevel -> !level.equals(valueLevel.asText()))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
    }

    @Test
    void fieldEvidenceRequiresActualEpochChangesAndObservedFieldLevels() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var request = mapper.createObjectNode().put("runId", "run").put("manifestSha256", "a".repeat(64)).put("nonce", UUID.randomUUID().toString())
                .put("sequence", 3).put("phase", "READ").put("previousSha256", "b".repeat(64)).put("requestedAt", now.toString()).put("expiresAt", now.plusSeconds(60).toString());
        var receipt = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "run").put("manifestSha256", "a".repeat(64)).put("databaseOid", 71)
                .put("nonce", request.path("nonce").asText()).put("sequence", 3).put("phase", "READ").put("previousSha256", "b".repeat(64))
                .put("status", "CONFIRMED").put("tenantId", "1001").put("applicationCode", "owned-light").put("roleId", "501").put("outcome", "APPLIED").put("recordedAt", now.plusSeconds(1).toString());
        receipt.putObject("before").put("roleVersion", 2).put("policyVersion", 10).put("roleEventCount", 7).put("rulesSha256", "c".repeat(64));
        receipt.putObject("after").put("roleVersion", 3).put("policyVersion", 11).put("roleEventCount", 8).put("rulesSha256", "d".repeat(64));
        receipt.putObject("snapshot").put("userAuthVersion", 6).put("policyVersion", 11).put("checksum", "sha256:" + "e".repeat(64)).put("missingFields", 0)
                .putArray("levels").add("READ").add("READ").add("READ");
        assertThatCode(() -> validateFieldPhaseEvidence(receipt, request, "1001", "owned-light", 71, now.plusSeconds(2))).doesNotThrowAnyException();
        for (String fault : List.of("epoch", "event", "snapshot", "level", "nonce", "scope", "extra", "overflow")) {
            var changed = receipt.deepCopy();
            if (fault.equals("epoch")) { changed.withObject("after").put("policyVersion", 10); }
            else if (fault.equals("event")) { changed.withObject("after").put("roleEventCount", 7); }
            else if (fault.equals("snapshot")) { changed.withObject("snapshot").put("policyVersion", 10); }
            else if (fault.equals("level")) { changed.withObject("snapshot").putArray("levels").add("WRITE").add("WRITE").add("WRITE"); }
            else if (fault.equals("nonce")) { changed.put("nonce", UUID.randomUUID().toString()); }
            else if (fault.equals("scope")) { changed.put("applicationCode", "foreign"); }
            else if (fault.equals("extra")) { changed.put("token", "forbidden"); }
            else { changed.put("roleId", "9223372036854775808"); }
            assertThatThrownBy(() -> validateFieldPhaseEvidence(changed, request, "1001", "owned-light", 71, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    private static byte[] fieldPhase(Path root, Path evidence, JsonNode ready, String previous, int sequence) throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.now();
        var request = mapper.createObjectNode().put("schemaVersion", 1).put("runId", ready.path("runId").asText())
                .put("manifestSha256", ready.path("manifestSha256").asText()).put("nonce", ready.path("nonce").asText()).put("sequence", sequence)
                .put("phase", FIELD_PHASES.get(sequence - 1)).put("previousSha256", previous).put("requestedAt", now.toString()).put("expiresAt", now.plusSeconds(60).toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("native-field-request-0" + sequence + ".json"), request);
        Path output = evidence.resolve("native-field-result-0" + sequence + ".json");
        while (true) {
            if (Files.exists(output, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(output, evidence, 128 * 1024); var result = mapper.readTree(bytes);
                if ("CONFIRMED".equals(result.path("status").asText())) {
                    validateFieldPhaseEvidence(result, request, ready.path("tenantId").asText(), ready.path("applicationCode").asText(),
                            transactionProofLong(ready, "databaseOid"), Instant.now()); return bytes;
                }
                if (!"RUNNING".equals(result.path("status").asText()) || !request.path("nonce").equals(result.path("nonce"))
                        || !request.path("phase").equals(result.path("phase")) || !request.path("previousSha256").equals(result.path("previousSha256"))) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
            }
            if (!now.plusSeconds(60).isAfter(Instant.now())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
            Thread.sleep(100);
        }
    }

    static void validateGrantPhaseEvidence(JsonNode value, JsonNode request, JsonNode ready, Instant now) throws Exception {
        var fields = new java.util.HashSet<String>(); value.fieldNames().forEachRemaining(fields::add);
        if (!value.isObject() || !fields.equals(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "databaseOid", "nonce", "sequence", "phase", "previousSha256", "status", "tenantId", "applicationCode", "roleId",
                "originalResourceIds", "resourceIdsSha256", "before", "after", "projection", "resourceAllowed", "recordedAt"))
                || !value.path("schemaVersion").isInt() || value.path("schemaVersion").intValue() != 1 || !"CONFIRMED".equals(value.path("status").asText())
                || !value.path("resourceAllowed").isBoolean()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (String field : List.of("runId", "manifestSha256", "nonce", "sequence", "phase", "previousSha256")) {
            if (!value.path(field).equals(request.path(field))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        for (String field : List.of("tenantId", "applicationCode", "roleId", "databaseOid")) {
            if (!value.path(field).equals(ready.path(field))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        boolean restore = "RESTORE".equals(request.path("phase").asText());
        if (!restore && !"REMOVE".equals(request.path("phase").asText()) || request.path("sequence").intValue() != (restore ? 2 : 1)
                || value.path("resourceAllowed").booleanValue() != restore) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        Instant completed = transactionProofInstant(value, "recordedAt");
        if (completed.isBefore(transactionProofInstant(request, "requestedAt")) || completed.isAfter(now)
                || completed.isAfter(transactionProofInstant(request, "expiresAt"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var ids = value.path("originalResourceIds"); var keys = new java.util.HashSet<String>();
        if (!ids.isArray() || ids.isEmpty() || ids.size() > 2000) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (var id : ids) {
            if (!id.isTextual() || !id.asText().matches("[1-9][0-9]{0,18}") || !keys.add(id.asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            try { if (Long.parseLong(id.asText()) <= 0) { throw new NumberFormatException(); } }
            catch (NumberFormatException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        var mapper = UnifiedIdentityLiveClient.strictJson();
        String originalHash = UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(ids));
        String desiredHash = restore ? originalHash : UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(List.of()));
        if (!originalHash.equals(ready.path("originalResourceIdsSha256").asText()) || !desiredHash.equals(value.path("resourceIdsSha256").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var before = value.path("before"); var after = value.path("after"); var projection = value.path("projection");
        for (var state : List.of(before, after)) {
            var names = new java.util.HashSet<String>(); state.fieldNames().forEachRemaining(names::add);
            if (!state.isObject() || !names.equals(java.util.Set.of("roleVersion", "policyVersion", "roleEventCount", "rulesSha256"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        var projectionFields = new java.util.HashSet<String>(); projection.fieldNames().forEachRemaining(projectionFields::add);
        if (!projection.isObject() || !projectionFields.equals(java.util.Set.of("tenantId", "rbacUserId", "authVersion", "policyVersion", "selectedRoleIds", "snapshotChecksum", "expiresAt"))) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        for (String field : List.of("roleVersion", "policyVersion", "roleEventCount")) {
            if (transactionProofLong(before, field) == Long.MAX_VALUE || transactionProofLong(after, field) != transactionProofLong(before, field) + 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        if (!before.path("rulesSha256").asText().matches("[0-9a-f]{64}") || !before.path("rulesSha256").equals(after.path("rulesSha256"))
                || !ready.path("tenantId").equals(projection.path("tenantId")) || transactionProofLong(projection, "policyVersion") < transactionProofLong(after, "policyVersion")
                || !projection.path("snapshotChecksum").asText().matches("[0-9a-f]{64}") || !transactionProofInstant(projection, "expiresAt").isAfter(completed)
                || !projection.path("selectedRoleIds").isArray() || projection.path("selectedRoleIds").size() < 4 || projection.path("selectedRoleIds").size() > 8) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        if (transactionProofLong(projection, "authVersion") < 1) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        var selected = new java.util.HashSet<String>();
        for (var id : projection.path("selectedRoleIds")) {
            if (!id.isTextual() || !id.asText().matches("[1-9][0-9]{0,18}") || !selected.add(id.asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            try { if (Long.parseLong(id.asText()) <= 0) { throw new NumberFormatException(); } }
            catch (NumberFormatException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        }
    }

    @Test
    void resourceGrantEvidenceRequiresTheRealEpochEventAndPermissionTransition() throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.parse("2026-10-15T00:00:00Z");
        var ready = mapper.createObjectNode().put("tenantId", "1001").put("applicationCode", "owned-light").put("roleId", "501").put("databaseOid", 71);
        var original = mapper.createArrayNode().add("701").add("702");
        ready.put("originalResourceIdsSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(original)));
        var request = mapper.createObjectNode().put("runId", "run").put("manifestSha256", "a".repeat(64)).put("nonce", UUID.randomUUID().toString())
                .put("sequence", 1).put("phase", "REMOVE").put("previousSha256", "b".repeat(64)).put("requestedAt", now.toString()).put("expiresAt", now.plusSeconds(60).toString());
        var receipt = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "run").put("manifestSha256", "a".repeat(64)).put("nonce", request.path("nonce").asText())
                .put("sequence", 1).put("phase", "REMOVE").put("previousSha256", "b".repeat(64)).put("status", "CONFIRMED").put("tenantId", "1001").put("applicationCode", "owned-light")
                .put("roleId", "501").put("databaseOid", 71).put("resourceIdsSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(List.of())))
                .put("resourceAllowed", false).put("recordedAt", now.plusSeconds(1).toString());
        receipt.set("originalResourceIds", original);
        receipt.putObject("before").put("roleVersion", 3).put("policyVersion", 7).put("roleEventCount", 4).put("rulesSha256", "c".repeat(64));
        receipt.putObject("after").put("roleVersion", 4).put("policyVersion", 8).put("roleEventCount", 5).put("rulesSha256", "c".repeat(64));
        var projection = receipt.putObject("projection").put("tenantId", "1001").put("rbacUserId", "101").put("authVersion", 5).put("policyVersion", 8)
                .put("snapshotChecksum", "d".repeat(64)).put("expiresAt", now.plusSeconds(300).toString());
        projection.putArray("selectedRoleIds").add("301").add("302").add("303").add("304");
        assertThatCode(() -> validateGrantPhaseEvidence(receipt, request, ready, now.plusSeconds(2))).doesNotThrowAnyException();
        for (String fault : List.of("epoch", "event", "rules", "projection", "permission", "resources", "unknown", "extra", "replay")) {
            var wrong = receipt.deepCopy();
            if (fault.equals("epoch")) { wrong.withObject("after").put("policyVersion", 7); }
            else if (fault.equals("event")) { wrong.withObject("after").put("roleEventCount", 4); }
            else if (fault.equals("rules")) { wrong.withObject("after").put("rulesSha256", "e".repeat(64)); }
            else if (fault.equals("projection")) { wrong.withObject("projection").put("policyVersion", 7); }
            else if (fault.equals("permission")) { wrong.put("resourceAllowed", true); }
            else if (fault.equals("resources")) { wrong.put("resourceIdsSha256", ready.path("originalResourceIdsSha256").asText()); }
            else if (fault.equals("unknown")) { wrong.put("status", "UNKNOWN_COMMIT"); }
            else if (fault.equals("extra")) { wrong.withObject("projection").put("accessToken", "forbidden"); }
            else { wrong.put("nonce", UUID.randomUUID().toString()); }
            assertThatThrownBy(() -> validateGrantPhaseEvidence(wrong, request, ready, now.plusSeconds(2))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        receipt.put("sequence", 2).put("phase", "RESTORE").put("resourceAllowed", true).put("resourceIdsSha256", ready.path("originalResourceIdsSha256").asText());
        request.put("sequence", 2).put("phase", "RESTORE");
        assertThatCode(() -> validateGrantPhaseEvidence(receipt, request, ready, now.plusSeconds(2))).doesNotThrowAnyException();
    }

    private static byte[] grantPhase(Path root, Path evidence, JsonNode ready, String previous, int sequence) throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant now = Instant.now();
        var request = mapper.createObjectNode().put("schemaVersion", 1).put("runId", ready.path("runId").asText()).put("manifestSha256", ready.path("manifestSha256").asText())
                .put("nonce", ready.path("nonce").asText()).put("sequence", sequence).put("phase", sequence == 1 ? "REMOVE" : "RESTORE").put("previousSha256", previous)
                .put("requestedAt", now.toString()).put("expiresAt", now.plusSeconds(60).toString());
        XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("native-resource-grant-request-0" + sequence + ".json"), request);
        Path result = evidence.resolve("native-resource-grant-result-0" + sequence + ".json");
        while (now.plusSeconds(60).isAfter(Instant.now())) {
            if (Files.exists(result, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(result, evidence, 128 * 1024); var receipt = mapper.readTree(bytes);
                if ("CONFIRMED".equals(receipt.path("status").asText())) { validateGrantPhaseEvidence(receipt, request, ready, Instant.now()); return bytes; }
                if ("UNKNOWN_COMMIT".equals(receipt.path("status").asText())) { throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"); }
                if (!"RUNNING".equals(receipt.path("status").asText()) || !request.path("nonce").equals(receipt.path("nonce"))
                        || !request.path("previousSha256").equals(receipt.path("previousSha256"))) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
    }

    private static void nativeResourceGrantLossAndRestoration(UnifiedIdentityLiveClient.IsolatedResourceProbe client, Path root, Path evidence, Path manifestFile,
            JsonNode manifest, String digest, NativeResourceHttpProbe probe, java.sql.Connection connection, String table, JsonNode light,
            List<NativeSqlRowVO> rows, UnifiedIdentityLiveClient.FormalReferenceDTO a, UnifiedIdentityLiveClient.FormalReferenceDTO b, JsonNode readA, JsonNode readB) throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); Instant deadline = Instant.now().plusSeconds(manifest.path("deadlines").path("httpSeconds").intValue());
        Path readyFile = evidence.resolve("native-resource-grant-ready.json");
        while (!Files.exists(readyFile, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            if (!deadline.isAfter(Instant.now())) { throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE"); } Thread.sleep(100);
        }
        byte[] readyBytes = UnifiedIdentityLiveClient.protectedBytes(readyFile, evidence, 128 * 1024); var ready = mapper.readTree(readyBytes);
        String app = client.nativeApplicationCode("native-light");
        var rbac = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/db-rbac.json"), root.resolve("evidence"), 128 * 1024));
        if (!manifest.path("runId").asText().equals(ready.path("runId").asText()) || !digest.equals(ready.path("manifestSha256").asText())
                || !"RESOURCE_GRANT_OWNER_ONLY".equals(ready.path("scope").asText()) || !"READY".equals(ready.path("status").asText())
                || !a.getTenantId().equals(ready.path("tenantId").asText()) || !app.equals(ready.path("applicationCode").asText())
                || !ready.path("originalResourceIdsSha256").asText().matches("[0-9a-f]{64}") || transactionProofLong(ready, "databaseOid") != transactionProofLong(rbac, "oid")
                || !ready.path("nonce").asText().matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        String pathA = "/api/users/" + responseId(readA), pathB = "/api/users/" + responseId(readB);
        assertThat(probe.request("GET", pathA, a, null, null, 200)).isEqualTo(readA);
        assertThat(probe.request("GET", pathB, b, null, null, 200)).isEqualTo(readB);
        client.verifiedClaims(a); client.verifiedClaims(b);
        var removed = grantPhase(root, evidence, ready, UnifiedIdentityLiveClient.sha256(readyBytes), 1);
        var proofs = mapper.createArrayNode(); proofs.addObject().put("phase", "REMOVE").put("sha256", UnifiedIdentityLiveClient.sha256(removed));
        byte[] restored;
        try {
            var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
            var denied = probe.request("GET", pathA, a, null, null, 403); assertThat(denied.path("code").asText()).isEqualTo("AUTHORIZATION_DENIED");
            var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
            UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(before, after, "get", 0);
            assertThat(probe.request("GET", pathB, b, null, null, 200)).isEqualTo(readB);
            if (!a.getExpiresAt().isAfter(Instant.now())) { throw new IllegalStateException("XINGYUAN_INPUT_EXPIRED"); }
            verifyLightConnection(connection, light); assertThat(readRows(connection, table, Long.parseLong(a.getTenantId()), Long.parseLong(b.getTenantId()))).isEqualTo(rows);
            var observed = proofs.addObject().put("phase", "DENIED").put("status", 403).put("otherTenantStatus", 200); observed.set("before", before); observed.set("after", after);
        } finally { restored = grantPhase(root, evidence, ready, UnifiedIdentityLiveClient.sha256(removed), 2); }
        var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
        assertThat(probe.request("GET", pathA, a, null, null, 200)).isEqualTo(readA);
        var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
        UnifiedIdentityLiveClient.requireOnlyBusinessMethodDelta(before, after, "get", 1);
        proofs.addObject().put("phase", "RESTORE").put("sha256", UnifiedIdentityLiveClient.sha256(restored)).put("status", 200);
        verifyLightConnection(connection, light); assertThat(readRows(connection, table, Long.parseLong(a.getTenantId()), Long.parseLong(b.getTenantId()))).isEqualTo(rows);
        var result = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest).put("scope", "ACTUAL_RESOURCE_GRANT_REMOVAL_AND_RESTORE_ONLY")
                .put("status", "PASSED").put("tokenReference", a.getTokenSha256()).put("tokenExpiresAt", a.getExpiresAt().toString()).put("recordedAt", Instant.now().toString());
        result.set("proofs", proofs); XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("native-resource-grant.json"), result);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void nativeFieldRulesControlWritesAcrossRealOwnerEpochsAndTenants() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated(); var mapper = UnifiedIdentityLiveClient.strictJson();
        Path manifestFile = Path.of(System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")));
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(manifestFile, null, 128 * 1024); var manifest = mapper.readTree(bytes);
        String digest = UnifiedIdentityLiveClient.sha256(bytes); Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
        requireEvidenceDirectory(root, evidence); requireRunningProcesses(root, manifestFile, manifest, digest, evidence, java.util.Set.of("jianshen", "native-light"));
        byte[] readyBytes = UnifiedIdentityLiveClient.protectedBytes(evidence.resolve("native-field-owner-ready.json"), evidence, 128 * 1024);
        var ready = mapper.readTree(readyBytes); String app = client.nativeApplicationCode("native-light"), resource = client.resourceForProcess("native-light");
        var a = client.formal("default", resource, "admin"); var b = client.formal("tenant-b", resource, "admin"); var viewer = client.formal("default", resource, "viewer");
        var converter = org.mapstruct.factory.Mappers.getMapper(top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter.class);
        if (!converter.scopes(client.verifiedClaims(a)).containsAll(java.util.Set.of(app + ":user:create", app + ":user:read"))
                || !converter.scopes(client.verifiedClaims(b)).containsAll(java.util.Set.of(app + ":user:create", app + ":user:read"))) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        client.verifiedClaims(viewer);
        var rbac = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/db-rbac.json"), root.resolve("evidence"), 128 * 1024));
        if (!manifest.path("runId").asText().equals(ready.path("runId").asText()) || !digest.equals(ready.path("manifestSha256").asText())
                || !"READY".equals(ready.path("status").asText()) || !"FIELD_RULE_OWNER_ONLY".equals(ready.path("scope").asText())
                || !a.getTenantId().equals(ready.path("tenantId").asText()) || !app.equals(ready.path("applicationCode").asText())
                || transactionProofLong(ready, "databaseOid") != transactionProofLong(rbac, "oid")
                || !ready.path("nonce").asText().matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
                || !ready.path("inputSha256").asText().matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var profile = verifiedProfile(root, manifest, digest, "native-light");
        if (!"false".equals(profile.getProperty("app.integrations.external-http.enabled", "false"))) { throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE"); }
        profile.clear();
        var light = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/db-light.json"), root.resolve("evidence"), 128 * 1024)); requireLightReceipt(manifest, digest, light);
        long tenantA = Long.parseLong(a.getTenantId()), tenantB = Long.parseLong(b.getTenantId());
        String nameA = "xingyuan-it-" + manifest.path("runId").asText() + "-a", nameB = "xingyuan-it-" + manifest.path("runId").asText() + "-b";
        var bodyA = mapper.createObjectNode().put("externalId", "ext-1").put("name", nameA).put("email", nameA + "@xingyuan.invalid");
        var bodyB = mapper.createObjectNode().put("externalId", "ext-1").put("name", nameB).put("email", nameB + "@xingyuan.invalid");
        var ownerProofs = mapper.createArrayNode(); var counterProofs = mapper.createArrayNode(); String previous = UnifiedIdentityLiveClient.sha256(readyBytes);
        try (var connection = ownedLightConnection(manifest, light);
             var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue())).build()) {
            String table = userTable(connection);
            if (!readRows(connection, table, tenantA, tenantB).isEmpty()) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            var probe = new NativeResourceHttpProbe("native-light", root, manifestFile, digest, http, Duration.ofSeconds(manifest.path("deadlines").path("httpSeconds").intValue()));
            JsonNode createdA = null, createdB = null; List<NativeSqlRowVO> rows = List.of();
            for (int sequence = 1; sequence <= FIELD_PHASES.size(); sequence++) {
                byte[] receipt = fieldPhase(root, evidence, ready, previous, sequence); previous = UnifiedIdentityLiveClient.sha256(receipt);
                ownerProofs.add(mapper.createObjectNode().put("phase", FIELD_PHASES.get(sequence - 1)).put("sha256", previous)
                        .put("path", root.relativize(evidence.resolve("native-field-result-0" + sequence + ".json")).toString()));
                verifyLightConnection(connection, light);
                if (sequence == 2 || sequence == 3) {
                    var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
                    var denied = probe.request("POST", "/api/users", a, bodyA, null, 403);
                    assertThat(denied.path("code").asText()).isEqualTo("FIELD_ACCESS_DENIED");
                    var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
                    assertThat(after).isEqualTo(before);
                    var observed = counterProofs.addObject().put("phase", FIELD_PHASES.get(sequence - 1)).put("responseCode", "FIELD_ACCESS_DENIED");
                    observed.set("before", before); observed.set("after", after);
                    assertThat(readRows(connection, table, tenantA, tenantB)).isEqualTo(rows);
                    if (sequence == 2) {
                        createdB = probe.request("POST", "/api/users", b, bodyB, null, 200);
                        rows = readRows(connection, table, tenantA, tenantB);
                        if (rows.size() != 1 || rows.getFirst().tenantId() != tenantB || !nameB.equals(rows.getFirst().name())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
                        requireResponseMatchesSql(createdB, rows.getFirst());
                    }
                } else if (sequence == 4) {
                    var before = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
                    createdA = probe.request("POST", "/api/users", a, bodyA, null, 200);
                    var after = XingyuanNativeAuthenticationIT.invokeApplicationProbe(root, evidence, manifestFile, manifest, digest, "native-light", "default", "BUSINESS_COUNTS", null);
                    assertThat(transactionProofLong(after.path("counts"), "create")).isEqualTo(Math.addExact(transactionProofLong(before.path("counts"), "create"), 1));
                    assertThat(after.path("counts").path("get")).isEqualTo(before.path("counts").path("get"));
                    var observed = counterProofs.addObject().put("phase", "WRITE").put("responseStatus", 200);
                    observed.set("before", before); observed.set("after", after);
                    rows = readRows(connection, table, tenantA, tenantB);
                    requireOwnedRows(rows, tenantA, tenantB, responseId(createdA), responseId(createdB), nameA, nameB);
                    requireResponseMatchesSql(createdA, rows.stream().filter(row -> row.tenantId() == tenantA).findFirst().orElseThrow());
                    requireResponseMatchesSql(createdB, rows.stream().filter(row -> row.tenantId() == tenantB).findFirst().orElseThrow());
                }
            }
            if (createdA == null || createdB == null) { throw new IllegalStateException("XINGYUAN_PARTIAL"); }
            long idA = responseId(createdA), idB = responseId(createdB);
            var readA = probe.request("GET", "/api/users/" + idA, a, null, null, 200);
            var readB = probe.request("GET", "/api/users/" + idB, b, null, null, 200);
            assertThat(readA).isEqualTo(createdA); assertThat(readB).isEqualTo(createdB);
            var forged = probe.request("GET", "/api/users/" + idA, a, null, b.getTenantId(), 200); assertThat(forged).isEqualTo(readA);
            var foreign = probe.request("GET", "/api/users/" + idA, b, null, null, 400); assertThat(foreign.path("status").asText()).isEqualTo("USER_NOT_FOUND");
            probe.request("GET", "/api/users/" + idA, null, null, null, 401);
            probe.request("POST", "/api/users", viewer, bodyA, null, 403); assertThat(readRows(connection, table, tenantA, tenantB)).isEqualTo(rows);
            var isolation = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest).put("status", "PASSED")
                    .put("resourceUri", a.getResourceUri().toASCIIString()).put("tenantA", a.getTenantId()).put("tenantB", b.getTenantId())
                    .put("tenantARowId", Long.toString(idA)).put("tenantBRowId", Long.toString(idB)).put("defaultRowsHash", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(readA)))
                    .put("tenantBRowsHash", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(readB))).put("forgedHeaderRowsHash", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(forged)))
                    .put("sqlRowsSha256", UnifiedIdentityLiveClient.sha256(mapper.writeValueAsBytes(rows))).put("tokenASha256", a.getTokenSha256()).put("tokenBSha256", b.getTokenSha256())
                    .put("sqlIsolationConfirmed", true).put("observedAt", Instant.now().toString());
            client.publishNativeTenantIsolationProof(evidence, isolation);
            var result = mapper.createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText()).put("manifestSha256", digest)
                    .put("scope", "ACTUAL_FIELD_RULE_WRITE_AND_SQL_ISOLATION_ONLY").put("status", "PASSED").put("zeroWritesDuringFieldDenial", true)
                    .put("crossTenantWriteUnaffected", true).put("recordedAt", Instant.now().toString());
            result.set("ownerProofs", ownerProofs);
            result.set("counterProofs", counterProofs);
            XingyuanNativeAuthenticationIT.publishNewNativeEvidence(root, evidence.resolve("native-field-write.json"), result);
            nativeResourceGrantLossAndRestoration(client, root, evidence, manifestFile, manifest, digest, probe, connection, table, light, rows, a, b, readA, readB);
        }
    }

}
