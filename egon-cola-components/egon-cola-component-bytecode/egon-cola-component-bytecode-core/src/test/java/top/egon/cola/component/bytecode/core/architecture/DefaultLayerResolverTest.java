package top.egon.cola.component.bytecode.core.architecture;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultLayerResolverTest {

    @Test
    void resolvesByExplicitModuleThenPackageThenSuffix() {
        LayerMapping mapping = new LayerMapping(
                Map.of(ArchitectureLayer.DOMAIN, Set.of("orders-special")),
                Map.of(ArchitectureLayer.APPLICATION, Set.of("sample.application.."))
        );
        DefaultLayerResolver resolver = new DefaultLayerResolver(mapping);

        assertEquals(ArchitectureLayer.DOMAIN,
                resolver.resolve("orders-special", "sample.application.OrderService"));
        assertEquals(ArchitectureLayer.APPLICATION,
                resolver.resolve("orders-custom", "sample.application.OrderService"));
        assertEquals(ArchitectureLayer.INFRASTRUCTURE,
                resolver.resolve("orders-infrastructure", "sample.other.Repository"));
        assertEquals(ArchitectureLayer.UNKNOWN,
                resolver.resolve("orders-custom", "sample.other.Unknown"));
    }

    @Test
    void rejectsDuplicateModuleAndAmbiguousPackageMatches() {
        assertThrows(IllegalArgumentException.class, () -> new LayerMapping(
                Map.of(
                        ArchitectureLayer.DOMAIN, Set.of("duplicate"),
                        ArchitectureLayer.APPLICATION, Set.of("duplicate")
                ),
                Map.of()
        ));

        DefaultLayerResolver resolver = new DefaultLayerResolver(new LayerMapping(
                Map.of(),
                Map.of(
                        ArchitectureLayer.DOMAIN, Set.of("sample.."),
                        ArchitectureLayer.APPLICATION, Set.of("sample.application..")
                )
        ));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve("custom", "sample.application.OrderService"));
    }
}
