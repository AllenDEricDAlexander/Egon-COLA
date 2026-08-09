package top.egon.cola.component.ddc;

import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNullApi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DdcPackageDocumentationTest {

    private static final String DDC_PACKAGE = "top.egon.cola.component.ddc";

    private static final Path DDC_SOURCE_ROOT = Path.of(
            "src/main/java/top/egon/cola/component/ddc"
    );

    private static final Pattern CHINESE_TEXT = Pattern.compile("[\\p{IsHan}]");

    static final List<String> TARGET_PACKAGES = List.of(
            "",
            "annotation",
            "api",
            "api.client",
            "api.extension",
            "api.refresh",
            "api.registry",
            "model",
            "model.config",
            "model.instance",
            "model.lease",
            "model.management",
            "model.registry",
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
            "environment",
            "format",
            "observability",
            "error",
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

    @Test
    void everyApprovedPackageHasChineseFirstEnglishSecondDocumentation() throws Exception {
        for (String suffix : TARGET_PACKAGES) {
            Path packageDirectory = suffix.isEmpty()
                    ? DDC_SOURCE_ROOT
                    : DDC_SOURCE_ROOT.resolve(suffix.replace('.', '/'));
            Path packageInfo = packageDirectory.resolve("package-info.java");

            assertThat(packageInfo)
                    .as("package-info source for %s", suffix)
                    .exists();

            String source = Files.readString(packageInfo);
            int chineseIndex = firstChineseCharacter(source);
            int englishParagraphIndex = source.indexOf("<p>");

            assertThat(chineseIndex)
                    .as("Chinese package documentation in %s", packageInfo)
                    .isGreaterThanOrEqualTo(0);
            assertThat(englishParagraphIndex)
                    .as("English package documentation in %s", packageInfo)
                    .isGreaterThan(chineseIndex);
            assertThat(source.substring(englishParagraphIndex))
                    .as("English package documentation in %s", packageInfo)
                    .containsPattern("[A-Za-z]{4}");
        }
    }

    private int firstChineseCharacter(String source) {
        var matcher = CHINESE_TEXT.matcher(source);
        return matcher.find() ? matcher.start() : -1;
    }

    private Class<?> loadPackageInfo(String packageName) {
        try {
            return Class.forName(packageName + ".package-info");
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }
}
