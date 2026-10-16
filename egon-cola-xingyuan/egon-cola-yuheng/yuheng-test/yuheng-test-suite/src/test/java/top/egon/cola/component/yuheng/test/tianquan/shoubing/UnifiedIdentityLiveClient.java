package top.egon.cola.component.yuheng.test.tianquan.shoubing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class UnifiedIdentityLiveClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    static UnifiedIdentityLiveClient enabled() {
        Assumptions.assumeTrue(Boolean.parseBoolean(
                System.getenv().getOrDefault("UNIFIED_IDENTITY_LIVE", "false")));
        return new UnifiedIdentityLiveClient();
    }

    int get(String baseUrl, String path, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    String token(String envName) throws IOException {
        return Files.readString(Path.of(requiredEnv(envName)), StandardCharsets.UTF_8)
                .trim();
    }

    JsonNode claims(String token) throws IOException {
        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            throw new IllegalArgumentException("JWT must have three segments");
        }
        byte[] payload = Base64.getUrlDecoder().decode(segments[1]);
        return JSON.readTree(payload);
    }
    public static IsolatedResourceProbe isolated() throws Exception {
        if (!"true".equals(System.getenv("XINGYUAN_ISOLATED_INTEGRATION"))) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
        String manifestValue = System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST"));
        String repoValue = System.getProperty("xingyuan.isolated.repository-root");
        if (manifestValue == null || repoValue == null) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
        Path repository = Path.of(repoValue).toRealPath();
        Path manifestFile = Path.of(manifestValue);
        byte[] bytes = protectedBytes(manifestFile, null, 128 * 1024);
        var mapper = strictJson();
        JsonNode manifest = mapper.readTree(bytes);
        String digest = sha256(bytes);
        Path root = Path.of(manifest.path("runtimeRoot").asText());
        Path indexFile = root.resolve("secrets/token-index.json");
        var process = new ProcessBuilder("python3", repository.resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(),
                "validate", manifestFile.toString(), repository.toString(), root.resolve("evidence/legacy-live-validation.json").toString(), indexFile.toString(), "")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroy(); throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
        }
        if (process.exitValue() != 0) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        JsonNode registry = mapper.readTree(protectedBytes(root.resolve("secrets/runtime-registry.json"), root.resolve("secrets"), 2 * 1024 * 1024));
        JsonNode index = mapper.readTree(protectedBytes(indexFile, root.resolve("secrets"), 128 * 1024));
        for (var value : java.util.List.of(registry, index)) {
            if (!value.path("schemaVersion").isIntegralNumber() || !value.path("schemaVersion").canConvertToInt() || value.path("schemaVersion").intValue() != 1 || !manifest.path("runId").asText().equals(value.path("runId").asText())
                    || !digest.equals(value.path("manifestSha256").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        }
        if (!registry.path("clients").isArray() || registry.path("clients").size() > 192 || !registry.path("resources").isArray()
                || registry.path("resources").size() > 64 || !registry.path("cliResources").isArray() || registry.path("cliResources").size() > 32
                || !index.path("tokens").isArray() || index.path("tokens").size() > 192) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        Path profile = root.resolve("profiles/shoubing.properties");
        byte[] profileBytes = protectedBytes(profile, root.resolve("profiles"), 2 * 1024 * 1024);
        var ready = mapper.readTree(protectedBytes(root.resolve("evidence/profiles-ready.json"), root.resolve("evidence"), 128 * 1024));
        if (!digest.equals(ready.path("manifestSha256").asText()) || !manifest.path("runId").asText().equals(ready.path("runId").asText())
                || !profile.toString().equals(ready.path("profiles").path("shoubing").path("path").asText())
                || !sha256(profileBytes).equals(ready.path("profiles").path("shoubing").path("sha256").asText())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var properties = new java.util.Properties();
        properties.load(new java.io.StringReader(new String(profileBytes, StandardCharsets.UTF_8)));
        java.util.Map<String, Object> flat = new java.util.LinkedHashMap<>();
        properties.forEach((key, value) -> flat.put(key.toString(), value));
        var sdk = new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(flat))
                .bind("egon.cola.platform.tianquan.shoubing", org.springframework.boot.context.properties.bind.Bindable.of(
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class)).orElseThrow(() -> new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"));
        var trusted = new top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry(sdk);
        var verifier = new top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent(trusted, sdk, java.time.Clock.systemUTC());
        verifier.initialize();
        int seconds = manifest.path("deadlines").path("httpSeconds").asInt();
        return new IsolatedResourceProbe(root, manifest, registry, index, digest, verifier, Duration.ofSeconds(seconds),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(seconds)).followRedirects(HttpClient.Redirect.NEVER).build());
    }

    public static ObjectMapper strictJson() {
        return org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
                .featuresToEnable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION,
                        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToDisable(com.fasterxml.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS).build();
    }

    public record HttpAuditObservationVO(String traceId, int status,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant startedAt,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) Instant completedAt) { }

    public static HttpAuditObservationVO observedHttpCall(HttpResponse<?> response, String trace, Instant started) {
        var headers = response.headers().allValues("traceparent");
        if (!trace.matches("[0-9a-f]{32}") || trace.equals("0".repeat(32)) || headers.size() != 1
                || !headers.getFirst().matches("00-" + trace + "-[0-9a-f]{16}-0[01]")
                || headers.getFirst().substring(36, 52).equals("0".repeat(16)) || started.isAfter(Instant.now())) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        return new HttpAuditObservationVO(trace, response.statusCode(), started, Instant.now());
    }

    public static HttpResponse.BodyHandler<Void> boundedBody(ByteArrayOutputStream output) {
        return HttpResponse.BodyHandlers.ofByteArrayConsumer(part -> part.ifPresent(bytes -> {
            if (bytes.length > 128 * 1024 - output.size()) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            output.writeBytes(bytes);
        }));
    }

    public static byte[] protectedBytes(Path file, Path root, int maximum) throws IOException {
        if (!file.isAbsolute() || !file.equals(file.normalize()) || root != null && (file.equals(root) || !file.startsWith(root))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        var owner = file.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
        for (Path current = file; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            if (root != null && current.startsWith(root) && !current.equals(file)
                    && (!Files.isDirectory(current) || !Files.getOwner(current).equals(owner)
                    || !Files.getPosixFilePermissions(current).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }
        var before = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.size() > maximum || !Files.getOwner(file).equals(owner)
                || !Files.getPosixFilePermissions(file).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        byte[] value;
        try (var source = Files.newInputStream(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { value = source.readNBytes(maximum + 1); }
        var after = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
        if (value.length > maximum || !java.util.Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return value;
    }

    public static List<String> activeNativeRuntimeRoles(Path root, JsonNode manifest, String digest) throws Exception {
        List<String> all = List.of("native-light", "native-web", "native-service", "native-agent");
        Path policy = root.resolve("policies/agent-runtime-deferred.json");
        var references = java.util.stream.StreamSupport.stream(manifest.path("policyFiles").spliterator(), false)
                .filter(value -> policy.toString().equals(value.path("path").asText())).toList();
        if (references.isEmpty()) {
            if (Files.exists(policy, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            return all;
        }
        if (references.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        byte[] bytes = UnifiedIdentityLiveClient.protectedBytes(policy, root.resolve("policies"), 128 * 1024);
        String hash = UnifiedIdentityLiveClient.sha256(bytes); var mapper = UnifiedIdentityLiveClient.strictJson(); var value = mapper.readTree(bytes);
        var fields = new HashSet<String>(); value.fieldNames().forEachRemaining(fields::add);
        if (!hash.equals(references.getFirst().path("sha256").asText())
                || !fields.equals(Set.of("schemaVersion", "runId", "role", "status", "reasonCode", "acceptedOn"))
                || !value.path("schemaVersion").isIntegralNumber() || !value.path("schemaVersion").canConvertToInt() || value.path("schemaVersion").intValue() != 1
                || !manifest.path("runId").asText().equals(value.path("runId").asText()) || !"native-agent".equals(value.path("role").asText())
                || !"UNVERIFIED".equals(value.path("status").asText()) || !"AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED".equals(value.path("reasonCode").asText())
                || !"2026-10-15".equals(value.path("acceptedOn").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var ready = mapper.readTree(UnifiedIdentityLiveClient.protectedBytes(root.resolve("evidence/profiles-ready.json"), root.resolve("evidence"), 128 * 1024));
        var exclusions = ready.path("runtimeExclusions");
        if (!manifest.path("runId").asText().equals(ready.path("runId").asText()) || !digest.equals(ready.path("manifestSha256").asText())
                || !exclusions.isObject() || exclusions.size() != 1 || !hash.equals(exclusions.path("native-agent").asText())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return all.subList(0, 3);
    }

    public static String sha256(byte[] value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    static String registryResourceForAlias(JsonNode registry, String alias) {
        if (!java.util.Set.of("edge-shoubing", "edge-jianshen", "edge-tianshu", "edge-yuheng").contains(alias)
                || registry == null || !registry.path("clients").isArray() || !registry.path("resources").isArray()
                || !registry.path("cliResources").isArray()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var clients = java.util.stream.StreamSupport.stream(registry.path("clients").spliterator(), false)
                .filter(value -> alias.equals(value.path("alias").asText())).toList();
        if (clients.size() != 2) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var realms = new java.util.HashSet<String>(); var ids = new java.util.HashSet<String>(); var primaryKeys = new java.util.HashSet<String>();
        String resourceUri = null;
        for (var client : clients) {
            String realm = client.path("realmCode").asText(), id = client.path("clientId").asText(), pk = client.path("clientPk").asText();
            String issuer = registry.path("realms").path(realm).path("issuer").asText();
            if (!java.util.Set.of("default", "tenant-b").contains(realm) || !realms.add(realm) || id.isBlank() || !ids.add(id)
                    || !pk.matches("[1-9][0-9]{0,18}") || new java.math.BigInteger(pk).bitLength() > 63 || !primaryKeys.add(pk)
                    || issuer.isBlank() || !issuer.equals(client.path("issuer").asText()) || !"ACTIVE".equals(client.path("status").asText())
                    || !"CONFIDENTIAL".equals(client.path("clientType").asText()) || !"USER".equals(client.path("purpose").asText())
                    || !client.path("resourceUris").isArray() || client.path("resourceUris").size() != 1
                    || !client.path("resourceUris").get(0).isTextual() || client.path("resourceUris").get(0).asText().isBlank()) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            String current = client.path("resourceUris").get(0).asText();
            if (resourceUri != null && !resourceUri.equals(current)) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            resourceUri = current;
        }
        String expectedUri = resourceUri;
        var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                .filter(value -> expectedUri.equals(value.path("resourceUri").asText()) && "ACTIVE".equals(value.path("status").asText())).toList();
        if (resources.size() != 1 || resources.getFirst().path("resourceServerId").asText().isBlank()) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        String resourceId = resources.getFirst().path("resourceServerId").asText();
        var bindings = java.util.stream.StreamSupport.stream(registry.path("cliResources").spliterator(), false)
                .filter(value -> resourceId.equals(value.path("resourceServerId").asText())).toList();
        if (bindings.size() != 1 || !bindings.getFirst().path("resourceKey").asText().matches("[a-z][a-z0-9-]{2,63}")) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return bindings.getFirst().path("resourceKey").asText();
    }

    static void validateRpcProviderCounterProofs(JsonNode item, String peer, int crossTenantReads) {
        var methods = switch (peer) {
            case "native-web" -> java.util.Set.of("createUser", "getUser");
            case "native-service" -> java.util.Set.of("create", "get", "page", "schedule");
            default -> throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
        };
        if (crossTenantReads < 0 || crossTenantReads > 1) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
        var snapshots = new java.util.ArrayList<java.util.Map<String, Long>>();
        for (String phase : java.util.List.of("beforeBusiness", "successfulBusiness", "beforeMissingToken", "afterMissingToken")) {
            var value = item.path(phase); var counts = value.path("counts"); var keys = new java.util.HashSet<String>(); counts.fieldNames().forEachRemaining(keys::add);
            if (!value.isObject() || value.size() != 1 || !counts.isObject() || !keys.equals(methods)) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            var snapshot = new java.util.LinkedHashMap<String, Long>();
            for (String method : methods) {
                var count = counts.get(method);
                if (!count.isIntegralNumber() || !count.canConvertToLong() || count.longValue() < 0) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
                snapshot.put(method, count.longValue());
            }
            snapshots.add(java.util.Map.copyOf(snapshot));
        }
        String operation = peer.equals("native-web") ? "getUser" : "get";
        for (String method : methods) {
            long first = snapshots.get(0).get(method), second = snapshots.get(1).get(method);
            long successDelta = method.equals(operation) ? 1 : 0, tenantDelta = method.equals(operation) ? crossTenantReads : 0;
            if (first > Long.MAX_VALUE - successDelta || second != first + successDelta
                    || second > Long.MAX_VALUE - tenantDelta || snapshots.get(2).get(method) != second + tenantDelta) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
        if (!snapshots.get(2).equals(snapshots.get(3))) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    public static void requireOnlyBusinessMethodDelta(JsonNode before, JsonNode after, String method, long delta) {
        var first = before.path("counts"); var second = after.path("counts");
        var fields = new HashSet<String>(); first.fieldNames().forEachRemaining(fields::add);
        var actual = new HashSet<String>(); second.fieldNames().forEachRemaining(actual::add);
        if (!before.isObject() || before.size() != 1 || !after.isObject() || after.size() != 1 || !first.isObject() || !second.isObject()
                || fields.isEmpty() || !fields.equals(actual) || !fields.contains(method) || delta < 0 || delta > 2) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
        for (String name : fields) {
            var start = first.path(name); var finish = second.path(name); long change = name.equals(method) ? delta : 0;
            if (!start.isIntegralNumber() || !start.canConvertToLong() || start.longValue() < 0 || !finish.isIntegralNumber() || !finish.canConvertToLong()
                    || finish.longValue() < 0 || start.longValue() > Long.MAX_VALUE - change || finish.longValue() != start.longValue() + change) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }
    }

    static FormalReferenceDTO identityOnlyReference(JsonNode metadata, FormalReferenceDTO canonical, String runId, String digest, Path metadataFile, java.time.Instant now) {
        if (metadata == null) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        var fields = new java.util.HashSet<String>(); metadata.fieldNames().forEachRemaining(fields::add);
        var token = metadata.path("token"); var tokenFields = new java.util.HashSet<String>(); token.fieldNames().forEachRemaining(tokenFields::add);
        if (!metadata.isObject() || !fields.equals(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "scope", "token"))
                || !metadata.path("schemaVersion").isInt() || metadata.path("schemaVersion").intValue() != 1 || !runId.equals(metadata.path("runId").asText())
                || !digest.equals(metadata.path("manifestSha256").asText()) || !"IDENTITY_ONLY_PUBLIC_CODE".equals(metadata.path("scope").asText())
                || !token.isObject() || !tokenFields.equals(java.util.Set.of("tenantCode", "tenantId", "resourceKey", "userKey", "clientId", "clientPk", "issuer", "resourceUri", "scopes", "tokenFile", "probeStatus", "tokenSha256", "expiresAt"))
                || !canonical.getRealmCode().equals(token.path("tenantCode").asText()) || !canonical.getTenantId().equals(token.path("tenantId").asText())
                || !canonical.getResourceKey().equals(token.path("resourceKey").asText()) || !"admin".equals(token.path("userKey").asText())
                || !canonical.getClientId().equals(token.path("clientId").asText()) || !canonical.getClientPk().equals(token.path("clientPk").asText())
                || !canonical.getIssuer().toASCIIString().equals(token.path("issuer").asText()) || !canonical.getResourceUri().toASCIIString().equals(token.path("resourceUri").asText())
                || !token.path("scopes").isArray() || token.path("scopes").size() != 1 || !"openid".equals(token.path("scopes").get(0).asText())
                || !token.path("probeStatus").isNull() || !token.path("tokenSha256").asText().matches("[0-9a-f]{64}")
                || canonical.getTokenSha256().equals(token.path("tokenSha256").asText()) || !token.path("tokenFile").isTextual()
                || !metadataFile.toString().equals(token.path("tokenFile").asText() + ".metadata.json") || !token.path("expiresAt").isTextual()
                || !token.path("expiresAt").asText().endsWith("Z")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        java.time.Instant expiry;
        try { expiry = java.time.Instant.parse(token.path("expiresAt").asText()); }
        catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE", invalid); }
        if (!expiry.isAfter(now)) { throw new IllegalArgumentException("XINGYUAN_INPUT_EXPIRED"); }
        Path file = Path.of(token.path("tokenFile").asText());
        if (!file.isAbsolute() || !file.equals(file.normalize()) || file.equals(canonical.getTokenFile())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
        return canonical.setTokenFile(file).setTokenSha256(token.path("tokenSha256").asText()).setExpiresAt(expiry).setScopes(java.util.Set.of("openid"));
    }

    static String validateRpcAuditObservation(JsonNode value, String expected, java.time.Instant now, Duration maximumAge) {
        var fields = new java.util.HashSet<String>(); value.fieldNames().forEachRemaining(fields::add);
        if (!value.isObject() || !fields.equals(java.util.Set.of("traceId", "status", "startedAt", "completedAt"))
                || !value.path("traceId").isTextual() || !value.path("traceId").asText().matches("(?!0{32})[0-9a-f]{32}")
                || !value.path("status").isTextual() || !expected.equals(value.path("status").asText())
                || !java.util.Set.of("OK", "UNAUTHENTICATED", "PERMISSION_DENIED").contains(expected)
                || maximumAge == null || maximumAge.isNegative() || maximumAge.isZero()
                || !value.path("startedAt").isTextual() || !value.path("startedAt").asText().endsWith("Z")
                || !value.path("completedAt").isTextual() || !value.path("completedAt").asText().endsWith("Z")) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
        java.time.Instant started, completed;
        try { started = java.time.Instant.parse(value.path("startedAt").asText()); completed = java.time.Instant.parse(value.path("completedAt").asText()); }
        catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED", invalid); }
        if (started.isBefore(now.minus(maximumAge)) || completed.isAfter(now) || completed.isBefore(started)) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
        return value.path("traceId").asText();
    }

    static void validateRpcServiceSecurity(JsonNode value, String peer, java.time.Instant now, Duration maximumAge) {
        var fields = new java.util.HashSet<String>(); value.fieldNames().forEachRemaining(fields::add);
        if (!value.isObject() || !fields.equals(java.util.Set.of("scopeDeniedStatus", "revokedStatus", "scopeMethodReference", "tokenReference", "authorizationReference", "ownerReceiptSha256", "tokenExpiresAt",
                "revocationAuditSha256", "scopeDeniedRequest", "revokedRequest", "beforeScope", "afterScope", "beforeRevoked", "afterRevoked")) || !"PERMISSION_DENIED".equals(value.path("scopeDeniedStatus").asText())
                || !"UNAUTHENTICATED".equals(value.path("revokedStatus").asText())) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
        for (String hash : java.util.List.of("scopeMethodReference", "tokenReference", "authorizationReference", "ownerReceiptSha256", "revocationAuditSha256")) {
            if (!value.path(hash).asText().matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
        }
        java.time.Instant expiry;
        try { expiry = java.time.Instant.parse(value.path("tokenExpiresAt").asText()); }
        catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED", invalid); }
        if (!value.path("tokenExpiresAt").asText().endsWith("Z") || !expiry.isAfter(now)) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
        String scopeTrace = validateRpcAuditObservation(value.path("scopeDeniedRequest"), "PERMISSION_DENIED", now, maximumAge);
        String revokedTrace = validateRpcAuditObservation(value.path("revokedRequest"), "UNAUTHENTICATED", now, maximumAge);
        if (scopeTrace.equals(revokedTrace)) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
        var methods = peer.equals("native-web") ? java.util.Set.of("createUser", "getUser") : peer.equals("native-service") ? java.util.Set.of("create", "get", "page", "schedule") : java.util.Set.<String>of();
        if (methods.isEmpty()) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
        for (String phase : java.util.List.of("beforeScope", "afterScope", "beforeRevoked", "afterRevoked")) {
            var snapshot = value.path(phase); var counts = snapshot.path("counts"); var names = new java.util.HashSet<String>(); counts.fieldNames().forEachRemaining(names::add);
            if (!snapshot.isObject() || snapshot.size() != 1 || !counts.isObject() || !names.equals(methods)) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            for (var count : counts) { if (!count.isIntegralNumber() || !count.canConvertToLong() || count.longValue() < 0) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); } }
        }
        if (!value.path("beforeScope").equals(value.path("afterScope")) || !value.path("beforeRevoked").equals(value.path("afterRevoked"))) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
    }

    static void validateNativeTenantIsolationProof(JsonNode proof, String runId, String digest, FormalReferenceDTO first,
                                                  FormalReferenceDTO second, java.time.Instant now, Duration maximumAge) {
        if (!proof.path("schemaVersion").isIntegralNumber() || !proof.path("schemaVersion").canConvertToInt() || proof.path("schemaVersion").intValue() != 1
                || !runId.equals(proof.path("runId").asText()) || !digest.equals(proof.path("manifestSha256").asText())
                || !"PASSED".equals(proof.path("status").asText()) || !first.getResourceUri().toASCIIString().equals(proof.path("resourceUri").asText())
                || !first.getResourceUri().equals(second.getResourceUri()) || first.getTenantId().equals(second.getTenantId())
                || !first.getTenantId().equals(proof.path("tenantA").asText()) || !second.getTenantId().equals(proof.path("tenantB").asText())
                || !first.getTokenSha256().equals(proof.path("tokenASha256").asText()) || !second.getTokenSha256().equals(proof.path("tokenBSha256").asText())
                || !proof.path("defaultRowsHash").asText().matches("[0-9a-f]{64}") || !proof.path("tenantBRowsHash").asText().matches("[0-9a-f]{64}")
                || !proof.path("sqlRowsSha256").asText().matches("[0-9a-f]{64}")
                || proof.path("defaultRowsHash").asText().equals(proof.path("tenantBRowsHash").asText())
                || !proof.path("defaultRowsHash").asText().equals(proof.path("forgedHeaderRowsHash").asText())
                || !proof.path("sqlIsolationConfirmed").isBoolean() || !proof.path("sqlIsolationConfirmed").booleanValue()) {
            throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
        }
        try {
            String firstId = proof.path("tenantARowId").asText(), secondId = proof.path("tenantBRowId").asText();
            if (!proof.path("tenantARowId").isTextual() || !proof.path("tenantBRowId").isTextual()
                    || !firstId.matches("[1-9][0-9]{0,18}") || !secondId.matches("[1-9][0-9]{0,18}")
                    || Long.parseLong(firstId) <= 0 || Long.parseLong(secondId) <= 0 || firstId.equals(secondId)) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            java.time.Instant observed = java.time.Instant.parse(proof.path("observedAt").asText());
            if (!proof.path("observedAt").asText().endsWith("Z") || observed.isAfter(now) || observed.isBefore(now.minus(maximumAge))) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        } catch (java.time.format.DateTimeParseException | NumberFormatException invalid) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED", invalid); }
    }

    @lombok.Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor @lombok.experimental.Accessors(chain = true) @lombok.Builder
    public static class FormalReferenceDTO {
        @jakarta.validation.constraints.NotBlank private String realmCode;
        @jakarta.validation.constraints.NotBlank private String resourceKey;
        @jakarta.validation.constraints.NotBlank private String tenantId;
        @com.fasterxml.jackson.annotation.JsonIgnore @lombok.ToString.Exclude @jakarta.validation.constraints.NotBlank private String subject;
        @jakarta.validation.constraints.NotBlank private String clientId;
        @jakarta.validation.constraints.NotBlank private String clientPk;
        @jakarta.validation.constraints.NotNull private URI issuer;
        @jakarta.validation.constraints.NotNull private URI resourceUri;
        @jakarta.validation.constraints.NotNull private URI readEndpoint;
        @jakarta.validation.constraints.NotNull private Path tokenFile;
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp = "[0-9a-f]{64}") private String tokenSha256;
        @jakarta.validation.constraints.NotNull private java.time.Instant expiresAt;
        @jakarta.validation.constraints.NotNull private java.util.Set<String> scopes;
    }

    @lombok.extern.slf4j.Slf4j
    @lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public static final class IsolatedResourceProbe {
        private final Path root;
        private final JsonNode manifest;
        private final JsonNode registry;
        private final JsonNode index;
        private final String digest;
        private final top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent verifier;
        private final Duration timeout;
        private final HttpClient http;

        public String resourceForAlias(String alias) { return registryResourceForAlias(registry, alias); }

        public String resourceForLlm() {
            var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                    .filter(value -> "https://127.0.0.1:28190".equals(value.path("resourceUri").asText()) && "ACTIVE".equals(value.path("status").asText())).toList();
            if (resources.size() != 1 || resources.getFirst().path("resourceServerId").asText().isBlank()) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            String id = resources.getFirst().path("resourceServerId").asText();
            var bindings = java.util.stream.StreamSupport.stream(registry.path("cliResources").spliterator(), false)
                    .filter(value -> id.equals(value.path("resourceServerId").asText())).toList();
            if (bindings.size() != 1 || !bindings.getFirst().path("resourceKey").asText().matches("[a-z][a-z0-9-]{2,63}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            return bindings.getFirst().path("resourceKey").asText();
        }

        public FormalReferenceDTO identityOnly(String realm, String resourceKey, Path metadataFile) throws Exception {
            var canonical = formal(realm, resourceKey, "admin");
            var metadata = strictJson().readTree(protectedBytes(metadataFile, root.resolve("secrets"), 128 * 1024));
            var reference = identityOnlyReference(metadata, canonical, manifest.path("runId").asText(), digest, metadataFile, java.time.Instant.now());
            try (var validators = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
                new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(reference);
            }
            tokenValue(reference); verifiedClaims(reference); return reference;
        }

        public String resourceForProcess(String processId) throws Exception {
            String appCode = nativeApplicationCode(processId);
            var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                    .filter(value -> appCode.equals(value.path("rbacApplicationCode").asText()) && "ACTIVE".equals(value.path("status").asText())).toList();
            if (resources.size() != 1) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            String id = resources.getFirst().path("resourceServerId").asText();
            var candidates = java.util.stream.StreamSupport.stream(registry.path("cliResources").spliterator(), false)
                    .filter(value -> id.equals(value.path("resourceServerId").asText())).toList();
            if (candidates.size() != 1) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            return candidates.getFirst().path("resourceKey").asText();
        }

        public String nativeApplicationCode(String processId) throws Exception {
            if (!java.util.Set.of("native-light", "native-web", "native-service", "native-agent").contains(processId)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            Path recipeFile = root.resolve("policies/native-authorization.json");
            byte[] recipeBytes = protectedBytes(recipeFile, root.resolve("policies"), 128 * 1024);
            requireBoundFile("policyFiles", recipeFile, recipeBytes);
            var recipe = strictJson().readTree(recipeBytes);
            var rows = java.util.stream.StreamSupport.stream(recipe.path("applications").spliterator(), false)
                    .filter(value -> processId.equals(value.path("processId").asText())).toList();
            if (rows.size() != 1) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            Path catalogFile = Path.of(rows.getFirst().path("catalogFile").asText());
            byte[] catalogBytes = protectedBytes(catalogFile, root, 2 * 1024 * 1024);
            requireBoundFile("catalogFiles", catalogFile, catalogBytes);
            String appCode = strictJson().readTree(catalogBytes).path("applicationCode").asText();
            if (!appCode.matches("[a-z][a-z0-9-]{2,127}")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            return appCode;
        }

        private void requireBoundFile(String field, Path file, byte[] value) {
            var references = java.util.stream.StreamSupport.stream(manifest.path(field).spliterator(), false)
                    .filter(reference -> file.toString().equals(reference.path("path").asText())).toList();
            if (references.size() != 1 || !sha256(value).equals(references.getFirst().path("sha256").asText())) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
        }

        public FormalReferenceDTO formal(String realm, String resourceKey, String user) throws Exception {
            if (!java.util.Set.of("default", "tenant-b").contains(realm) || !java.util.Set.of("admin", "viewer").contains(user)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var selected = java.util.stream.StreamSupport.stream(index.path("tokens").spliterator(), false)
                    .filter(value -> realm.equals(value.path("tenantCode").asText()) && resourceKey.equals(value.path("resourceKey").asText())
                            && user.equals(value.path("userKey").asText())).toList();
            if (selected.size() != 1) { throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE"); }
            var token = selected.getFirst();
            var bindings = java.util.stream.StreamSupport.stream(registry.path("cliResources").spliterator(), false)
                    .filter(value -> resourceKey.equals(value.path("resourceKey").asText())).toList();
            if (bindings.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var binding = bindings.getFirst();
            String id = binding.path("clientIds").path(realm).asText();
            var clients = java.util.stream.StreamSupport.stream(registry.path("clients").spliterator(), false)
                    .filter(value -> realm.equals(value.path("realmCode").asText()) && id.equals(value.path("clientId").asText())).toList();
            if (clients.size() != 1) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var client = clients.getFirst();
            String tenant = registry.path("tenantIds").path(realm).asText();
            String issuer = manifest.path("publicOrigin").asText() + "/tenants/" + tenant;
            if (!"PUBLIC".equals(client.path("clientType").asText()) || !"USER".equals(client.path("purpose").asText())
                    || !"ACTIVE".equals(client.path("status").asText()) || !issuer.equals(client.path("issuer").asText())
                    || !tenant.equals(token.path("tenantId").asText()) || !issuer.equals(token.path("issuer").asText())
                    || !id.equals(token.path("clientId").asText()) || !client.path("clientPk").asText().equals(token.path("clientPk").asText())
                    || !client.path("resourceUris").isArray() || client.path("resourceUris").size() != 1
                    || !client.path("resourceUris").get(0).asText().equals(token.path("resourceUri").asText())
                    || !client.path("refreshTokenTtlSeconds").isIntegralNumber() || !client.path("refreshTokenTtlSeconds").canConvertToInt()
                    || client.path("refreshTokenTtlSeconds").intValue() != 0) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            String path = binding.path("readPath").asText();
            boolean llmModels = "https://127.0.0.1:28190".equals(token.path("resourceUri").asText()) && "/v1/models".equals(path);
            if (!path.startsWith("/api/") && !llmModels || path.contains("..") || path.contains("\\") || path.contains("#")) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var scopes = scopeSet(token.path("scopes"));
            if (!scopes.equals(scopeSet(client.path("allowedScopes"))) || scopes.contains("offline_access")) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var resources = java.util.stream.StreamSupport.stream(registry.path("resources").spliterator(), false)
                    .filter(value -> binding.path("resourceServerId").asText().equals(value.path("resourceServerId").asText())
                            && "ACTIVE".equals(value.path("status").asText()) && token.path("resourceUri").asText().equals(value.path("resourceUri").asText())).toList();
            if (resources.size() != 1 || !client.path("clientPk").asText().matches("[1-9][0-9]{0,18}")
                    || new java.math.BigInteger(client.path("clientPk").asText()).compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var reference = FormalReferenceDTO.builder().realmCode(realm).resourceKey(resourceKey).tenantId(tenant).subject(registry.path("subjects").path(user).asText())
                    .clientId(id).clientPk(client.path("clientPk").asText()).issuer(URI.create(issuer)).resourceUri(URI.create(token.path("resourceUri").asText()))
                    .readEndpoint(URI.create(manifest.path("publicOrigin").asText() + path)).tokenFile(Path.of(token.path("tokenFile").asText()))
                    .tokenSha256(token.path("tokenSha256").asText()).expiresAt(java.time.Instant.parse(token.path("expiresAt").asText())).scopes(java.util.Set.copyOf(scopes)).build();
            try (var validators = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
                new top.egon.cola.component.common.core.validation.ValidationUtils(validators.getValidator()).validate(reference);
            }
            tokenValue(reference);
            return reference;
        }

        private java.util.Set<String> scopeSet(JsonNode values) {
            if (!values.isArray() || values.isEmpty() || values.size() > 64) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            var result = new java.util.HashSet<String>();
            for (var value : values) {
                if (!value.isTextual() || !value.textValue().matches("[A-Za-z0-9._:-]{1,128}") || !result.add(value.textValue())) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            }
            return java.util.Set.copyOf(result);
        }

        private String tokenValue(FormalReferenceDTO reference) throws IOException {
            if (!reference.getExpiresAt().isAfter(java.time.Instant.now())) { throw new IllegalStateException("XINGYUAN_INPUT_EXPIRED"); }
            byte[] bytes = protectedBytes(reference.getTokenFile(), root.resolve("secrets"), 128 * 1024);
            try {
                if (!sha256(bytes).equals(reference.getTokenSha256())) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
                return new String(bytes, StandardCharsets.UTF_8);
            } finally { java.util.Arrays.fill(bytes, (byte) 0); }
        }

        private org.springframework.security.oauth2.jwt.Jwt verifiedAccessClaims(FormalReferenceDTO reference) throws Exception {
            var verified = verifier.authenticate(tokenValue(reference));
            var jwt = verified.getJwt();
            if (!"at+jwt".equals(String.valueOf(jwt.getHeaders().get("typ")))
                    || !reference.getExpiresAt().equals(jwt.getExpiresAt())
                    || !reference.getScopes().equals(java.util.Set.of(jwt.getClaimAsString("scope").split(" ")))
                    || !reference.getIssuer().toASCIIString().equals(jwt.getIssuer().toString()) || !reference.getSubject().equals(jwt.getSubject())
                    || !java.util.List.of(reference.getResourceUri().toASCIIString()).equals(jwt.getAudience())
                    || !reference.getTenantId().equals(jwt.getClaimAsString("tenant_id")) || !reference.getClientId().equals(jwt.getClaimAsString("client_id"))
                    || !reference.getClientPk().equals(jwt.getClaimAsString("client_registration_id")) || !"USER".equals(jwt.getClaimAsString("principal_type"))
                    || !"TENANT".equals(jwt.getClaimAsString("scope_context"))) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            for (String forbidden : java.util.List.of("tid", "sid", "token_version", "roles", "capabilities")) {
                if (jwt.hasClaim(forbidden)) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            }
            return jwt;
        }

        public org.springframework.security.oauth2.jwt.Jwt verifiedClaims(FormalReferenceDTO reference) throws Exception {
            var jwt = verifiedAccessClaims(reference);
            var request = HttpRequest.newBuilder(URI.create(reference.getIssuer() + "/userinfo")).timeout(timeout)
                    .header("Authorization", "Bearer " + tokenValue(reference)).GET().build();
            var body = new ByteArrayOutputStream(); var response = http.send(request, boundedBody(body));
            if (response.statusCode() != 200 || !reference.getSubject().equals(strictJson().readTree(body.toByteArray()).path("sub").asText())) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
            return jwt;
        }

        public HttpAuditObservationVO observeNativeUserInfo(FormalReferenceDTO reference, boolean authenticated) throws Exception {
            verifiedAccessClaims(reference);
            String trace = java.util.UUID.randomUUID().toString().replace("-", ""); Instant started = Instant.now();
            var request = HttpRequest.newBuilder(URI.create(reference.getIssuer() + "/userinfo")).timeout(timeout).GET()
                    .header("traceparent", "00-" + trace + '-' + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01");
            if (authenticated) { request.header("Authorization", "Bearer " + tokenValue(reference)); }
            var body = new ByteArrayOutputStream(); var response = http.send(request.build(), boundedBody(body));
            if (response.statusCode() != (authenticated ? 200 : 401) || authenticated
                    && !reference.getSubject().equals(strictJson().readTree(body.toByteArray()).path("sub").asText())) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            return observedHttpCall(response, trace, started);
        }

        public int read(FormalReferenceDTO reference) throws Exception {
            return http.send(HttpRequest.newBuilder(reference.getReadEndpoint()).timeout(timeout).header("Authorization", "Bearer " + tokenValue(reference)).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode();
        }

        public int withoutToken(FormalReferenceDTO reference) throws Exception {
            return http.send(HttpRequest.newBuilder(reference.getReadEndpoint()).timeout(timeout).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode();
        }

        public boolean wrongTenantHeaderDoesNotSelectAnotherContext(FormalReferenceDTO source, FormalReferenceDTO other) throws Exception {
            var response = http.send(HttpRequest.newBuilder(source.getReadEndpoint()).timeout(timeout)
                    .header("Authorization", "Bearer " + tokenValue(source)).header("X-Tenant-Id", other.getTenantId()).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        }

        public void requireNativeTenantIsolationProof(FormalReferenceDTO first, FormalReferenceDTO second) throws Exception {
            Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
            var proof = strictJson().readTree(protectedBytes(evidence.resolve("native-tenant-isolation.json"), root.resolve("evidence"), 128 * 1024));
            validateNativeTenantIsolationProof(proof, manifest.path("runId").asText(), digest, first, second,
                    java.time.Instant.now(), Duration.ofSeconds(manifest.path("deadlines").path("operationSeconds").intValue()));
        }

        public void bindNativeTenantReadEndpoints(FormalReferenceDTO first, FormalReferenceDTO second) throws Exception {
            String resource = resourceForProcess("native-light");
            if (!resource.equals(first.getResourceKey()) || !resource.equals(second.getResourceKey())
                    || !"default".equals(first.getRealmCode()) || !"tenant-b".equals(second.getRealmCode())) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
            var proof = strictJson().readTree(protectedBytes(evidence.resolve("native-tenant-isolation.json"), root.resolve("evidence"), 128 * 1024));
            validateNativeTenantIsolationProof(proof, manifest.path("runId").asText(), digest, first, second,
                    java.time.Instant.now(), Duration.ofSeconds(manifest.path("deadlines").path("operationSeconds").intValue()));
            // A by-ID API has different real rows per tenant; one static registry path cannot select both.
            first.setReadEndpoint(URI.create("https://127.0.0.1:28310/api/users/" + proof.path("tenantARowId").asText()));
            second.setReadEndpoint(URI.create("https://127.0.0.1:28310/api/users/" + proof.path("tenantBRowId").asText()));
            try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
                var validation = new top.egon.cola.component.common.core.validation.ValidationUtils(factory.getValidator());
                validation.validate(first); validation.validate(second);
            }
        }

        public void publishNativeTenantIsolationProof(Path evidence, JsonNode proof) throws Exception {
            if (!evidence.equals(root.resolve("evidence")) && (!root.resolve("evidence").equals(evidence.getParent())
                    || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            String resource = resourceForProcess("native-light");
            validateNativeTenantIsolationProof(proof, manifest.path("runId").asText(), digest,
                    formal("default", resource, "admin"), formal("tenant-b", resource, "admin"), java.time.Instant.now(),
                    Duration.ofSeconds(manifest.path("deadlines").path("operationSeconds").intValue()));
            publishPrivateEvidence(evidence.resolve("native-tenant-isolation.json"), proof);
        }

        public void publishNativeAgentKnowledgeProof(Path evidence, JsonNode proof) throws Exception {
            if (!evidence.equals(root.resolve("evidence")) && (!root.resolve("evidence").equals(evidence.getParent())
                    || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var fields = new java.util.HashSet<String>(); proof.fieldNames().forEachRemaining(fields::add);
            if (!fields.equals(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "status", "scope", "resourceUri", "tokenAReference", "tokenBReference",
                    "knowledgeBaseAId", "knowledgeBaseBId", "baseAReference", "baseBReference", "pageAReference", "pageBReference", "viewerDeniedStatus", "deniedRecords",
                    "foreignTenantCode", "bareStatus", "recordedAt")) || !proof.path("schemaVersion").isInt() || proof.path("schemaVersion").intValue() != 1
                    || !manifest.path("runId").asText().equals(proof.path("runId").asText()) || !digest.equals(proof.path("manifestSha256").asText())
                    || !"PASSED".equals(proof.path("status").asText()) || !"AGENT_KNOWLEDGE_METADATA_ONLY".equals(proof.path("scope").asText())
                    || !proof.path("viewerDeniedStatus").isInt() || proof.path("viewerDeniedStatus").intValue() != 403
                    || !proof.path("deniedRecords").isInt() || proof.path("deniedRecords").intValue() != 0
                    || !proof.path("bareStatus").isInt() || proof.path("bareStatus").intValue() != 401
                    || !"KNOWLEDGE_BASE_NOT_FOUND".equals(proof.path("foreignTenantCode").asText())) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            String resource = resourceForProcess("native-agent");
            var first = formal("default", resource, "admin"); var second = formal("tenant-b", resource, "admin");
            if (!first.getResourceUri().toASCIIString().equals(proof.path("resourceUri").asText())
                    || !first.getTokenSha256().equals(proof.path("tokenAReference").asText()) || !second.getTokenSha256().equals(proof.path("tokenBReference").asText())
                    || proof.path("knowledgeBaseAId").asText().equals(proof.path("knowledgeBaseBId").asText())) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            for (String field : java.util.List.of("knowledgeBaseAId", "knowledgeBaseBId")) {
                if (!proof.path(field).isTextual() || !proof.path(field).textValue().matches("[1-9][0-9]{0,18}")) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
                try { if (Long.parseLong(proof.path(field).textValue()) <= 0) { throw new NumberFormatException(); } }
                catch (NumberFormatException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            }
            for (String field : java.util.List.of("tokenAReference", "tokenBReference", "baseAReference", "baseBReference", "pageAReference", "pageBReference")) {
                if (!proof.path(field).asText().matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            }
            java.time.Instant recorded;
            try { recorded = java.time.Instant.parse(proof.path("recordedAt").asText()); }
            catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            var now = java.time.Instant.now();
            if (!proof.path("recordedAt").asText().endsWith("Z") || recorded.isAfter(now)
                    || recorded.isBefore(now.minusSeconds(manifest.path("deadlines").path("operationSeconds").intValue()))) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            publishPrivateEvidence(evidence.resolve("native-agent-knowledge.json"), proof);
        }

        public void publishNativeGraphQlProof(Path evidence, JsonNode proof) throws Exception {
            if (!evidence.equals(root.resolve("evidence")) && (!root.resolve("evidence").equals(evidence.getParent())
                    || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var fields = new java.util.HashSet<String>(); proof.fieldNames().forEachRemaining(fields::add);
            if (!fields.equals(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "status", "scope", "resourceUri", "tokenAReference", "tokenBReference",
                    "queryReference", "tenantAResponseReference", "tenantBResponseReference", "forgedHeaderResponseReference", "foreignTenantCode", "bareStatus", "recordedAt",
                    "identityOnlyTokenReference", "scopeDeniedCode", "beforeScopeDenied", "afterScopeDenied", "afterBare", "afterRecovery"))
                    || !proof.path("schemaVersion").isInt() || proof.path("schemaVersion").intValue() != 1
                    || !manifest.path("runId").asText().equals(proof.path("runId").asText()) || !digest.equals(proof.path("manifestSha256").asText())
                    || !"PASSED".equals(proof.path("status").asText()) || !"LIGHT_GRAPHQL_TWO_TENANT_READ".equals(proof.path("scope").asText())
                    || !"USER_NOT_FOUND".equals(proof.path("foreignTenantCode").asText()) || !"AUTHORIZATION_DENIED".equals(proof.path("scopeDeniedCode").asText())
                    || !proof.path("bareStatus").isInt() || proof.path("bareStatus").intValue() != 401) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            String resource = resourceForProcess("native-light");
            var first = formal("default", resource, "admin"); var second = formal("tenant-b", resource, "admin");
            if (!first.getResourceUri().toASCIIString().equals(proof.path("resourceUri").asText())
                    || !first.getTokenSha256().equals(proof.path("tokenAReference").asText()) || !second.getTokenSha256().equals(proof.path("tokenBReference").asText())) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            if (proof.path("afterBare").path("counts").size() != 2 || !proof.path("afterBare").path("counts").has("create") || !proof.path("afterBare").path("counts").has("get")
                    || !proof.path("beforeScopeDenied").equals(proof.path("afterScopeDenied")) || !proof.path("afterScopeDenied").equals(proof.path("afterBare"))) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            requireOnlyBusinessMethodDelta(proof.path("afterBare"), proof.path("afterRecovery"), "get", 2);
            for (String name : java.util.List.of("tokenAReference", "tokenBReference", "queryReference", "tenantAResponseReference", "tenantBResponseReference", "forgedHeaderResponseReference", "identityOnlyTokenReference")) {
                if (!proof.path(name).asText().matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            }
            java.time.Instant recorded;
            try { recorded = java.time.Instant.parse(proof.path("recordedAt").asText()); }
            catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            var now = java.time.Instant.now();
            if (!proof.path("recordedAt").asText().endsWith("Z") || recorded.isAfter(now)
                    || recorded.isBefore(now.minusSeconds(manifest.path("deadlines").path("operationSeconds").intValue()))) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            requireNativeTenantIsolationProof(first, second);
            publishPrivateEvidence(evidence.resolve("native-light-graphql.json"), proof);
        }

        public void publishNativeRpcProtocolProof(Path evidence, JsonNode proof) throws Exception {
            if (!evidence.equals(root.resolve("evidence")) && (!root.resolve("evidence").equals(evidence.getParent())
                    || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var fields = new java.util.HashSet<String>(); proof.fieldNames().forEachRemaining(fields::add);
            boolean userData = "STANDALONE_SERVICE_TO_WEB_DATA".equals(proof.path("scope").asText());
            var expectedFields = new java.util.HashSet<>(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "status", "scope", "recordedAt", "proofs"));
            if (userData) { expectedFields.add("crossTenantIsolationConfirmed"); }
            if (!fields.equals(expectedFields)
                    || !proof.path("schemaVersion").isInt() || proof.path("schemaVersion").intValue() != 1
                    || !manifest.path("runId").asText().equals(proof.path("runId").asText()) || !digest.equals(proof.path("manifestSha256").asText())
                    || !"PASSED".equals(proof.path("status").asText()) || !userData && !"STANDALONE_NOT_FOUND_PROTOCOL".equals(proof.path("scope").asText())
                    || userData && (!proof.path("crossTenantIsolationConfirmed").isBoolean() || !proof.path("crossTenantIsolationConfirmed").booleanValue())
                    || !proof.path("proofs").isArray() || proof.path("proofs").size() != (userData ? 2 : 4)
                    || !proof.path("recordedAt").isTextual() || !proof.path("recordedAt").textValue().endsWith("Z")) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            var tuples = new java.util.HashSet<String>();
            var requestTraces = new java.util.HashSet<String>();
            java.time.Instant recorded;
            try { recorded = java.time.Instant.parse(proof.path("recordedAt").textValue()); }
            catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            var now = java.time.Instant.now();
            if (recorded.isAfter(now) || recorded.isBefore(now.minusSeconds(manifest.path("deadlines").path("operationSeconds").intValue()))) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            for (var item : proof.path("proofs")) {
                var itemFields = new java.util.HashSet<String>(); item.fieldNames().forEachRemaining(itemFields::add);
                String caller = item.path("consumerRole").asText(), peer = item.path("providerRole").asText(), tenant = item.path("tenantId").asText();
                var expectedItemFields = new java.util.HashSet<>(java.util.Set.of("consumerRole", "providerRole", "tenantId", "methodReference", "descriptorReference", "leaseReference",
                        "leaseRevision", "leaseExpiresAt", "businessStatus", "missingTokenStatus", "responseReference",
                        "beforeBusiness", "successfulBusiness", "beforeMissingToken", "afterMissingToken", "successfulRequest", "missingTokenRequest"));
                if (userData) { expectedItemFields.add("httpReference"); }
                else { expectedItemFields.add("serviceSecurity"); }
                if (!itemFields.equals(expectedItemFields)
                        || !java.util.Set.of("native-web", "native-service").contains(caller) || caller.equals(peer)
                        || !java.util.Set.of("native-web", "native-service").contains(peer)
                        || userData && (!"native-service".equals(caller) || !"native-web".equals(peer) || !item.path("httpReference").asText().matches("[0-9a-f]{64}"))
                        || !registry.path("tenantIds").path("default").asText().equals(tenant) && !registry.path("tenantIds").path("tenant-b").asText().equals(tenant)
                        || !tuples.add(caller + ":" + peer + ":" + tenant) || !"UNAUTHENTICATED".equals(item.path("missingTokenStatus").asText())
                        || !item.path("leaseRevision").isIntegralNumber() || !item.path("leaseRevision").canConvertToLong() || item.path("leaseRevision").longValue() < 1
                        || !item.path("leaseExpiresAt").isTextual() || !item.path("leaseExpiresAt").textValue().endsWith("Z")
                        || !(userData ? "USER_DATA_MATCHED" : peer.equals("native-service") ? "COURSE_NOT_FOUND" : "ORG_NOT_FOUND").equals(item.path("businessStatus").asText())) {
                    throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
                }
                validateRpcProviderCounterProofs(item, peer, userData && registry.path("tenantIds").path("tenant-b").asText().equals(tenant) ? 1 : 0);
                var maximumAge = Duration.ofSeconds(manifest.path("deadlines").path("operationSeconds").intValue());
                if (!requestTraces.add(validateRpcAuditObservation(item.path("successfulRequest"), "OK", recorded, maximumAge))
                        || !requestTraces.add(validateRpcAuditObservation(item.path("missingTokenRequest"), "UNAUTHENTICATED", recorded, maximumAge))) {
                    throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
                }
                if (!userData) {
                    var security = item.path("serviceSecurity"); validateRpcServiceSecurity(security, peer, now, maximumAge);
                    if (!requestTraces.add(validateRpcAuditObservation(security.path("scopeDeniedRequest"), "PERMISSION_DENIED", recorded, maximumAge))
                            || !requestTraces.add(validateRpcAuditObservation(security.path("revokedRequest"), "UNAUTHENTICATED", recorded, maximumAge))) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
                }
                for (String reference : java.util.List.of("methodReference", "descriptorReference", "leaseReference", "responseReference")) {
                    if (!item.path(reference).asText().matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
                }
            }
            publishPrivateEvidence(evidence.resolve(userData ? "native-rpc-user-read.json" : "native-rpc-contract-probe.json"), proof);
        }

        public void publishNativeServiceCacheReady(Path evidence, JsonNode proof) throws Exception {
            if (!evidence.equals(root.resolve("evidence")) && (!root.resolve("evidence").equals(evidence.getParent())
                    || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var fields = new java.util.HashSet<String>(); proof.fieldNames().forEachRemaining(fields::add);
            var activeRoles = activeNativeRuntimeRoles(root, manifest, digest);
            if (!fields.equals(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "status", "phase", "recordedAt", "requestCount", "profileCount"))
                    || !proof.path("schemaVersion").isInt() || proof.path("schemaVersion").intValue() != 1
                    || !manifest.path("runId").asText().equals(proof.path("runId").asText()) || !digest.equals(proof.path("manifestSha256").asText())
                    || !"READY".equals(proof.path("status").asText()) || !"INITIAL_VERIFIED".equals(proof.path("phase").asText())
                    || !proof.path("requestCount").isInt() || proof.path("requestCount").intValue() < 1 || proof.path("requestCount").intValue() > 192
                    || !proof.path("profileCount").isInt() || proof.path("profileCount").intValue() != activeRoles.size()
                    || !proof.path("recordedAt").isTextual() || !proof.path("recordedAt").textValue().endsWith("Z")) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            java.time.Instant recorded;
            try { recorded = java.time.Instant.parse(proof.path("recordedAt").textValue()); }
            catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            var now = java.time.Instant.now();
            if (recorded.isAfter(now) || recorded.isBefore(now.minusSeconds(manifest.path("deadlines").path("operationSeconds").intValue()))) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            publishPrivateEvidence(evidence.resolve("native-service-cache-ready.json"), proof);
        }

        public void publishNativeServiceCacheProof(Path evidence, JsonNode proof) throws Exception {
            if (!evidence.equals(root.resolve("evidence")) && (!root.resolve("evidence").equals(evidence.getParent())
                    || !evidence.getFileName().toString().matches("verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            var envelopeFields = new java.util.HashSet<String>(); proof.fieldNames().forEachRemaining(envelopeFields::add);
            var activeRoles = java.util.Set.copyOf(activeNativeRuntimeRoles(root, manifest, digest));
            var expectedFields = new java.util.HashSet<>(java.util.Set.of("schemaVersion", "runId", "manifestSha256", "status", "clock", "completedAt", "proofs"));
            if (!activeRoles.contains("native-agent")) {
                expectedFields.add("excludedRuntimeRoles");
                if (!proof.path("excludedRuntimeRoles").isArray() || proof.path("excludedRuntimeRoles").size() != 1
                        || !"native-agent".equals(proof.path("excludedRuntimeRoles").get(0).asText())) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            }
            if (!envelopeFields.equals(expectedFields)
                    || !proof.path("schemaVersion").isInt() || proof.path("schemaVersion").intValue() != 1
                    || !manifest.path("runId").asText().equals(proof.path("runId").asText()) || !digest.equals(proof.path("manifestSha256").asText())
                    || !"PASSED".equals(proof.path("status").asText()) || !"SYSTEM_UTC".equals(proof.path("clock").asText())
                    || !proof.path("proofs").isArray() || proof.path("proofs").isEmpty() || proof.path("proofs").size() > 192) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            java.time.Instant completed;
            try { completed = java.time.Instant.parse(proof.path("completedAt").asText()); }
            catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            java.time.Instant now = java.time.Instant.now();
            if (!proof.path("completedAt").asText().endsWith("Z") || completed.isAfter(now)
                    || completed.isBefore(now.minusSeconds(manifest.path("deadlines").path("operationSeconds").intValue()))) {
                throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
            }
            var roles = new java.util.HashSet<String>();
            var keys = new java.util.HashSet<String>();
            for (var item : proof.path("proofs")) {
                var fields = new java.util.HashSet<String>(); item.fieldNames().forEachRemaining(fields::add);
                if (!fields.equals(java.util.Set.of("role", "authorizationKeyReference", "initialTokenReference", "renewedTokenReference",
                        "initialTokenResponses", "totalTokenResponses", "concurrentCallers", "renewalCacheMisses", "refreshTokenAbsent", "initialExpiresAt", "renewedAt"))
                        || !java.util.Set.of("native-light", "native-web", "native-service", "native-agent").contains(item.path("role").asText())
                        || !item.path("authorizationKeyReference").asText().matches("[0-9a-f]{64}") || !keys.add(item.path("authorizationKeyReference").asText())
                        || !item.path("initialTokenReference").asText().matches("[0-9a-f]{64}") || !item.path("renewedTokenReference").asText().matches("[0-9a-f]{64}")
                        || item.path("initialTokenReference").asText().equals(item.path("renewedTokenReference").asText())
                        || !item.path("initialTokenResponses").isInt() || item.path("initialTokenResponses").intValue() != 1
                        || !item.path("totalTokenResponses").isInt() || item.path("totalTokenResponses").intValue() != 2
                        || !item.path("concurrentCallers").isInt() || item.path("concurrentCallers").intValue() != 8
                        || !item.path("renewalCacheMisses").isInt() || item.path("renewalCacheMisses").intValue() != 8
                        || !item.path("refreshTokenAbsent").isBoolean() || !item.path("refreshTokenAbsent").booleanValue()) {
                    throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED");
                }
                for (String field : java.util.List.of("initialExpiresAt", "renewedAt")) {
                    if (!item.path(field).isTextual() || !item.path(field).textValue().endsWith("Z")) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
                    try { java.time.Instant.parse(item.path(field).textValue()); }
                    catch (java.time.format.DateTimeParseException invalid) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
                }
                roles.add(item.path("role").asText());
            }
            if (!roles.equals(activeRoles)) { throw new IllegalArgumentException("XINGYUAN_VERIFICATION_FAILED"); }
            publishPrivateEvidence(evidence.resolve("native-service-cache.json"), proof);
        }

        public void awaitFormalRevocation(FormalReferenceDTO reference, org.springframework.security.oauth2.jwt.Jwt jwt) throws Exception {
            Path evidence = Path.of(System.getProperty("xingyuan.isolated.evidence-directory", root.resolve("evidence").toString()));
            Path file = evidence.resolve("formal-revocation-" + reference.getTokenSha256() + ".json");
            if (!evidence.equals(root.resolve("evidence")) && (!root.resolve("evidence").equals(evidence.getParent())
                    || !evidence.getFileName().toString().matches("verify-[0-9a-f-]{36}"))) { throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE"); }
            java.time.Instant requestedAt = java.time.Instant.now();
            if (Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { throw new IllegalStateException("XINGYUAN_STATE_CONFLICT"); }
            var request = strictJson().createObjectNode().put("schemaVersion", 1).put("runId", manifest.path("runId").asText())
                    .put("manifestSha256", digest).put("status", "READY").put("baselineStatus", 200)
                    .put("tokenSha256", reference.getTokenSha256()).put("issuer", reference.getIssuer().toASCIIString())
                    .put("clientPk", reference.getClientPk()).put("tenantId", reference.getTenantId()).put("resourceUri", reference.getResourceUri().toASCIIString())
                    .put("authorizationReference", sha256(verifiedAuthorizationId(jwt).getBytes(StandardCharsets.UTF_8)))
                    .put("recordedAt", requestedAt.toString());
            publishPrivateEvidence(evidence.resolve("formal-revocation-request-" + reference.getTokenSha256() + ".json"), request);
            int operationSeconds = manifest.path("deadlines").path("operationSeconds").intValue();
            var bridge = new ProcessBuilder("python3", "-I", Path.of(manifest.path("repoRoot").asText())
                    .resolve("scripts/unified-xingyuan/lib/isolated_integration.py").toString(), "formal-revoke",
                    System.getProperty("xingyuan.isolated.manifest-file", System.getenv("XINGYUAN_INTEGRATION_MANIFEST")),
                    file.toString(), reference.getTokenSha256(), manifest.path("repoRoot").asText())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try {
                if (!bridge.waitFor(operationSeconds + 15L, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
                }
                if (bridge.exitValue() != 0) {
                    throw new IllegalStateException(bridge.exitValue() == 9 ? "XINGYUAN_COMMIT_UNKNOWN" : "XINGYUAN_VERIFICATION_FAILED");
                }
            } finally {
                if (bridge.isAlive()) {
                    bridge.destroy();
                    bridge.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
                }
            }
            var receipt = strictJson().readTree(protectedBytes(file, root.resolve("evidence"), 128 * 1024));
            if (!receipt.path("schemaVersion").isIntegralNumber() || receipt.path("schemaVersion").intValue() != 1
                    || !manifest.path("runId").asText().equals(receipt.path("runId").asText()) || !digest.equals(receipt.path("manifestSha256").asText())
                    || !"CONFIRMED".equals(receipt.path("status").asText()) || !receipt.path("denyApplied").isBoolean() || !receipt.path("denyApplied").booleanValue()
                    || !reference.getResourceUri().toASCIIString().equals(receipt.path("resourceUri").asText())
                    || !reference.getTenantId().equals(receipt.path("tenantId").asText())
                    || !receipt.path("recordedAt").isTextual() || !receipt.path("recordedAt").asText().endsWith("Z")
                    || java.time.Instant.parse(receipt.path("recordedAt").asText()).isBefore(requestedAt)
                    || !reference.getTokenSha256().equals(receipt.path("tokenSha256").asText())
                    || !reference.getClientPk().equals(receipt.path("clientPk").asText()) || !reference.getIssuer().toASCIIString().equals(receipt.path("issuer").asText())
                    || !sha256(verifiedAuthorizationId(jwt).getBytes(StandardCharsets.UTF_8)).equals(receipt.path("authorizationReference").asText())) {
                throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED");
            }
        }

        private void publishPrivateEvidence(Path file, JsonNode value) throws IOException {
            Path parent = file.getParent();
            var owner = parent.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
            for (Path directory = parent; directory != null && directory.startsWith(root); directory = directory.getParent()) {
                if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory) || !Files.getOwner(directory).equals(owner)
                        || !Files.getPosixFilePermissions(directory).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))) {
                    throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
                }
            }
            Path temporary = Files.createTempFile(parent, ".formal-revocation-", ".tmp", java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
            try {
                try (var output = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) {
                    var bytes = java.nio.ByteBuffer.wrap(strictJson().writeValueAsBytes(value));
                    while (bytes.hasRemaining()) { output.write(bytes); }
                    output.force(true);
                }
                Files.createLink(file, temporary);
                try (var directory = java.nio.channels.FileChannel.open(parent, java.nio.file.StandardOpenOption.READ)) { directory.force(true); }
            } finally { Files.deleteIfExists(temporary); }
        }

        private String verifiedAuthorizationId(org.springframework.security.oauth2.jwt.Jwt jwt) {
            String id = jwt.getClaimAsString("authorization_id");
            if (id == null || !id.matches("[1-9][0-9]{0,18}")) { throw new IllegalStateException("XINGYUAN_VERIFICATION_FAILED"); }
            return id;
        }
    }

}
