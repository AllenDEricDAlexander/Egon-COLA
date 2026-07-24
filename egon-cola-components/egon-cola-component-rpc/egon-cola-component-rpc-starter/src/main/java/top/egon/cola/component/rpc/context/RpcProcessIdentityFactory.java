package top.egon.cola.component.rpc.context;

import org.springframework.core.env.Environment;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.config.DdcProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RpcProcessIdentityFactory {

    private final Environment environment;

    private final DdcProperties ddcProperties;

    public RpcProcessIdentityFactory(Environment environment,
                                     DdcProperties ddcProperties) {
        this.environment = environment;
        this.ddcProperties = ddcProperties;
    }

    public RpcProcessIdentity create() {
        String applicationName = environment.getProperty(
                "spring.application.name",
                ddcProperties.getAppCode()
        );
        String host = resolveHost();
        long pid = ProcessHandle.current().pid();
        String instanceId = String.join(
                ":",
                applicationName,
                host,
                Long.toString(pid),
                UuidV7.simpleString()
        );
        return new RpcProcessIdentity(
                applicationName,
                ddcProperties.getEnv(),
                ddcProperties.getNamespace(),
                host,
                pid,
                instanceId
        );
    }

    private String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalStateException("cannot resolve RPC process host", exception);
        }
    }
}
