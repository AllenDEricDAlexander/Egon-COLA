package top.egon.cola.component.rpc.context;

import org.springframework.core.env.Environment;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;

public class RpcProcessIdentityFactory {

    private final Environment environment;

    private final DdcProperties ddcProperties;

    private final DdcInstanceIdentity ddcIdentity;

    public RpcProcessIdentityFactory(Environment environment,
                                     DdcProperties ddcProperties,
                                     DdcInstanceIdentity ddcIdentity) {
        this.environment = environment;
        this.ddcProperties = ddcProperties;
        this.ddcIdentity = ddcIdentity;
    }

    public RpcProcessIdentity create() {
        String applicationName = environment.getProperty(
                "spring.application.name",
                ddcProperties.getAppCode()
        );
        long pid = ProcessHandle.current().pid();
        return new RpcProcessIdentity(
                applicationName,
                ddcProperties.getEnv(),
                ddcIdentity.host(),
                pid,
                ddcIdentity.instanceId()
        );
    }
}
