package top.egon.cola.component.ddc.bootstrap;

import top.egon.cola.component.ddc.client.HttpDdcAdminClient;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class DdcBootstrapClient {

    private final Supplier<List<DdcConfigValue>> pullSupplier;

    private final long maxYamlBytes;

    private volatile List<DdcConfigValue> cachedValues;

    public DdcBootstrapClient(DdcProperties properties) {
        this(
                () -> new HttpDdcAdminClient(properties).pull(),
                properties.getMaxYamlBytes()
        );
    }

    DdcBootstrapClient(Supplier<List<DdcConfigValue>> pullSupplier,
                       long maxYamlBytes) {
        this.pullSupplier = Objects.requireNonNull(
                pullSupplier,
                "pullSupplier"
        );
        if (maxYamlBytes <= 0) {
            throw new IllegalArgumentException("maxYamlBytes must be positive");
        }
        this.maxYamlBytes = maxYamlBytes;
    }

    public DdcConfigValue load(String resourceName) {
        List<DdcConfigValue> values = values();
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() != 1) {
            throw new IllegalStateException(
                    "DDC scope must contain exactly one application.yml"
            );
        }
        DdcConfigValue value = values.getFirst();
        if (value == null
                || !resourceName.equals(value.getConfigKey())
                || !"YAML".equals(value.getValueType())) {
            throw new IllegalStateException(
                    "DDC scope must contain only application.yml with YAML type"
            );
        }
        if (value.getVersion() == null || value.getVersion() <= 0) {
            throw new IllegalStateException(
                    "DDC application.yml must have a positive version"
            );
        }
        String content = value.getConfigValue();
        long contentBytes = content == null
                ? 0
                : content.getBytes(StandardCharsets.UTF_8).length;
        if (contentBytes > maxYamlBytes) {
            throw new IllegalStateException(
                    "DDC application.yml exceeds the UTF-8 limit of "
                            + maxYamlBytes + " bytes"
            );
        }
        return value;
    }

    private List<DdcConfigValue> values() {
        List<DdcConfigValue> current = cachedValues;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedValues == null) {
                List<DdcConfigValue> pulled = pullSupplier.get();
                cachedValues = pulled == null ? List.of() : List.copyOf(pulled);
            }
            return cachedValues;
        }
    }
}
