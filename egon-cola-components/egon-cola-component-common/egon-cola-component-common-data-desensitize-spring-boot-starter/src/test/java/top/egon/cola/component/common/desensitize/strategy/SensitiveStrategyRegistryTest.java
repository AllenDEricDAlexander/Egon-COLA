package top.egon.cola.component.common.desensitize.strategy;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SensitiveStrategyRegistryTest {

    private final SensitiveStrategyRegistry registry = SensitiveStrategyRegistry.defaults();

    @Test
    void masksAllBuiltInTypes() {
        assertEquals("138****5678", registry.mask(
                SensitiveType.MOBILE,
                "13812345678"
        ));
        assertEquals("s*********@gmail.com", registry.mask(
                SensitiveType.EMAIL,
                "supermario@gmail.com"
        ));
        assertEquals("330106********1234", registry.mask(
                SensitiveType.ID_CARD,
                "330106199901011234"
        ));
        assertEquals("6222********7890", registry.mask(
                SensitiveType.BANK_CARD,
                "6222021234567890"
        ));
        assertEquals("张*", registry.mask(SensitiveType.NAME, "张三"));
        assertEquals("浙江省杭州市********", registry.mask(
                SensitiveType.ADDRESS,
                "浙江省杭州市西湖区文三路1号"
        ));
        assertEquals("******", registry.mask(SensitiveType.FULL, "secret"));
    }

    @Test
    void fullyMasksValuesShorterThanAKeepWindow() {
        assertEquals("**", registry.mask(SensitiveType.MOBILE, "12"));
        assertEquals("*", registry.mask(SensitiveType.NAME, "张"));
        assertEquals("**", registry.mask(SensitiveType.FULL, "😀a"));
    }

    @Test
    void customStrategyOverridesOneBuiltInType() {
        SensitiveStrategy custom = new SensitiveStrategy() {
            @Override
            public SensitiveType type() {
                return SensitiveType.NAME;
            }

            @Override
            public String mask(String value) {
                return "custom";
            }
        };

        SensitiveStrategyRegistry customized = registry.withOverrides(List.of(custom));

        assertEquals("custom", customized.mask(SensitiveType.NAME, "张三"));
        assertEquals("138****5678", customized.mask(
                SensitiveType.MOBILE,
                "13812345678"
        ));
    }

    @Test
    void rejectsAmbiguousCustomStrategies() {
        SensitiveStrategy first = strategy(SensitiveType.NAME);
        SensitiveStrategy second = strategy(SensitiveType.NAME);

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.withOverrides(List.of(first, second))
        );
    }

    private SensitiveStrategy strategy(SensitiveType type) {
        return new SensitiveStrategy() {
            @Override
            public SensitiveType type() {
                return type;
            }

            @Override
            public String mask(String value) {
                return value;
            }
        };
    }
}
