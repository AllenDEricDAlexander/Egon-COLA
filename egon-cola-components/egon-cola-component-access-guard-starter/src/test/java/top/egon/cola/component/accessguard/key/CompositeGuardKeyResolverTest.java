package top.egon.cola.component.accessguard.key;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.api.GuardKey;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.contributor.ArgumentKeyContributor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeGuardKeyResolverTest {

    private final CompositeGuardKeyResolver resolver = new CompositeGuardKeyResolver(
            List.of(new ArgumentKeyContributor()),
            new HmacSha256KeyHasher());

    @Test
    void ordersCompositePartsByOrderThenDeclaration() throws Exception {
        GuardKeyResolution resolution = resolver.resolve(
                invocation("user-1", "tenant-1"),
                config());

        assertThat(resolution.parts())
                .extracting(GuardKeyPart::name)
                .containsExactly("tenant", "user");
        assertThat(resolution.keyHash()).matches("[0-9a-f]{64}");
        assertThat(resolution.toString()).doesNotContain("tenant-1", "user-1");
    }

    @Test
    void requiredMissingPartNeverFallsBackToGlobal() throws Exception {
        assertThatThrownBy(() -> resolver.resolve(invocation(null, "tenant-1"), config()))
                .isInstanceOf(GuardKeyResolutionException.class)
                .hasMessageNotContaining("tenant-1")
                .hasMessageNotContaining("user-1");
    }

    @Test
    void rejectsControlCharactersBeforeHashing() throws Exception {
        assertThatThrownBy(() -> resolver.resolve(invocation("user\n1", "tenant-1"), config()))
                .isInstanceOf(GuardKeyResolutionException.class)
                .hasMessageContaining("invalid");
    }

    private static KeyConfig config() {
        return new KeyConfig(List.of("ARGUMENT"), List.of(), "test-secret", List.of(), 128);
    }

    private static GuardInvocation invocation(String user, String tenant) throws Exception {
        Method method = Sample.class.getDeclaredMethod("draw", String.class, String.class);
        return new GuardInvocation(
                "draw",
                new Sample(),
                Sample.class,
                method,
                new Object[]{user, tenant},
                Map.of(),
                GuardEntryType.AOP,
                GuardInvocationKind.METHOD,
                () -> "ok");
    }

    static class Sample {

        String draw(
                @GuardKey(value = "user", order = 20) String user,
                @GuardKey(value = "tenant", order = 10) String tenant
        ) {
            return user + tenant;
        }
    }
}
