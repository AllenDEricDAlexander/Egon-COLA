package top.egon.cola.component.common;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.exception.BizException;
import top.egon.cola.component.common.page.PageQuery;
import top.egon.cola.component.common.page.PageResponse;
import top.egon.cola.component.common.result.Response;
import top.egon.cola.component.common.result.SingleResponse;
import top.egon.cola.component.common.validation.Assert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonComponentBoundaryTest {

    @Test
    void responseFactoriesKeepColaStyleContract() {
        Response success = Response.buildSuccess();
        assertTrue(success.isSuccess());

        Response failure = Response.buildFailure("BIZ_ERROR", "bad request");
        assertFalse(failure.isSuccess());
        assertEquals("BIZ_ERROR", failure.getErrCode());
        assertEquals("bad request", failure.getErrMessage());

        SingleResponse<String> single = SingleResponse.of("ok");
        assertTrue(single.isSuccess());
        assertEquals("ok", single.getData());
    }

    @Test
    void pageModelsNormalizeInvalidPageValues() {
        PageQuery query = new PageQuery() {
            private static final long serialVersionUID = 1L;
        };
        assertEquals(10, query.getPageSize());

        query.setPageIndex(0);
        query.setPageSize(0);

        assertEquals(1, query.getPageIndex());
        assertEquals(1, query.getPageSize());

        PageResponse<String> page = PageResponse.of(List.of("a", "b"), 5, 2, 2);
        assertTrue(page.isSuccess());
        assertEquals(3, page.getTotalPages());
        assertEquals(List.of("a", "b"), page.getData());
    }

    @Test
    void assertThrowsBizException() {
        BizException exception = assertThrows(BizException.class, () -> Assert.notNull(null, "missing value"));
        assertEquals("BIZ_ERROR", exception.getErrCode());
        assertEquals("missing value", exception.getErrMessage());
    }

    @Test
    void commonSourceDoesNotImportRuntimeFrameworks() throws Exception {
        Path sourceRoot = Path.of("src/main/java/top/egon/cola/component/common");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            List<String> badImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(line -> line.startsWith("import org.springframework.")
                            || line.startsWith("import jakarta.")
                            || line.startsWith("import javax.")
                            || line.startsWith("import org.redisson.")
                            || line.startsWith("import redis.")
                            || line.startsWith("import top.egon.cola.component.dtp.")
                            || line.startsWith("import com.alibaba.cola."))
                    .toList();
            assertEquals(List.of(), badImports);
        }
    }
}
