package top.egon.cola.component.ddc.service;

import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Properties;

public class DdcInstanceIdentityFactory {

    private static final String VERSION_RESOURCE = "META-INF/egon-cola-ddc.properties";

    private final DdcProperties properties;
    private final DdcInstanceIdProvider instanceIdProvider;

    public DdcInstanceIdentityFactory(DdcProperties properties) {
        this(properties, null);
    }

    public DdcInstanceIdentityFactory(
            DdcProperties properties,
            DdcInstanceIdProvider instanceIdProvider) {
        this.properties = properties;
        this.instanceIdProvider = instanceIdProvider;
    }

    public DdcInstanceIdentity create() {
        String host = host();
        String pid = pid();
        String instanceId = instanceId();
        return new DdcInstanceIdentity(
                instanceId,
                properties.getAppCode(),
                properties.getEnv(),
                properties.getNamespace(),
                host,
                null,
                pid,
                sdkVersion()
        );
    }

    private String instanceId() {
        String configured = normalized(properties.getInstance().getId());
        if (configured != null) {
            return configured;
        }
        if (instanceIdProvider != null) {
            String provided = normalized(instanceIdProvider.getInstanceId());
            if (provided == null) {
                throw new DdcException("custom DDC instance id must not be blank");
            }
            return provided;
        }
        return UuidV7.string();
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String sdkVersion() {
        Properties version = new Properties();
        try (InputStream input = new ClassPathResource(VERSION_RESOURCE).getInputStream()) {
            version.load(input);
            String value = version.getProperty("sdk.version");
            if (value == null || value.isBlank() || value.contains("${")) {
                throw new DdcException("DDC SDK version resource is invalid");
            }
            return value;
        } catch (IOException exception) {
            throw new DdcException("load DDC SDK version failed", exception);
        }
    }

    private String host() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception exception) {
            return "127.0.0.1";
        }
    }

    private String pid() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }
}
