package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Validator;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;

/** Composes host transport and durable-audit checks with the existing security runtime. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "egon.cola.platform.tianquan.shoubing.usage-observation", name = "enabled", havingValue = "true")
public class TianquanSecurityObservationConfiguration {
    @Bean(name = "idpMeterRegistrySupplier")
    Supplier<MeterRegistry> idpMeterRegistrySupplier(ObjectProvider<MeterRegistry> registries) { return registries::getObject; }

    @Bean(name = "idpSecurityMetricsRecorder")
    IdpSecurityMetricsRecorder idpSecurityMetricsRecorder(@Qualifier("idpMeterRegistrySupplier") Supplier<MeterRegistry> registries,
            @Qualifier("defaultValidator") Validator validator) {
        registries.get();
        return new TianquanSecurityMetricsComponent(registries, validator);
    }
    /** Separate from the port factory to avoid a producer-to-recorder initialization cycle. */
    @Bean(name = "tianquanSecurityObservationStartupCheck")
    Object tianquanSecurityObservationStartupCheck(@Qualifier("tokenUsageAuditComponent") TokenUsageAuditComponent producer,
            @Qualifier("oauthUsageObservationProperties") OAuthUsageObservationProperties properties,
            @Qualifier("defaultValidator") Validator validator,
            @Qualifier("idpMeterRegistrySupplier") Supplier<MeterRegistry> registries,
            @Qualifier("environment") org.springframework.core.env.Environment environment,
            @Qualifier("tianquanSecurityAuditReady") java.util.function.BooleanSupplier auditReady) {
        new ValidationUtils(validator).validate(properties);
        registries.get();
        java.util.Objects.requireNonNull(producer, "producer");
        validateTransport(environment, properties);
        if (!auditReady.getAsBoolean()) { throw new IllegalStateException("OAUTH_AUDIT_NOT_READY"); }
        return new Object();
    }

    /** Holds an exclusive process lease until the host context is closed. */
    @Bean(name = "tianquanAuditSpoolLease", destroyMethod = "close")
    java.nio.channels.FileChannel tianquanAuditSpoolLease(
            @Qualifier("tokenUsageAuditComponent") TokenUsageAuditComponent producer,
            @Qualifier("oauthAuditProperties") top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties audit,
            @Qualifier("defaultValidator") Validator validator) throws java.io.IOException {
        new ValidationUtils(validator).validate(audit);
        java.nio.file.Path directory = audit.spoolDirectory();
        if (java.nio.file.Files.isSymbolicLink(directory)) { throw new IllegalStateException("OAUTH_AUDIT_SPOOL_SYMLINK"); }
        java.nio.file.Files.createDirectories(directory);
        var channel = java.nio.channels.FileChannel.open(directory.resolve(".receiver.lock"),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE, java.nio.file.LinkOption.NOFOLLOW_LINKS);
        try {
            java.nio.file.Files.setPosixFilePermissions(directory.resolve(".receiver.lock"), java.util.Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
            if (channel.tryLock() == null) { throw new IllegalStateException("OAUTH_AUDIT_SPOOL_IN_USE"); }
            java.nio.file.Path probe = java.nio.file.Files.createTempFile(directory, ".readiness-", ".tmp");
            java.nio.file.Path published = probe.resolveSibling(probe.getFileName() + ".published");
            try {
                try (var file = java.nio.channels.FileChannel.open(probe, java.nio.file.StandardOpenOption.WRITE)) { file.force(true); }
                java.nio.file.Files.move(probe, published, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                try (var parent = java.nio.channels.FileChannel.open(directory, java.nio.file.StandardOpenOption.READ)) { parent.force(true); }
            } finally {
                java.nio.file.Files.deleteIfExists(probe); java.nio.file.Files.deleteIfExists(published);
            }
            return channel;
        } catch (java.io.IOException | RuntimeException failure) {
            try { channel.close(); } catch (java.io.IOException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    @Bean(name = "tianquanSecurityAuditReady")
    java.util.function.BooleanSupplier tianquanSecurityAuditReady(
            @Qualifier("tianquanAuditSpoolLease") java.nio.channels.FileChannel lease,
            @Qualifier("oauthAuditProperties") top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties audit) {
        return () -> {
            try {
                if (!lease.isOpen() || !java.nio.file.Files.isWritable(audit.spoolDirectory())
                        || !java.nio.file.Files.isRegularFile(audit.hmacKeyFile(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) { return false; }
                var permissions = java.nio.file.Files.getPosixFilePermissions(audit.hmacKeyFile());
                if (permissions.stream().anyMatch(permission -> !permission.name().startsWith("OWNER_"))) { return false; }
                long keyLength = java.nio.file.Files.size(audit.hmacKeyFile());
                if (keyLength < 32 || keyLength > 4096) { return false; }
                long bytes = 0;
                try (var files = java.nio.file.Files.newDirectoryStream(audit.spoolDirectory())) {
                    for (var path : files) {
                        if (!java.nio.file.Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) { continue; }
                        String name = path.getFileName().toString();
                        if (name.endsWith(".dead") || name.endsWith(".evt")
                                && !java.nio.file.Files.exists(path.resolveSibling(name + ".sent"))) { return false; }
                        bytes = Math.addExact(bytes, java.nio.file.Files.size(path));
                        if (bytes >= audit.spoolMaxBytes()) { return false; }
                    }
                }
                return true;
            } catch (java.io.IOException | ArithmeticException unavailable) { return false; }
        };
    }

    /** Contributes to the existing Actuator readiness group without replacing other indicators. */
    @Bean(name = "tianquanAuditReadiness")
    org.springframework.boot.actuate.health.HealthIndicator tianquanAuditReadiness(
            @Qualifier("tianquanSecurityAuditReady") java.util.function.BooleanSupplier ready) {
        return () -> ready.getAsBoolean() ? org.springframework.boot.actuate.health.Health.up().build()
                : org.springframework.boot.actuate.health.Health.down().withDetail("failedCheck", "oauthAudit").build();
    }

    private void validateTransport(org.springframework.core.env.Environment environment, OAuthUsageObservationProperties usage) {
        var binder = org.springframework.boot.context.properties.bind.Binder.get(environment);
        var idp = binder.bind("egon.cola.platform.tianquan.shoubing", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class)
                .orElseGet(top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties::new);
        boolean local = java.util.Arrays.asList(environment.getActiveProfiles()).contains("local")
                && java.util.Set.of("local", "dev").contains(environment.getProperty("DEPLOYMENT_ENV",
                        environment.getProperty("egon.cola.component.tianshu.env", ""))) && usage.isAllowLocalPlaintext();
        if (idp.getTrustedIssuers().isEmpty()) {
            secureEndpoint(idp.getIssuer(), "https", false);
            secureEndpoint(idp.getJwkSetUri(), "https", local);
        } else {
            for (var entry : idp.getTrustedIssuers().values()) {
                secureEndpoint(entry.getIssuer(), "https", false);
                secureEndpoint(entry.getJwksUri(), "https", false);
            }
        }
        secureEndpoint(idp.getResourceUri() == null ? null : idp.getResourceUri().toASCIIString(), "https", local);
        secureEndpoint(idp.getRevocation().getPrimaryEndpoint(), "rediss", local);
        boolean trustedFallback = idp.getTrustedIssuers().isEmpty()
                ? usage.getFallbackIssuer().toASCIIString().equals(idp.getIssuer())
                : idp.getTrustedIssuers().values().stream().anyMatch(entry -> usage.getFallbackIssuer().toASCIIString().equals(entry.getIssuer())
                        && "ACTIVE".equals(entry.getStatus()));
        if (!trustedFallback) { throw new IllegalStateException("OAUTH_OBSERVATION_FALLBACK_UNTRUSTED"); }
        secureEndpoint(environment.getProperty("egon.cola.platform.tianquan.jianshen.authorization.endpoint"), "https", local);
        secureEndpoint(environment.getProperty("egon.tianquan-shoubing.oauth.public-origin"), "https", local);
        var clients = binder.bind("spring.security.oauth2.client", org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.class)
                .orElseGet(org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties::new);
        for (var provider : clients.getProvider().values()) {
            if (provider.getIssuerUri() != null) { secureEndpoint(provider.getIssuerUri(), "https", false); }
            for (String endpoint : new String[] { provider.getTokenUri(), provider.getJwkSetUri(), provider.getAuthorizationUri(), provider.getUserInfoUri() }) {
                if (endpoint != null) { secureEndpoint(endpoint, "https", local); }
            }
        }
        for (String prefix : java.util.List.of("egon.cola.component.rpc", "egon.cola.component.tianshu")) {
            if (!environment.getProperty(prefix + ".enabled", Boolean.class, false)) { continue; }
            String tls = prefix + (prefix.endsWith("tianshu") ? ".rpc.tls" : ".tls");
            boolean enabled = environment.getProperty(tls + ".enabled", Boolean.class, false);
            boolean plaintext = environment.getProperty(tls + ".development-plaintext", Boolean.class, false);
            if (!enabled && !(local && plaintext) || plaintext && !local) { throw new IllegalStateException("OAUTH_OBSERVATION_RPC_TLS_REQUIRED"); }
            if (!enabled && (prefix.endsWith("tianshu") || environment.getProperty(prefix + ".provider.enabled", Boolean.class, false))) {
                String target = environment.getProperty(prefix.endsWith("tianshu") ? prefix + ".rpc.target" : prefix + ".provider.bind-address", "");
                String host = target.replaceFirst("^dns:///", "").replaceFirst("^https?://", "").replaceFirst(":\\d+$", "");
                if (!java.util.Set.of("localhost", "127.0.0.1", "[::1]").contains(host)) { throw new IllegalStateException("OAUTH_OBSERVATION_RPC_LOCAL_TARGET_REQUIRED"); }
            }
        }
    }

    private void secureEndpoint(String value, String scheme, boolean local) {
        if (value == null || value.isBlank()) { throw new IllegalStateException("OAUTH_OBSERVATION_ENDPOINT_REQUIRED"); }
        java.net.URI uri = java.net.URI.create(value);
        boolean plaintext = local && uri.getHost() != null && java.util.Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost())
                && ("https".equals(scheme) && "http".equals(uri.getScheme()) || "rediss".equals(scheme) && "redis".equals(uri.getScheme()));
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !scheme.equals(uri.getScheme()) && !plaintext) { throw new IllegalStateException("OAUTH_OBSERVATION_TLS_REQUIRED"); }
    }
}
