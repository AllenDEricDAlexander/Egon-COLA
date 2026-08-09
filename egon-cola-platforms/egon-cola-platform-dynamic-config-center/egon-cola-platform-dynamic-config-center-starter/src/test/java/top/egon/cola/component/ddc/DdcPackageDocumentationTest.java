package top.egon.cola.component.ddc;

import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNullApi;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcPackageDocumentationTest {

    private static final String DDC_PACKAGE = "top.egon.cola.component.ddc";

    private static final List<String> TARGET_PACKAGES = List.of(
            "",
            "annotation",
            "api",
            "api.client",
            "api.extension",
            "api.refresh",
            "api.registry",
            "model",
            "model.client",
            "model.config",
            "model.instance",
            "model.lease",
            "model.management",
            "model.registry",
            "client",
            "client.config",
            "client.http",
            "client.management",
            "client.registry",
            "service",
            "service.binding",
            "service.lifecycle",
            "service.refresh",
            "service.registry",
            "listener",
            "listener.config",
            "listener.registry",
            "state",
            "redis",
            "configdata",
            "environment",
            "format",
            "observability",
            "error",
            "error.http",
            "error.management",
            "autoconfigure",
            "autoconfigure.properties"
    );

    @Test
    void everyApprovedPackagePublishesTheNonNullApiContract() {
        for (String suffix : TARGET_PACKAGES) {
            String packageName = suffix.isEmpty()
                    ? DDC_PACKAGE
                    : DDC_PACKAGE + "." + suffix;
            Class<?> packageInfo = loadPackageInfo(packageName);

            assertThat(packageInfo)
                    .as("package-info for %s", packageName)
                    .isNotNull();

            assertThat(packageInfo.getPackage().getAnnotation(NonNullApi.class))
                    .as("@NonNullApi on %s", packageName)
                    .isNotNull();
        }
    }

    private Class<?> loadPackageInfo(String packageName) {
        try {
            return Class.forName(packageName + ".package-info");
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }
}
