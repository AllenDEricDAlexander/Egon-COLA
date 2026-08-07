package top.egon.cola.component.ddc.admin.service.config;

import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.environment.DdcYamlPropertySourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class DdcYamlConfigValidator {

    private final long maxValueBytes;

    private final DdcYamlPropertySourceLoader propertySourceLoader =
            new DdcYamlPropertySourceLoader();

    public DdcYamlConfigValidator(long maxValueBytes) {
        if (maxValueBytes <= 0) {
            throw new IllegalArgumentException("maxValueBytes must be positive");
        }
        this.maxValueBytes = maxValueBytes;
    }

    public void validate(String content) {
        long bytes = content == null
                ? 0
                : content.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > maxValueBytes) {
            throw new DdcAdminException(
                    "application.yml exceeds the UTF-8 limit of "
                            + maxValueBytes + " bytes"
            );
        }
        try {
            propertySourceLoader.load(
                    DdcYamlPropertySourceLoader.RESOURCE_NAME,
                    content,
                    1L
            );
        } catch (IOException | RuntimeException exception) {
            throw new DdcAdminException(
                    "invalid application.yml: " + exception.getMessage()
            );
        }
    }
}
