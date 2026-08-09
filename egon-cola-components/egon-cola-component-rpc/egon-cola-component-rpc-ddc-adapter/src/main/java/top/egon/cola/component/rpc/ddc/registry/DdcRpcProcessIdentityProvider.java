package top.egon.cola.component.rpc.ddc.registry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProcessIdentityProvider;

import java.net.InetAddress;

/** 在 DDC 启用时使 RPC 注册复用同一物理实例身份。 / Reuses DDC physical identity for RPC registration. */
public final class DdcRpcProcessIdentityProvider implements RpcProcessIdentityProvider {

    private final ObjectProvider<DdcInstanceIdentity> ddcIdentity;
    private final DdcProperties properties;
    private final Environment environment;

    public DdcRpcProcessIdentityProvider(
            ObjectProvider<DdcInstanceIdentity> ddcIdentity,
            DdcProperties properties,
            Environment environment) {
        this.ddcIdentity = ddcIdentity;
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public RpcProcessIdentity identity() {
        DdcInstanceIdentity identity = ddcIdentity.getIfAvailable();
        if (identity != null) {
            return new RpcProcessIdentity(
                    applicationName(), identity.env(), identity.host(),
                    pid(identity.pid()), identity.instanceId()
            );
        }
        String host = environment.getProperty(
                "egon.cola.component.rpc.identity.host", localHost());
        long pid = ProcessHandle.current().pid();
        String instanceId = properties.getInstance().getId();
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = environment.getProperty(
                    "egon.cola.component.rpc.identity.instance-id",
                    applicationName() + "-" + host + "-" + pid
            );
        }
        return new RpcProcessIdentity(
                applicationName(), properties.getEnv(), host, pid, instanceId);
    }

    private String applicationName() {
        return environment.getProperty("spring.application.name", properties.getAppCode());
    }

    private long pid(String value) {
        if (value != null) {
            String candidate = value.contains("@") ? value.substring(0, value.indexOf('@')) : value;
            try { return Long.parseLong(candidate); } catch (NumberFormatException ignored) { }
        }
        return ProcessHandle.current().pid();
    }

    private String localHost() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception ignored) { return "127.0.0.1"; }
    }
}
