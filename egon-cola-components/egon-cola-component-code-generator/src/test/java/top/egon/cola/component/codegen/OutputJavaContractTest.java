package top.egon.cola.component.codegen;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.codegen.validation.OutputPathValidator;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutputJavaContractTest {

    private final OutputPathValidator paths = new OutputPathValidator();

    @Test
    void rejectsPackageAndPublicTypeMismatches() {
        String path = "goods-infrastructure/src/main/java/example/infrastructure/goods/converter/BrandPersistenceConverter.java";
        for (String source : new String[]{
                "package example.infrastructure.goods.service.impl;\npublic interface BrandPersistenceConverter {}",
                "package example.infrastructure.goods.converter;\npublic interface OtherConverter {}",
                "public interface BrandPersistenceConverter {}"}) {
            var failure = assertThrows(OutputPathValidator.PathValidationException.class,
                    () -> paths.checkJavaSource(path, source.getBytes(StandardCharsets.UTF_8)));
            assertEquals(OutputPathValidator.JAVA_SOURCE_MISMATCH, failure.getCode());
        }
    }

    @Test
    void acceptsSingleAndMultiModulePathsWithHeaderComments() {
        String source = "/* package ignored.header; */\n// generated contract\n"
                + "package example.application.goods;\npublic class BrandResult {}";
        for (String prefix : new String[]{"", "goods-application/"}) {
            paths.checkJavaSource(prefix + "src/main/java/example/application/goods/BrandResult.java",
                    source.getBytes(StandardCharsets.UTF_8));
        }
    }
}
