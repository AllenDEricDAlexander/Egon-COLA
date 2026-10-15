package top.egon.cola.platform.tianquan.shoubing.starter.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration;

class TokenUsageAuditComponentTest {

    private static final String STREAM = "egon:oauth:audit:v1";
    private static final String ISSUER = "https://idp.example/tenants/1001";
    private static final String TOKEN_REF = "a".repeat(64);

    @TempDir
    Path temporary;

    @Test
    void strictAtomicAndDirectoryFailuresNeverPublishAndKeepRecoverableFacts() throws Exception {
        for (boolean directoryFailure : java.util.List.of(false, true)) {
            RedissonClient redisson = mock(RedissonClient.class);
            @SuppressWarnings("unchecked") RStream<String,String> stream = mock(RStream.class);
            org.mockito.Mockito.doReturn(stream).when(redisson).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
            Path spool = temporary.resolve(directoryFailure ? "directory-failure" : "atomic-failure");
            TokenUsageAuditComponent component = org.mockito.Mockito.spy(component(redisson, spool, secureKeyFile(), 1_048_576));
            if (directoryFailure) {
                org.mockito.Mockito.doThrow(new java.io.IOException("directory force unsupported")).when(component).forceDirectory();
            } else {
                org.mockito.Mockito.doThrow(new java.nio.file.AtomicMoveNotSupportedException("fixture", "fixture", "unsupported"))
                        .when(component).atomicMove(any(Path.class), any(Path.class));
            }
            assertThatThrownBy(() -> component.appendStrict(event())).isInstanceOf(IllegalStateException.class)
                    .hasMessage("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
            org.mockito.Mockito.verifyNoInteractions(stream);
            assertThat(Files.exists(spool.resolve("event-1.evt"))).isEqualTo(directoryFailure);
        }
    }

    @Test
    void strictFileForceFailureCannotUseTheAvailableBroker() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked") RStream<String,String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        TokenUsageAuditComponent component = component(redisson, temporary.resolve("file-force"), secureKeyFile(), 1_048_576);
        java.nio.channels.FileChannel channel = mock(java.nio.channels.FileChannel.class);
        when(channel.write(any(java.nio.ByteBuffer.class))).thenAnswer(call -> {
            java.nio.ByteBuffer buffer = call.getArgument(0); int remaining = buffer.remaining(); buffer.position(buffer.limit()); return remaining;
        });
        org.mockito.Mockito.doThrow(new java.io.IOException("force failed")).when(channel).force(true);
        try (var files = org.mockito.Mockito.mockStatic(java.nio.channels.FileChannel.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> java.nio.channels.FileChannel.open(any(Path.class), any(java.nio.file.OpenOption[].class))).thenReturn(channel);
            assertThatThrownBy(() -> component.appendStrict(event())).isInstanceOf(IllegalStateException.class)
                    .hasMessage("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
            org.mockito.Mockito.verifyNoInteractions(stream);
        }
    }

    @Test
    void strictHandoffDoesNotFallBackToBrokerOnlyWhenDurableSpoolIsFull() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked") RStream<String,String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(9,1));
        TokenUsageAuditComponent component = component(redisson, temporary.resolve("strict-full"), secureKeyFile(), 1);
        assertThatThrownBy(() -> component.appendStrict(event())).isInstanceOf(IllegalStateException.class)
                .hasMessage("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
        org.mockito.Mockito.verifyNoInteractions(stream);
    }

    @Test
    void strictBrokerFailureKeepsTheSameDurableFactForRecovery() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked") RStream<String,String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenThrow(new RedisException("offline"));
        Path spool = temporary.resolve("strict-broker");
        TokenUsageAuditComponent component = component(redisson, spool, secureKeyFile(), 1_048_576);
        assertThatThrownBy(() -> component.appendStrict(event())).isInstanceOf(IllegalStateException.class);
        assertThat(Files.exists(spool.resolve("event-1.evt"))).isTrue();
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(10,1));
        assertThat(component.appendStrict(event())).isEqualTo(new StreamMessageId(10,1));
    }

    @Test
    void durablySpoolsBeforePublishingAndDeletesTheSegmentOnlyAfterAcknowledgement() throws Exception {
        Path spool = temporary.resolve("spool");
        Path keyFile = secureKeyFile();
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        StreamMessageId messageId = new StreamMessageId(5L, 1L);
        when(stream.add(any(StreamAddArgs.class))).thenReturn(messageId);
        TokenUsageAuditComponent component = component(redisson, spool, keyFile, 1_048_576L);

        assertThat(component.append(event())).isEqualTo(messageId);

        Path segment = spool.resolve("event-1.evt");
        assertThat(Files.exists(segment)).isTrue();
        assertThat(Files.getPosixFilePermissions(segment))
                .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        assertThat(new String(Files.readAllBytes(segment), java.nio.charset.StandardCharsets.ISO_8859_1))
                .doesNotContain("raw-token-value");
        confirmedReceipt(redisson, component, "STORED");
        component.acknowledge("event-1");
        assertThat(Files.exists(segment)).isFalse();
        assertThat(component.verifySignature(ISSUER, TOKEN_REF, signature(ISSUER, TOKEN_REF, Files.readAllBytes(keyFile))))
                .isTrue();
        assertThat(component.verifySignature(ISSUER, TOKEN_REF, "0".repeat(64))).isFalse();
    }

    @Test
    void replaysUnsentFramesAfterRedisFailureAndDoesNotRepublishMarkedFrames() throws Exception {
        Path spool = temporary.resolve("spool");
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class)))
                .thenThrow(new RedisException("redis unavailable"))
                .thenReturn(new StreamMessageId(6L, 1L));
        TokenUsageAuditComponent component = component(redisson, spool, secureKeyFile(), 1_048_576L);

        assertThatThrownBy(() -> component.append(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OAUTH_AUDIT_STREAM_UNAVAILABLE");
        assertThat(Files.exists(spool.resolve("event-1.evt"))).isTrue();
        org.mockito.Mockito.doReturn(mock(org.redisson.api.RBucket.class)).when(redisson)
                .getBucket(any(String.class), eq(StringCodec.INSTANCE));
        assertThat(component.replayPending()).isEqualTo(1);
        assertThat(component.replayPending()).isZero();
        confirmedReceipt(redisson, component, "STORED");
        component.acknowledge("event-1");
        try (var files = Files.list(spool)) {
            assertThat(files.toList()).isEmpty();
        }
    }

    @Test
    void fallsBackToDirectStreamWhenTheSpoolIsFullAndFailsClosedWhenBothDestinationsFail() throws Exception {
        Path spool = temporary.resolve("spool");
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class)))
                .thenReturn(new StreamMessageId(7L, 1L))
                .thenThrow(new RedisException("redis unavailable"));
        TokenUsageAuditComponent direct = component(redisson, spool, secureKeyFile(), 1L);

        assertThat(direct.append(event())).isEqualTo(new StreamMessageId(7L, 1L));

        TokenUsageAuditComponent unavailable = component(redisson, temporary.resolve("full"),
                secureKeyFile(), 1L);
        when(stream.add(any(StreamAddArgs.class))).thenThrow(new RedisException("redis unavailable"));
        assertThatThrownBy(() -> unavailable.append(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
    }

    @Test
    void starterRegistersTheDurableAuditComponentOnlyWhenItsRequiredPathsAreConfigured() throws Exception {
        Path configuredSpool = temporary.resolve("configured-spool");
        Path configuredKey = temporary.resolve("configured-audit.key");
        Files.write(configuredKey, "0123456789abcdef0123456789abcdef"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(configuredKey,
                EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        new ApplicationContextRunner()
                .withUserConfiguration(IdpStarterAutoConfiguration.OAuthAuditConfiguration.class)
                .withBean("idpAuditRedissonClient", RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new)
                .withBean("idpClock", java.time.Clock.class, java.time.Clock::systemUTC)
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> {
                    var validator = new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
                    validator.afterPropertiesSet(); return validator;
                })
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.audit.spool-directory=" + configuredSpool,
                        "egon.cola.platform.tianquan.shoubing.audit.hmac-key-file=" + configuredKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OAuthAuditProperties.class);
                    assertThat(context).hasSingleBean(TokenUsageAuditComponent.class);
        });
    }

    @Test
    void eventIdsFitTheAuditKeyAndRequireAnExactHttpsIssuer() {
        assertThat(event().toString()).doesNotContain(TOKEN_REF);
        assertThatThrownBy(() -> new TokenUsageAuditEvent("e".repeat(65), 1, URI.create(ISSUER),
                "https://api.example/resource", null, null, null, "SUCCESS", "ACCESS_GRANTED",
                Instant.parse("2026-10-08T12:00:00Z"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TOKEN_USAGE_AUDIT_EVENT_INVALID");
        assertThatThrownBy(() -> new TokenUsageAuditEvent("event-2", 1, null,
                "https://api.example/resource", null, null, null, "SUCCESS", "ACCESS_GRANTED",
                Instant.parse("2026-10-08T12:00:00Z"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TOKEN_USAGE_AUDIT_ISSUER_INVALID");
    }

    @Test
    void generatedStarterConstructorRetainsTheRequiredQualifiers() {
        var constructor = Arrays.stream(TokenUsageAuditComponent.class.getDeclaredConstructors())
                .filter(value -> value.getParameterCount() == 6).findFirst().orElseThrow();
        var qualifierValues = Arrays.stream(constructor.getParameterAnnotations())
                .map(annotations -> Arrays.stream(annotations)
                        .filter(Qualifier.class::isInstance)
                        .map(Qualifier.class::cast)
                        .map(Qualifier::value)
                        .findFirst()
                        .orElse(""))
                .toList();

        assertThat(qualifierValues).containsExactly(
                "rbac3RuntimeRedissonClient", "jacksonObjectMapper", "oauthAuditProperties",
                "auditDeliveryProperties", "idpClock", "defaultValidator");
    }

    private static void confirmedReceipt(RedissonClient redis, TokenUsageAuditComponent component, String outcome) {
        @SuppressWarnings("unchecked") org.redisson.api.RBucket<String> bucket = mock(org.redisson.api.RBucket.class);
        org.mockito.Mockito.doReturn(bucket).when(redis).getBucket(any(String.class), eq(StringCodec.INSTANCE));
        when(bucket.get()).thenReturn(component.encodeReceipt(component.createReceipt(event(), outcome, Instant.now())));
    }

    private Path secureKeyFile() throws Exception {
        Path keyFile = temporary.resolve("audit-hmac.key");
        Files.write(keyFile, "0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(keyFile,
                EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        return keyFile;
    }

    private static TokenUsageAuditComponent component(
            RedissonClient redisson, Path spool, Path keyFile, long maxBytes) {
        return new TokenUsageAuditComponent(redisson, new ObjectMapper().findAndRegisterModules(),
                new OAuthAuditProperties(STREAM, spool, maxBytes, keyFile));
    }

    private static TokenUsageAuditEvent event() {
        return new TokenUsageAuditEvent("event-1", 1, URI.create(ISSUER), "https://api.example/resource",
                "b".repeat(64), "c".repeat(64), TOKEN_REF, "SUCCESS", "ACCESS_GRANTED",
                Instant.parse("2026-10-08T12:00:00Z"), "trace-1");
    }

    private static String signature(String issuer, String tokenRef, byte[] key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
        return java.util.HexFormat.of().formatHex(mac.doFinal(
                (issuer + "\n" + tokenRef).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
