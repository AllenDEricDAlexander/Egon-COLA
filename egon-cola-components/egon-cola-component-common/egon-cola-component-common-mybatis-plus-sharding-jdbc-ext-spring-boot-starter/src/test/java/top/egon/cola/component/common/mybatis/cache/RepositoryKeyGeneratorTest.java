package top.egon.cola.component.common.mybatis.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * REQ-021 的缓存键生成合同：具名 KeyGenerator 只接受单个正 Long 或 {@code EgonModel}，
 * 产出可信 {@code tenant:id}，且键不含方法名。
 */
class RepositoryKeyGeneratorTest {

    private final TestTenantIdProvider tenantContext = new TestTenantIdProvider();
    private final EgonColaRepositoryKeyGenerator generator = new EgonColaRepositoryKeyGenerator();

    @BeforeEach
    void publishTenant() {
        tenantContext.set(41L);
    }

    @AfterEach
    void clearTenant() {
        tenantContext.clear();
    }

    private static Method method(String name, Class<?>... parameterTypes) throws Exception {
        return Fixtures.class.getDeclaredMethod(name, parameterTypes);
    }

    @Test
    void singleLongBecomesTenantScopedKey() throws Exception {
        assertThat(generator.generate(this, method("byId", Long.class), 7L)).isEqualTo("41:7");
    }

    @Test
    void modelWithMatchingTenantUsesItsId() throws Exception {
        assertThat(generator.generate(this, method("byModel", TestBusinessModel.class),
                model(7L, 41L))).isEqualTo("41:7");
    }

    @Test
    void modelWithoutTenantStillUsesTrustedContext() throws Exception {
        assertThat(generator.generate(this, method("byModel", TestBusinessModel.class),
                model(7L, null))).isEqualTo("41:7");
    }

    @Test
    void modelWithForeignTenantIsRejected() throws Exception {
        TestBusinessModel foreign = model(7L, 42L);

        assertThatThrownBy(() -> generator.generate(this, method("byModel", TestBusinessModel.class), foreign))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_TENANT_MISMATCH");
    }

    @Test
    void keyNeverContainsMethodName() throws Exception {
        Object byId = generator.generate(this, method("byId", Long.class), 7L);
        Object other = generator.generate(this, method("otherById", Long.class), 7L);

        assertThat(byId).isEqualTo("41:7").isEqualTo(other);
    }

    @Test
    void nonPositiveMissingOrUnsupportedIdIsRejected() throws Exception {
        assertThatThrownBy(() -> generator.generate(this, method("byId", Long.class), 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_ID_INVALID");
        assertThatThrownBy(() -> generator.generate(this, method("byId", Long.class), (Object) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_ID_INVALID");
        assertThatThrownBy(() -> generator.generate(this, method("byId", Long.class), "7"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_ID_INVALID");
        assertThatThrownBy(() -> generator.generate(this, method("byModel", TestBusinessModel.class),
                model(null, 41L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_ID_INVALID");
    }

    @Test
    void collectionAndMultiParameterSignaturesAreRejected() throws Exception {
        assertThatThrownBy(() -> generator.generate(this, method("byIds", List.class), List.of(7L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_ARGUMENT_UNSUPPORTED");
        assertThatThrownBy(() -> generator.generate(this, method("byIdAndTitle", Long.class, String.class),
                7L, "t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_ARGUMENT_UNSUPPORTED");
        assertThatThrownBy(() -> generator.generate(this, method("byId", Long.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CACHE_KEY_ARGUMENT_UNSUPPORTED");
    }

    @Test
    void absentTenantContextFailsBeforeProducingAKey() throws Exception {
        tenantContext.clear();

        assertThatThrownBy(() -> generator.generate(this, method("byId", Long.class), 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TENANT_CONTEXT_MISSING");
    }

    private static TestBusinessModel model(Long id, Long tenantId) {
        TestBusinessModel model = new TestBusinessModel().businessValues("title", null);
        model.setId(id);
        model.setTenantId(tenantId);
        return model;
    }

    /** KeyGenerator only reflects on the signature, so placeholder methods carry the shapes under test. */
    static final class Fixtures {

        private Fixtures() {
        }

        private static void byId(Long id) {
        }

        private static void otherById(Long id) {
        }

        private static void byModel(TestBusinessModel model) {
        }

        private static void byIds(List<Long> ids) {
        }

        private static void byIdAndTitle(Long id, String title) {
        }
    }
}
