package top.egon.cola.component.rpc.context;

import org.springframework.core.env.Environment;
import top.egon.cola.component.rpc.config.EgonRpcProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RpcProcessIdentityFactory {

    private final Environment environment;

    private final EgonRpcProperties.Identity properties;

    public RpcProcessIdentityFactory(Environment environment,
                                     EgonRpcProperties properties) {
        this.environment = environment;
        this.properties = properties.getIdentity();
    }

    public RpcProcessIdentity create() {
        String applicationName = environment.getProperty(
                "spring.application.name",
                "application"
        );
        long pid = ProcessHandle.current().pid();
        String host = configuredOrDefault(properties.getHost(), localHost());
        String instanceId = configuredOrDefault(
                properties.getInstanceId(),
                applicationName + "-" + host + "-" + pid
        );
        return new RpcProcessIdentity(
                applicationName,
                configuredOrDefault(properties.getEnv(), "default"),
                host,
                pid,
                instanceId
        );
    }

    private String localHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            return "127.0.0.1";
        }
    }

    private String configuredOrDefault(String configured, String fallback) {
        return configured == null || configured.isBlank()
                ? fallback
                : configured.trim();
    }
}
