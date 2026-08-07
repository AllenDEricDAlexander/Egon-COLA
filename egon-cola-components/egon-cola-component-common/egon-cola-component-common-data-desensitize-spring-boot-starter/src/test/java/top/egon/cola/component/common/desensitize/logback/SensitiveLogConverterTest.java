package top.egon.cola.component.common.desensitize.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveScene;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategy;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveLogConverterTest {

    private LoggerContext loggerContext;

    private PatternLayout layout;

    @BeforeEach
    void setUp() {
        loggerContext = new LoggerContext();
        layout = new PatternLayout();
        layout.setContext(loggerContext);
        layout.getInstanceConverterMap().put(
                "sensitiveMsg",
                SensitiveLogConverter::new
        );
        layout.setPattern("%sensitiveMsg");
        layout.start();
    }

    @AfterEach
    void tearDown() {
        layout.stop();
        loggerContext.stop();
    }

    @Test
    void masksAnnotatedObjectArgumentWithoutUsingRawToString() {
        UserDto user = new UserDto(
                7L,
                "张三",
                "13812345678",
                "visible@example.com"
        );

        String message = format("用户信息: {}", user);

        assertTrue(message.startsWith("用户信息: UserDto{"));
        assertTrue(message.contains("id=7"));
        assertTrue(message.contains("name=张*"));
        assertTrue(message.contains("mobile=138****5678"));
        assertTrue(message.contains("responseOnlyEmail=visible@example.com"));
        assertFalse(message.contains("张三"));
        assertFalse(message.contains("13812345678"));
        assertEquals("13812345678", user.getMobile());
    }

    @Test
    void masksAnnotatedObjectsInsideCollectionsAndMaps() {
        UserDto user = new UserDto(
                7L,
                "张三",
                "13812345678",
                "visible@example.com"
        );

        String message = format(
                "users={}, indexed={}",
                List.of(user),
                Map.of("primary", user)
        );

        assertTrue(message.contains("users=[UserDto{"));
        assertTrue(message.contains("indexed={primary=UserDto{"));
        assertFalse(message.contains("13812345678"));
    }

    @Test
    void discoversAnnotatedObjectInsideUnannotatedWrapper() {
        UserDto user = new UserDto(
                7L,
                "张三",
                "13812345678",
                "visible@example.com"
        );

        String message = format("wrapper={}", new UserWrapper("primary", user));

        assertTrue(message.startsWith("wrapper=UserWrapper{"));
        assertTrue(message.contains("label=primary"));
        assertTrue(message.contains("user=UserDto{"));
        assertFalse(message.contains("张三"));
        assertFalse(message.contains("13812345678"));
    }

    @Test
    void supportsMethodAnnotationAndExplicitScalarMasking() {
        MethodAnnotatedDto dto = new MethodAnnotatedDto("6222021234567890");

        String message = format(
                "dto={}, mobile={}",
                dto,
                SensitiveLogs.of("13812345678", SensitiveType.MOBILE)
        );

        assertTrue(message.contains("bankCard=6222********7890"));
        assertTrue(message.contains("mobile=138****5678"));
        assertFalse(message.contains("6222021234567890"));
        assertFalse(message.contains("13812345678"));
    }

    @Test
    void usesStrategyRegistryInstalledInLoggerContext() {
        SensitiveStrategy customNameStrategy = new SensitiveStrategy() {
            @Override
            public SensitiveType type() {
                return SensitiveType.NAME;
            }

            @Override
            public String mask(String value) {
                return "custom-name";
            }
        };
        SensitiveStrategyRegistry registry = SensitiveStrategyRegistry.defaults()
                .withOverrides(List.of(customNameStrategy));
        try (SensitiveLogbackRegistryBridge ignored =
                     new SensitiveLogbackRegistryBridge(loggerContext, registry)) {
            String message = format(
                    "user={}",
                    new UserDto(7L, "张三", "13812345678", "visible@example.com")
            );

            assertTrue(message.contains("name=custom-name"));
            assertTrue(message.contains("mobile=138****5678"));
            assertFalse(message.contains("张三"));
        }
    }

    @Test
    void restoresLoggerContextRegistryWhenBridgesCloseOutOfOrder() {
        SensitiveStrategyRegistry firstRegistry = SensitiveStrategyRegistry.defaults();
        SensitiveStrategyRegistry secondRegistry = SensitiveStrategyRegistry.defaults();
        SensitiveLogbackRegistryBridge first =
                new SensitiveLogbackRegistryBridge(loggerContext, firstRegistry);
        SensitiveLogbackRegistryBridge second =
                new SensitiveLogbackRegistryBridge(loggerContext, secondRegistry);

        assertSame(secondRegistry, loggerContext.getObject(
                SensitiveLogConverter.STRATEGY_REGISTRY_CONTEXT_KEY
        ));
        first.close();
        assertSame(secondRegistry, loggerContext.getObject(
                SensitiveLogConverter.STRATEGY_REGISTRY_CONTEXT_KEY
        ));
        second.close();
        assertNull(loggerContext.getObject(
                SensitiveLogConverter.STRATEGY_REGISTRY_CONTEXT_KEY
        ));
    }

    @Test
    void leavesOrdinaryArgumentsAndEscapedPlaceholdersToSlf4jFormatting() {
        assertEquals("literal={}, value=42", format("literal=\\{}, value={}", 42));
    }

    private String format(String pattern, Object... arguments) {
        LoggingEvent event = new LoggingEvent(
                getClass().getName(),
                loggerContext.getLogger("sensitive-test"),
                Level.INFO,
                pattern,
                null,
                arguments
        );
        return layout.doLayout(event);
    }

    private static class UserDto {

        private final Long id;

        @Sensitive(type = SensitiveType.NAME)
        private final String name;

        @Sensitive(type = SensitiveType.MOBILE)
        private final String mobile;

        @Sensitive(type = SensitiveType.EMAIL, scenes = SensitiveScene.RESPONSE)
        private final String responseOnlyEmail;

        UserDto(Long id, String name, String mobile, String responseOnlyEmail) {
            this.id = id;
            this.name = name;
            this.mobile = mobile;
            this.responseOnlyEmail = responseOnlyEmail;
        }

        public String getMobile() {
            return mobile;
        }

        @Override
        public String toString() {
            return "raw=" + name + "/" + mobile;
        }
    }

    private static class MethodAnnotatedDto {

        private final String bankCard;

        MethodAnnotatedDto(String bankCard) {
            this.bankCard = bankCard;
        }

        @Sensitive(type = SensitiveType.BANK_CARD)
        public String getBankCard() {
            return bankCard;
        }
    }

    private static class UserWrapper {

        private final String label;

        private final UserDto user;

        UserWrapper(String label, UserDto user) {
            this.label = label;
            this.user = user;
        }
    }
}
