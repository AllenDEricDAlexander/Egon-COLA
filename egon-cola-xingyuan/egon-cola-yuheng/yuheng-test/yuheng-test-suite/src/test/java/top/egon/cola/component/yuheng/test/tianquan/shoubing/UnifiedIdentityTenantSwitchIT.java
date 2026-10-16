package top.egon.cola.component.yuheng.test.tianquan.shoubing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedIdentityTenantSwitchIT {

    @Test
    void identityOnlyProbeCannotReplaceAnotherClientResourceOrReuseTheFullScopeToken(@org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary) throws Exception {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var now = java.time.Instant.parse("2026-10-15T00:00:00Z");
        var root = temporary.toRealPath(); var scoped = root.resolve("scope.at"); var metadataFile = root.resolve("scope.at.metadata.json");
        java.util.function.Supplier<UnifiedIdentityLiveClient.FormalReferenceDTO> base = () -> UnifiedIdentityLiveClient.FormalReferenceDTO.builder()
                .realmCode("default").resourceKey("owned-resource").tenantId("1001").subject("owned-subject").clientId("owned-client").clientPk("51")
                .issuer(java.net.URI.create("https://127.0.0.1:28443/tenants/1001")).resourceUri(java.net.URI.create("https://127.0.0.1:28140"))
                .readEndpoint(java.net.URI.create("https://127.0.0.1:28140/api/v1/yuheng/admin/scopes")).tokenFile(root.resolve("full.at"))
                .tokenSha256("a".repeat(64)).expiresAt(now.plusSeconds(90)).scopes(java.util.Set.of("openid", "yuheng:read")).build();
        var metadata = mapper.createObjectNode().put("schemaVersion", 1).put("runId", "owned-run").put("manifestSha256", "b".repeat(64)).put("scope", "IDENTITY_ONLY_PUBLIC_CODE");
        var token = metadata.putObject("token").put("tenantCode", "default").put("tenantId", "1001").put("resourceKey", "owned-resource").put("userKey", "admin")
                .put("clientId", "owned-client").put("clientPk", "51").put("issuer", "https://127.0.0.1:28443/tenants/1001").put("resourceUri", "https://127.0.0.1:28140")
                .put("tokenFile", scoped.toString()).putNull("probeStatus").put("tokenSha256", "c".repeat(64)).put("expiresAt", now.plusSeconds(120).toString());
        token.putArray("scopes").add("openid");
        var actual = UnifiedIdentityLiveClient.identityOnlyReference(metadata, base.get(), "owned-run", "b".repeat(64), metadataFile, now);
        assertThat(actual.getScopes()).containsExactly("openid"); assertThat(actual.getTokenFile()).isEqualTo(scoped);
        for (String field : java.util.List.of("tenantId", "clientId", "clientPk", "issuer", "resourceUri", "resourceKey", "userKey", "tokenFile")) {
            var wrong = metadata.deepCopy(); wrong.withObject("token").put(field, "foreign");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.identityOnlyReference(wrong, base.get(), "owned-run", "b".repeat(64), metadataFile, now)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        for (String fault : java.util.List.of("widened", "reused", "secret", "oldRun", "fractionalSchema")) {
            var wrong = metadata.deepCopy(); var item = wrong.withObject("token");
            if (fault.equals("widened")) { item.withArray("scopes").add("yuheng:read"); }
            else if (fault.equals("reused")) { item.put("tokenSha256", "a".repeat(64)); }
            else if (fault.equals("secret")) { item.put("accessToken", "forbidden"); }
            else if (fault.equals("oldRun")) { wrong.put("runId", "old"); }
            else { wrong.put("schemaVersion", 1.5); }
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.identityOnlyReference(wrong, base.get(), "owned-run", "b".repeat(64), metadataFile, now)).hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        var expired = metadata.deepCopy(); expired.withObject("token").put("expiresAt", now.minusSeconds(1).toString());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.identityOnlyReference(expired, base.get(), "owned-run", "b".repeat(64), metadataFile, now)).hasMessage("XINGYUAN_INPUT_EXPIRED");
    }

    @Test
    void rpcCounterProofsCannotHideAnAnonymousHandlerOrAnUnrelatedWrite() {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        for (String role : java.util.List.of("native-web", "native-service")) {
            var methods = role.equals("native-web") ? java.util.Set.of("createUser", "getUser") : java.util.Set.of("create", "get", "page", "schedule");
            String get = role.equals("native-web") ? "getUser" : "get", write = role.equals("native-web") ? "createUser" : "create";
            var proof = mapper.createObjectNode();
            for (String phase : java.util.List.of("beforeBusiness", "successfulBusiness", "beforeMissingToken", "afterMissingToken")) {
                var counts = proof.putObject(phase).putObject("counts"); methods.forEach(method -> counts.put(method, method.equals(get) && !phase.equals("beforeBusiness") ? 11 : 10));
            }
            org.assertj.core.api.Assertions.assertThatCode(() -> UnifiedIdentityLiveClient.validateRpcProviderCounterProofs(proof, role, 0)).doesNotThrowAnyException();
            for (String fault : java.util.List.of("anonymousHandler", "unrelatedWrite", "missingMethod", "fractional", "secret", "overflow")) {
                var invalid = proof.deepCopy(); var counts = invalid.withObject("afterMissingToken").withObject("counts");
                if (fault.equals("anonymousHandler")) { counts.put(get, 12); }
                else if (fault.equals("unrelatedWrite")) { counts.put(write, 11); }
                else if (fault.equals("missingMethod")) { counts.remove(write); }
                else if (fault.equals("fractional")) { counts.put(get, 11.5); }
                else if (fault.equals("secret")) { invalid.withObject("beforeBusiness").put("accessToken", "forbidden"); }
                else { invalid.withObject("beforeBusiness").withObject("counts").put(get, Long.MAX_VALUE); }
                org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.validateRpcProviderCounterProofs(invalid, role, 0)).hasMessage("XINGYUAN_VERIFICATION_FAILED");
            }
            for (String phase : java.util.List.of("beforeMissingToken", "afterMissingToken")) { proof.withObject(phase).withObject("counts").put(get, 12); }
            org.assertj.core.api.Assertions.assertThatCode(() -> UnifiedIdentityLiveClient.validateRpcProviderCounterProofs(proof, role, 1)).doesNotThrowAnyException();
        }
    }

    @Test
    void serviceRpcDenialProofRejectsHandlerGrowthNaturalExpiryAndARevocationClaimWithoutOwnerEvidence() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var now = java.time.Instant.parse("2026-10-15T00:00:00Z");
        var proof = mapper.createObjectNode().put("scopeDeniedStatus", "PERMISSION_DENIED").put("revokedStatus", "UNAUTHENTICATED")
                .put("scopeMethodReference", "a".repeat(64)).put("tokenReference", "b".repeat(64)).put("authorizationReference", "c".repeat(64))
                .put("ownerReceiptSha256", "d".repeat(64)).put("revocationAuditSha256", "e".repeat(64)).put("tokenExpiresAt", now.plusSeconds(60).toString());
        for (String phase : java.util.List.of("beforeScope", "afterScope", "beforeRevoked", "afterRevoked")) { proof.putObject(phase).putObject("counts").put("createUser", 0).put("getUser", 1); }
        proof.putObject("scopeDeniedRequest").put("traceId", "1".repeat(32)).put("status", "PERMISSION_DENIED").put("startedAt", now.minusSeconds(2).toString()).put("completedAt", now.minusSeconds(1).toString());
        proof.putObject("revokedRequest").put("traceId", "2".repeat(32)).put("status", "UNAUTHENTICATED").put("startedAt", now.minusSeconds(1).toString()).put("completedAt", now.toString());
        org.assertj.core.api.Assertions.assertThatCode(() -> UnifiedIdentityLiveClient.validateRpcServiceSecurity(proof, "native-web", now, java.time.Duration.ofSeconds(120))).doesNotThrowAnyException();
        for (String fault : java.util.List.of("scopeHandler", "revokedHandler", "expired", "receipt", "wrongStatus", "audit", "sameTrace", "future", "clock", "requestStatus", "secret")) {
            var invalid = proof.deepCopy();
            if (fault.equals("scopeHandler")) { invalid.withObject("afterScope").withObject("counts").put("createUser", 1); }
            else if (fault.equals("revokedHandler")) { invalid.withObject("afterRevoked").withObject("counts").put("getUser", 2); }
            else if (fault.equals("expired")) { invalid.put("tokenExpiresAt", now.toString()); }
            else if (fault.equals("receipt")) { invalid.remove("ownerReceiptSha256"); }
            else if (fault.equals("wrongStatus")) { invalid.put("revokedStatus", "NOT_FOUND"); }
            else if (fault.equals("audit")) { invalid.remove("revocationAuditSha256"); }
            else if (fault.equals("sameTrace")) { invalid.withObject("revokedRequest").put("traceId", "1".repeat(32)); }
            else if (fault.equals("future")) { invalid.withObject("revokedRequest").put("completedAt", now.plusSeconds(1).toString()); }
            else if (fault.equals("clock")) { invalid.withObject("scopeDeniedRequest").put("completedAt", now.minusSeconds(3).toString()); }
            else if (fault.equals("requestStatus")) { invalid.withObject("revokedRequest").put("status", "OK"); }
            else { invalid.put("accessToken", "forbidden"); }
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.validateRpcServiceSecurity(invalid, "native-web", now, java.time.Duration.ofSeconds(120))).hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
    }

    @Test
    void platformAliasSelectsOnlyTheTwoRealRealmClientsAndTheirSingletonResource() {
        var mapper = UnifiedIdentityLiveClient.strictJson(); var registry = mapper.createObjectNode();
        var clients = registry.putArray("clients"); var realms = registry.putObject("realms");
        int index = 1;
        for (String realm : java.util.List.of("default", "tenant-b")) {
            String issuer = "https://127.0.0.1:28443/tenants/" + index;
            realms.putObject(realm).put("issuer", issuer);
            clients.addObject().put("alias", "edge-yuheng").put("realmCode", realm).put("issuer", issuer).put("clientId", "owned-" + index).put("clientPk", Integer.toString(index++))
                    .put("status", "ACTIVE").put("clientType", "CONFIDENTIAL").put("purpose", "USER").putArray("resourceUris").add("https://owned.invalid/yuheng");
        }
        registry.putArray("resources").addObject().put("resourceServerId", "actual-yuheng").put("status", "ACTIVE").put("resourceUri", "https://owned.invalid/yuheng");
        registry.putArray("cliResources").addObject().put("resourceServerId", "actual-yuheng").put("resourceKey", "yuheng-input");
        assertThat(UnifiedIdentityLiveClient.registryResourceForAlias(registry, "edge-yuheng")).isEqualTo("yuheng-input");
        for (String fault : java.util.List.of("missingB", "wrongIssuer", "differentResource", "sharedClient", "public", "missingResource", "duplicateBinding", "invalidKey")) {
            var invalid = registry.deepCopy();
            if (fault.equals("missingB")) { invalid.withArray("clients").remove(1); }
            else if (fault.equals("wrongIssuer")) { ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("clients").get(1)).put("issuer", "https://foreign.invalid"); }
            else if (fault.equals("differentResource")) { ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("clients").get(1)).putArray("resourceUris").add("https://foreign.invalid"); }
            else if (fault.equals("sharedClient")) { ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("clients").get(1)).put("clientId", "owned-1"); }
            else if (fault.equals("public")) { ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("clients").get(1)).put("clientType", "PUBLIC"); }
            else if (fault.equals("missingResource")) { invalid.withArray("resources").removeAll(); }
            else if (fault.equals("duplicateBinding")) { invalid.withArray("cliResources").add(invalid.path("cliResources").get(0).deepCopy()); }
            else { ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("cliResources").get(0)).put("resourceKey", "../foreign"); }
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.registryResourceForAlias(invalid, "edge-yuheng"))
                    .hasMessage("XINGYUAN_TARGET_UNSAFE");
        }
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.registryResourceForAlias(registry, "edge-ui")).hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "XINGYUAN_ISOLATED_INTEGRATION", matches = "true")
    void tenantSwitchKeepsTheIdentitySubjectButIssuesATenantScopedToken() throws Exception {
        var client = UnifiedIdentityLiveClient.isolated();
        String resource = client.resourceForProcess("native-light");
        var first = client.formal("default", resource, "admin");
        var second = client.formal("tenant-b", resource, "admin");
        client.bindNativeTenantReadEndpoints(first, second);
        var firstJwt = client.verifiedClaims(first);
        var secondJwt = client.verifiedClaims(second);
        // These are independently acquired PUBLIC families; the BFF A/B proof remains a separate browser journey.
        assertThat(firstJwt.getSubject().equals(secondJwt.getSubject())).isTrue();
        assertThat(!firstJwt.getClaimAsString("tenant_id").equals(secondJwt.getClaimAsString("tenant_id"))).isTrue();
        assertThat(!firstJwt.getIssuer().equals(secondJwt.getIssuer())).isTrue();
        assertThat(firstJwt.getAudience().equals(secondJwt.getAudience())).isTrue();
        assertThat(client.withoutToken(first)).isEqualTo(401);
        assertThat(client.read(first)).isEqualTo(200);
        assertThat(client.read(second)).isEqualTo(200);
        assertThat(client.wrongTenantHeaderDoesNotSelectAnotherContext(first, second)).isTrue();
        client.requireNativeTenantIsolationProof(first, second);
    }
    @Test
    void formalInputReadersRejectSharedPermissionsAndSymbolicReferences(@org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary) throws Exception {
        var root = temporary.toRealPath();
        java.nio.file.Files.setPosixFilePermissions(root, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        var input = root.resolve("formal.at");
        java.nio.file.Files.writeString(input, "controlled-not-a-live-token");
        java.nio.file.Files.setPosixFilePermissions(input, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        assertThat(UnifiedIdentityLiveClient.protectedBytes(input, root, 128)).isNotEmpty();
        java.nio.file.Files.setPosixFilePermissions(input, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.protectedBytes(input, root, 128))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
        java.nio.file.Files.setPosixFilePermissions(input, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        var link = root.resolve("symbolic.at");
        java.nio.file.Files.createSymbolicLink(link, input);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.protectedBytes(link, root, 128))
                .hasMessage("XINGYUAN_TARGET_UNSAFE");
    }

    @Test
    void formalMetadataRejectsDuplicateAndTrailingJsonBeforeAnyRequest() {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.readTree("{\"runId\":\"a\",\"runId\":\"b\"}"))
                .isInstanceOf(java.io.IOException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.readTree("{}{}"))
                .isInstanceOf(java.io.IOException.class);
    }

    @Test
    void nativeSqlProofRejectsAnotherFormalFamilyAndStaleOrFutureObservations() {
        var mapper = UnifiedIdentityLiveClient.strictJson();
        var now = java.time.Instant.parse("2026-10-15T00:00:00Z");
        var age = java.time.Duration.ofSeconds(30);
        String run = "controlled-run", digest = "d".repeat(64);
        var first = UnifiedIdentityLiveClient.FormalReferenceDTO.builder().tenantId("101")
                .resourceUri(java.net.URI.create("urn:controlled:light")).tokenSha256("a".repeat(64)).build();
        var second = UnifiedIdentityLiveClient.FormalReferenceDTO.builder().tenantId("102")
                .resourceUri(first.getResourceUri()).tokenSha256("b".repeat(64)).build();
        var proof = mapper.createObjectNode().put("schemaVersion", 1).put("runId", run).put("manifestSha256", digest)
                .put("status", "PASSED").put("resourceUri", first.getResourceUri().toASCIIString()).put("tenantA", "101").put("tenantB", "102")
                .put("tenantARowId", "11").put("tenantBRowId", "12")
                .put("tokenASha256", first.getTokenSha256()).put("tokenBSha256", second.getTokenSha256())
                .put("defaultRowsHash", "1".repeat(64)).put("tenantBRowsHash", "2".repeat(64)).put("forgedHeaderRowsHash", "1".repeat(64))
                .put("sqlRowsSha256", "3".repeat(64)).put("sqlIsolationConfirmed", true).put("observedAt", now.toString());
        org.assertj.core.api.Assertions.assertThatCode(() -> UnifiedIdentityLiveClient.validateNativeTenantIsolationProof(proof, run, digest, first, second, now, age))
                .doesNotThrowAnyException();
        for (String field : java.util.List.of("tokenASha256", "tokenBSha256", "manifestSha256", "tenantA", "sqlRowsSha256", "tenantARowId")) {
            var foreign = proof.deepCopy().put(field, "foreign");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.validateNativeTenantIsolationProof(foreign, run, digest, first, second, now, age))
                    .hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        for (var instant : java.util.List.of(now.minusSeconds(31), now.plusSeconds(1))) {
            var stale = proof.deepCopy().put("observedAt", instant.toString());
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.validateNativeTenantIsolationProof(stale, run, digest, first, second, now, age))
                    .hasMessage("XINGYUAN_VERIFICATION_FAILED");
        }
        var overflow = proof.deepCopy().put("schemaVersion", 4294967297L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> UnifiedIdentityLiveClient.validateNativeTenantIsolationProof(overflow, run, digest, first, second, now, age))
                .hasMessage("XINGYUAN_VERIFICATION_FAILED");
    }

}
