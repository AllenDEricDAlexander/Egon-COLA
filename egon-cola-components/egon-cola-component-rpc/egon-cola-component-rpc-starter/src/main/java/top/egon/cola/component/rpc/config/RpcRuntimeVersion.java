package top.egon.cola.component.rpc.config;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class RpcRuntimeVersion {

    private static final String RESOURCE =
            "META-INF/egon-cola-rpc.properties";

    private RpcRuntimeVersion() {
    }

    static String load() {
        Properties properties = new Properties();
        try (InputStream input =
                     new ClassPathResource(RESOURCE).getInputStream()) {
            properties.load(input);
            String version = properties.getProperty(
                    "egon.rpc.runtime-version"
            );
            if (version == null
                    || version.isBlank()
                    || version.contains("${")) {
                throw new IllegalStateException(
                        "RPC runtime version resource is invalid"
                );
            }
            return version;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "load RPC runtime version failed",
                    exception
            );
        }
    }
}
