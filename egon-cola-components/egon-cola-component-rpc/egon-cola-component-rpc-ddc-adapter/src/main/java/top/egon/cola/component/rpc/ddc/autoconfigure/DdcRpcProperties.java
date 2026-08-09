package top.egon.cola.component.rpc.ddc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.ddc.security.DdcRpcCredential;

import java.time.Duration;

/**
 * DDC 直连 RPC 的本地引导配置。
 * / Local bootstrap settings for direct DDC RPC transport.
 */
@ConfigurationProperties(prefix = "egon.cola.component.ddc.rpc")
public class DdcRpcProperties {

    private String target;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration defaultTimeout = Duration.ofSeconds(10);
    private String loadBalancingPolicy = "round_robin";
    private int maxInboundMessageSize = 4 * 1024 * 1024;
    private Duration shutdownTimeout = Duration.ofSeconds(5);
    private Tls tls = new Tls();
    private Auth auth = new Auth();

    public String requireTarget() {
        if (target == null || target.isBlank()) {
            throw new IllegalStateException(
                    "egon.cola.component.ddc.rpc.target is required"
            );
        }
        return target.trim();
    }

    public DdcRpcCredential runtimeCredential() {
        return auth.runtime.require("runtime");
    }

    public DdcRpcCredential registryCredential() {
        return auth.registry.require("registry");
    }

    public DdcRpcCredential managementCredential() {
        return auth.management.require("management");
    }

    public RpcTransportSecurity transportSecurity() {
        return new RpcTransportSecurity(
                tls.enabled,
                tls.developmentPlaintext,
                tls.certificateChainPath,
                tls.privateKeyPath,
                tls.trustCertificateCollectionPath
        );
    }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = positive(connectTimeout, "connect-timeout"); }
    public Duration getDefaultTimeout() { return defaultTimeout; }
    public void setDefaultTimeout(Duration defaultTimeout) { this.defaultTimeout = positive(defaultTimeout, "default-timeout"); }
    public String getLoadBalancingPolicy() { return loadBalancingPolicy; }
    public void setLoadBalancingPolicy(String value) { this.loadBalancingPolicy = value; }
    public int getMaxInboundMessageSize() { return maxInboundMessageSize; }
    public void setMaxInboundMessageSize(int value) { this.maxInboundMessageSize = value; }
    public Duration getShutdownTimeout() { return shutdownTimeout; }
    public void setShutdownTimeout(Duration shutdownTimeout) { this.shutdownTimeout = positive(shutdownTimeout, "shutdown-timeout"); }
    public Tls getTls() { return tls; }
    public void setTls(Tls tls) { this.tls = tls; }
    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth; }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    "egon.cola.component.ddc.rpc." + name + " must be positive"
            );
        }
        return value;
    }

    /** RPC mTLS 或显式开发明文设置。 / RPC mTLS or explicit development plaintext settings. */
    public static class Tls {
        private boolean enabled;
        private boolean developmentPlaintext = true;
        private String certificateChainPath;
        private String privateKeyPath;
        private String trustCertificateCollectionPath;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isDevelopmentPlaintext() { return developmentPlaintext; }
        public void setDevelopmentPlaintext(boolean value) { this.developmentPlaintext = value; }
        public String getCertificateChainPath() { return certificateChainPath; }
        public void setCertificateChainPath(String value) { this.certificateChainPath = value; }
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String value) { this.privateKeyPath = value; }
        public String getTrustCertificateCollectionPath() { return trustCertificateCollectionPath; }
        public void setTrustCertificateCollectionPath(String value) { this.trustCertificateCollectionPath = value; }
    }

    /** RPC HMAC 开关和最小权限凭据。 / RPC HMAC switch and least-privilege credentials. */
    public static class Auth {
        private boolean enabled = true;
        private Credential runtime = new Credential();
        private Credential registry = new Credential();
        private Credential management = new Credential();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Credential getRuntime() { return runtime; }
        public void setRuntime(Credential runtime) { this.runtime = runtime; }
        public Credential getRegistry() { return registry; }
        public void setRegistry(Credential registry) { this.registry = registry; }
        public Credential getManagement() { return management; }
        public void setManagement(Credential management) { this.management = management; }
    }

    /** 一个能力独立使用的 HMAC 凭据。 / HMAC credential dedicated to one capability. */
    public static class Credential {
        private String accessKey;
        private String secretKey;

        DdcRpcCredential require(String profile) {
            if (accessKey == null || accessKey.isBlank()) {
                throw new IllegalStateException(
                        "egon.cola.component.ddc.rpc." + profile + ".access-key is required"
                );
            }
            if (secretKey == null || secretKey.isBlank()) {
                throw new IllegalStateException(
                        "egon.cola.component.ddc.rpc." + profile + ".secret-key is required"
                );
            }
            return new DdcRpcCredential(accessKey, secretKey);
        }

        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    }
}
